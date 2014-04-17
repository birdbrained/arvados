class ActionsController < ApplicationController

  skip_before_filter :find_object_by_uuid, only: :post

  def combine_selected_files_into_collection
    lst = []
    files = []
    params["selection"].each do |s|
      m = CollectionsHelper.match(s)
      if m and m[1] and m[2]
        lst.append(m[1] + m[2])
        files.append(m)
      end
    end

    collections = Collection.where(uuid: lst)

    chash = {}
    collections.each do |c|
      c.reload()
      chash[c.uuid] = c
    end

    combined = ""
    files.each do |m|
      mt = chash[m[1]+m[2]].manifest_text
      if m[4]
        IO.popen(['arv-normalize', '--extract', m[4][1..-1]], 'w+b') do |io|
          io.write mt
          io.close_write
          while buf = io.read(2**20)
            combined += buf
          end
        end
      else
        combined += chash[m[1]+m[2]].manifest_text
      end
    end

    normalized = ''
    IO.popen(['arv-normalize'], 'w+b') do |io|
      io.write combined
      io.close_write
      while buf = io.read(2**20)
        normalized += buf
      end
    end

    require 'digest/md5'

    d = Digest::MD5.new()
    d << normalized
    newuuid = "#{d.hexdigest}+#{normalized.length}"

    env = Hash[ENV].
      merge({
              'ARVADOS_API_HOST' =>
              $arvados_api_client.arvados_v1_base.
              sub(/\/arvados\/v1/, '').
              sub(/^https?:\/\//, ''),
              'ARVADOS_API_TOKEN' => Thread.current[:arvados_api_token],
              'ARVADOS_API_HOST_INSECURE' =>
              Rails.configuration.arvados_insecure_https ? 'true' : 'false'
            })

    IO.popen([env, 'arv-put', '--raw'], 'w+b') do |io|
      io.write normalized
      io.close_write
      while buf = io.read(2**20)

      end
    end

    newc = Collection.new({:uuid => newuuid, :manifest_text => normalized})
    newc.save!

    chash.each do |k,v|
      l = Link.new({
                     tail_uuid: k,
                     head_uuid: newuuid,
                     link_class: "provenance",
                     name: "provided"
                   })
      l.save!
    end

    redirect_to controller: 'collections', action: :show, id: newc.uuid
  end

  def post
    if params["combine_selected_files_into_collection"]
      combine_selected_files_into_collection
    else
      redirect_to :back
    end
  end
end
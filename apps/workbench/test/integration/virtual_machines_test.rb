require 'integration_helper'

class VirtualMachinesTest < ActionDispatch::IntegrationTest
  test "make and name a new virtual machine" do
    Capybara.current_driver = Capybara.javascript_driver
    visit page_with_token('admin_trustedclient')
    click_link 'Virtual machines'
    assert page.has_text? 'testvm.shell'
    click_on 'Add a new virtual machine'
    assert page.has_text? 'none'
    click_link 'none'
    assert page.has_text? 'Update hostname'
    fill_in 'editable-text', with: 'testname'
    click_button 'editable-submit'
    assert page.has_text? 'testname'
  end
end
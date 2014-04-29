package org.arvados.sdk.java;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

import com.google.api.services.discovery.model.RestDescription;
import com.google.api.services.discovery.model.RestResource;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Unit test for Arvados.
 */
public class ArvadosTest {

  @Test(expected=Exception.class)
  public void testMainWithNoParams() throws Exception {
    String[] args = new String[0];
    Arvados.main(args);
  }

  @Test(expected=Exception.class)
  public void testHelp() throws Exception {
    String[] args = new String[1];

    args[0] = "help";
    Arvados.help(args); // expect this to succeed with no problems

    args = new String[2];

    args[0] = "help";
    args[1] = "call";
    Arvados.main(args); // call via main
    
    args[0] = "help";
    args[1] = "discover";
    Arvados.help(args); // call help directly

    args[0] = "help";
    args[1] = "unknown";
    Arvados.help(args); // expect exception
  }

  /**
   * test discover method
   * @throws Exception
   */
  @Test
  public void testDiscover() throws Exception {
    Arvados arv = new Arvados("arvados");

    List<String> params = new ArrayList<String>();
    params.add("discover");
    params.add("arvados");
    params.add("v1");

    RestDescription restDescription = arv.discover(params);

    // The discover method returns the supported methods
    Map<String, RestResource> resources = restDescription.getResources();
    assertNotNull("Expected resources", resources);

    Object users = resources.get("users");
    assertNotNull ("Expected users.list method", users);
    assertEquals("Exepcted users.list to be a RestResource type", RestResource.class, users.getClass());

    assertTrue("Root URL expected to match ARVADOS_API_HOST env paramdeter", 
        restDescription.getRootUrl().contains(System.getenv().get("ARVADOS_API_HOST")));
  }

  /**
   * Test users.list api
   * @throws Exception
   */
  @Test
  public void testCallUsersList() throws Exception {
    Arvados arv = new Arvados("arvados");

    List<String> params = new ArrayList<String>();
    params.add("call");
    params.add("arvados");
    params.add("v1");
    params.add("users.list");

    String response = arv.call(params);
    assertTrue("Expected users.list in response", response.contains("arvados#userList"));
    assertTrue("Expected users.list in response", response.contains("uuid"));

    JSONParser parser = new JSONParser();
    Object obj = parser.parse(response);
    JSONObject jsonObject = (JSONObject) obj;

    assertEquals("Expected kind to be users.list", "arvados#userList", jsonObject.get("kind"));

    List items = (List)jsonObject.get("items");
    assertNotNull("expected users list items", items);
    assertTrue("expected at least one item in users list", items.size()>0);

    JSONObject firstUser = (JSONObject)items.get(0);
    assertNotNull ("Expcted at least one user", firstUser);

    assertEquals("Expected kind to be user", "arvados#user", firstUser.get("kind"));
    assertNotNull("Expected uuid for first user", firstUser.get("uuid"));
  }

  /**
   * Test users.get <uuid> api
   * @throws Exception
   */
  @Test
  public void testCallUsersGet() throws Exception {
    Arvados arv = new Arvados("arvados");

    // call user.system and get uuid of this user
    List<String> params = new ArrayList<String>();
    params.add("call");
    params.add("arvados");
    params.add("v1");
    params.add("users.list");

    String response = arv.call(params);
    JSONParser parser = new JSONParser();
    Object obj = parser.parse(response);
    JSONObject jsonObject = (JSONObject) obj;
    assertNotNull("expected users list", jsonObject);
    List items = (List)jsonObject.get("items");
    assertNotNull("expected users list items", items);

    JSONObject firstUser = (JSONObject)items.get(0);
    String userUuid = (String)firstUser.get("uuid");

    // invoke users.get with the system user uuid
    params = new ArrayList<String>();
    params.add("call");
    params.add("arvados");
    params.add("v1");
    params.add("users.get");
    params.add(userUuid);

    response = arv.call(params);

    //JSONParser parser = new JSONParser();
    jsonObject = (JSONObject) parser.parse(response);;
    assertNotNull("Expected uuid for first user", jsonObject.get("uuid"));
    assertEquals("Expected system user uuid", userUuid, jsonObject.get("uuid"));
  }

  /**
   * Test users.create api
   * @throws Exception
   */
  @Test
  public void testCreateUser() throws Exception {
    Arvados arv = new Arvados("arvados");

    File file = new File(getClass().getResource( "/create_user.json" ).toURI());
    String filePath = file.getPath();

    List<String> params = new ArrayList<String>();
    params.add("call");
    params.add("arvados");
    params.add("v1");
    params.add("users.create");
    params.add(filePath);
    String response = arv.call(params);

    JSONParser parser = new JSONParser();
    JSONObject jsonObject = (JSONObject) parser.parse(response);
    assertEquals("Expected kind to be user", "arvados#user", jsonObject.get("kind"));
    assertNotNull("Expected uuid for first user", jsonObject.get("uuid"));
  }

  /**
   * Test unsupported api version api
   * @throws Exception
   */
  @Test
  public void testUnsupportedApiName() throws Exception {
    Arvados arv = new Arvados("not_arvados");

    List<String> params = new ArrayList<String>();
    params.add("call");
    params.add("not_arvados");
    params.add("v1");
    params.add("users.list");

    Exception caught = null;
    try {
      arv.call(params);
    } catch (Exception e) {
      caught = e;
    }

    assertNotNull ("expected exception", caught);
    assertTrue ("Expected 404 when unsupported api is used", caught.getMessage().contains("404 Not Found"));
  }

  /**
   * Test unsupported api version api
   * @throws Exception
   */
  @Test
  public void testUnsupportedVersion() throws Exception {
    Arvados arv = new Arvados("arvados");

    List<String> params = new ArrayList<String>();
    params.add("call");
    params.add("arvados");
    params.add("v2");         // no such version
    params.add("users.list");

    Exception caught = null;
    try {
      arv.call(params);
    } catch (Exception e) {
      caught = e;
    }

    assertNotNull ("expected exception", caught);
    assertTrue ("Expected 404 when unsupported version is used", caught.getMessage().contains("404 Not Found"));
  }
  
  /**
   * Test unsupported api version api
   * @throws Exception
   */
  @Test
  public void testCallWithTooFewParams() throws Exception {
    Arvados arv = new Arvados("arvados");

    List<String> params = new ArrayList<String>();
    params.add("call");
    params.add("arvados");
    params.add("v1");

    Exception caught = null;
    try {
      arv.call(params);
    } catch (Exception e) {
      caught = e;
    }

    assertNotNull ("expected exception", caught);
    assertTrue ("Expected ERROR: missing method name", caught.getMessage().contains("ERROR: missing method name"));
  }
  
  /**
   * Test pipeline_tempates.create api
   * @throws Exception
   */
  @Test
  public void testCreateAndGetPipelineTemplate() throws Exception {
    Arvados arv = new Arvados("arvados");

    File file = new File(getClass().getResource( "/first_pipeline.json" ).toURI());
    String filePath = file.getPath();

    List<String> params = new ArrayList<String>();
    params.add("call");
    params.add("arvados");
    params.add("v1");
    params.add("pipeline_templates.create");
    params.add(filePath);
    String response = arv.call(params);

    JSONParser parser = new JSONParser();
    JSONObject jsonObject = (JSONObject) parser.parse(response);
    assertEquals("Expected kind to be user", "arvados#pipelineTemplate", jsonObject.get("kind"));
    String uuid = (String)jsonObject.get("uuid");
    assertNotNull("Expected uuid for pipeline template", uuid);
    
    // get the pipeline
    params = new ArrayList<String>();
    params.add("call");
    params.add("arvados");
    params.add("v1");
    params.add("pipeline_templates.get");
    params.add(uuid);
    response = arv.call(params);

    parser = new JSONParser();
    jsonObject = (JSONObject) parser.parse(response);
    assertEquals("Expected kind to be user", "arvados#pipelineTemplate", jsonObject.get("kind"));
    assertEquals("Expected uuid for pipeline template", uuid, jsonObject.get("uuid"));
  }


}
package org.redpill.alfresco.unzip;

import static com.jayway.restassured.RestAssured.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.redpill.alfresco.test.AbstractRepoFunctionalTest;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;

public abstract class AbstractUnzipFunctionalTest extends AbstractRepoFunctionalTest {

  protected void unzipDocument(String zipFileNodeRef, String documentLibraryNodeRef, boolean async) {
    RestAssured.requestContentType(ContentType.JSON);
    RestAssured.responseContentType(ContentType.JSON);
    
    given()
      .baseUri(getBaseUri())
      .pathParameter("source", zipFileNodeRef)
      .pathParameter("target", documentLibraryNodeRef)
      .pathParameter("async", async ? "true" : "false")
      .expect()
      .contentType(ContentType.JSON).and().statusCode(200)
      .when().post("/org/redpill/unzip?source={source}&target={target}&async={async}");
  }

  public void assertNodeExists(String nodeRef) {
    RestAssured.requestContentType(ContentType.JSON);
    RestAssured.responseContentType(ContentType.JSON);

    Response response = given()
        .baseUri(getBaseUri())
        .expect().statusCode(200)
        .when().get("/api/metadata?nodeRef=" + nodeRef);
    
    String result = response.path("nodeRef");
    
    assertNotNull("Node with nodeRef '" + nodeRef + "' does not exist!", result);
  }

  public void assertNodeExists(String name, String parentNodeRef) {
    RestAssured.requestContentType(ContentType.JSON);
    RestAssured.responseContentType(ContentType.JSON);
    
    String storeType = StringUtils.split(parentNodeRef, "/")[0];
    storeType = StringUtils.replace(storeType, ":", "");
    
    String storeId = StringUtils.split(parentNodeRef, "/")[1];
    
    String id = StringUtils.split(parentNodeRef, "/")[2];
       
    String uri = "/slingshot/doclib/doclist/{type}/node/{store_type}/{store_id}/{id}";

    Response response = given()
        .baseUri(getBaseUri())
        .pathParameter("type", "all")
        .pathParameter("store_type", storeType)
        .pathParameter("store_id", storeId)
        .pathParameter("id", id)
        .expect().statusCode(200)
        .when().get(uri);
    
    List<Map<String, Object>> list = response.path("items");
    
    boolean matches = false;
    
    for (Map<String, Object> item : list) {
      String filename = (String) item.get("fileName");
      
      if (filename.equalsIgnoreCase(name)) {
        matches = true;
      }
    }
    
    assertTrue("Node with parent nodeRef '" + parentNodeRef + "' and name '" + name + "' does not exist!", matches);
  }

}

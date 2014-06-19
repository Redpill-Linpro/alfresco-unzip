package org.redpill.alfresco.unzip;

import static com.jayway.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

import java.io.InputStream;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;

public abstract class AbstractRepoFunctionalTest {

  public static final String BASE_URI = "http://localhost:8080/alfresco/service";

  protected void unzipDocument(String zipFileNodeRef, String documentLibraryNodeRef) {
    RestAssured.requestContentType(ContentType.JSON);
    RestAssured.responseContentType(ContentType.JSON);
    
    given()
      .baseUri(BASE_URI)
      .pathParameter("source", zipFileNodeRef)
      .pathParameter("target", documentLibraryNodeRef)
      .expect()
      .contentType(ContentType.JSON).and().statusCode(200)
      .when().post("/org/redpill/unzip?source={source}&target={target}");
  }

  protected String uploadDocument(String filename, String site) {
    RestAssured.requestContentType("multipart/form-data");
    RestAssured.responseContentType(ContentType.JSON);
  
    InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
    
    return given()
        .baseUri(BASE_URI)
        .multiPart("filedata", filename, inputStream)
        .formParam("filename", filename)
        .formParam("siteid", site)
        .formParam("containerid", "documentLibrary")
        .expect().statusCode(200)
        .when().post("/api/upload").path("nodeRef");
  }

  protected void createSite(String shortName) {
    RestAssured.requestContentType(ContentType.JSON);
    RestAssured.responseContentType(ContentType.JSON);
  
    String visibility = "PRIVATE";
    String title = "Demoplats";
    String description = "This is a description";
    String siteDashboard = "site-dashboard";
  
    given()
        .baseUri(BASE_URI)
        .body(
            "{\"visibility\":\"" + visibility + "\",\"title\":\"" + title + "\",\"shortName\":\"" + shortName + "\",\"description\":\"" + description + "\",\"sitePreset\":\"" + siteDashboard + "\"}")
        .expect()
        .contentType(ContentType.JSON)
        .and().statusCode(200)
        .and().body("shortName", equalTo(shortName))
        .when().post("/api/sites");
  }

  protected void deleteSite(String shortName) {
    RestAssured.requestContentType(ContentType.JSON);
    RestAssured.responseContentType(ContentType.JSON);
  
    given().baseUri(BASE_URI).expect().statusCode(200).when().delete("/api/sites/" + shortName);
  }

  protected String getDocumentLibraryNodeRef(String site) {
    RestAssured.requestContentType(ContentType.JSON);
    RestAssured.responseContentType(ContentType.JSON);
  
    Response response = given().pathParameter("shortName", site).pathParam("container", "documentLibrary").baseUri(BASE_URI).expect().statusCode(200).when()
        .get("/slingshot/doclib/container/{shortName}/{container}");
  
    return response.path("container.nodeRef");
  }

}

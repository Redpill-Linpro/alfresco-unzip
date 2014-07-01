package org.redpill.alfresco.unzip;

import static com.jayway.restassured.RestAssured.*;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

public abstract class AbstractUnzipFunctionalTest extends AbstractRepoFunctionalTest {

  protected void unzipDocument(String zipFileNodeRef, String documentLibraryNodeRef, boolean async) {
    RestAssured.requestContentType(ContentType.JSON);
    RestAssured.responseContentType(ContentType.JSON);
    
    given()
      .baseUri(BASE_URI)
      .pathParameter("source", zipFileNodeRef)
      .pathParameter("target", documentLibraryNodeRef)
      .pathParameter("async", async ? "true" : "false")
      .expect()
      .contentType(ContentType.JSON).and().statusCode(200)
      .when().post("/org/redpill/unzip?source={source}&target={target}&async={async}");
  }

}

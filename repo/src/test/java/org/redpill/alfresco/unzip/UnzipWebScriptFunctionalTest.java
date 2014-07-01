package org.redpill.alfresco.unzip;

import static com.jayway.restassured.RestAssured.*;
import static org.redpill.alfresco.unzip.RestAssert.*;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jayway.restassured.RestAssured;

public class UnzipWebScriptFunctionalTest extends AbstractUnzipFunctionalTest {

  @Before
  public void setUp() {
    RestAssured.authentication = preemptive().basic("admin", "admin");
  }

  @After
  public void tearDown() {
    RestAssured.reset();
  }

  @Test
  public void testUnzipSync() throws IOException {
    String site = "site_name_" + System.currentTimeMillis();
    String filename = "documents.zip";

    createSite(site);

    try {
      String zipFileNodeRef = uploadDocument(filename, site);

      String documentLibraryNodeRef = getDocumentLibraryNodeRef(site);

      unzipDocument(zipFileNodeRef, documentLibraryNodeRef, false);

      assertNodeExists(zipFileNodeRef);
      assertNodeExists(documentLibraryNodeRef);
      
      assertNodeExists("file1.txt", documentLibraryNodeRef);
      assertNodeExists("file2.txt", documentLibraryNodeRef);
      assertNodeExists("folder1", documentLibraryNodeRef);
    } finally {
      deleteSite(site);
    }
  }

  @Test
  public void testUnzipAsync() throws IOException {
    String site = "site_name_" + System.currentTimeMillis();
    String filename = "documents.zip";

    createSite(site);

    try {
      String zipFileNodeRef = uploadDocument(filename, site);

      String documentLibraryNodeRef = getDocumentLibraryNodeRef(site);

      unzipDocument(zipFileNodeRef, documentLibraryNodeRef, true);

      assertNodeExists(zipFileNodeRef);
      assertNodeExists(documentLibraryNodeRef);
      
      assertNodeExists("file1.txt", documentLibraryNodeRef);
      assertNodeExists("file2.txt", documentLibraryNodeRef);
      assertNodeExists("folder1", documentLibraryNodeRef);
    } finally {
      deleteSite(site);
    }
  }

}

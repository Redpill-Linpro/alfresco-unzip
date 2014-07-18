package org.redpill.alfresco.unzip;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.alfresco.util.TempFileProvider;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang.StringUtils;
import org.redpill.alfresco.test.AbstractRepoIntegrationTest;

public abstract class AbstractUnzipIntegrationTest extends AbstractRepoIntegrationTest {

  public File createTestZipFile() {
    return createTestZipFile(null);
  }

  public File createTestZipFile(String encoding) {
    OutputStream outputStream = null;

    try {
      File zipfile = TempFileProvider.createTempFile("zip_output", ".zip");

      outputStream = new FileOutputStream(zipfile);

      ZipArchiveOutputStream logicalZip = (ZipArchiveOutputStream) new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, outputStream);

      if (StringUtils.isNotBlank(encoding)) {
        logicalZip.setEncoding(encoding);
      }

      addFileToZip("file1.txt", logicalZip);
      addFileToZip("file2.txt", logicalZip);
      addFileToZip("folder.with.dots.in.the.name/", logicalZip);
      addFileToZip("folder.with.dots.in.the.name/file5.txt", logicalZip);
      addFileToZip("folder.with.dots.in.the.name/file4.txt", logicalZip);
      addFileToZip("folder1/", logicalZip);
      addFileToZip("folder1/file3.txt", logicalZip);
      addFileToZip("file_ending_with_a_dot.", logicalZip);
      // addFileToZip("1. Överenskommelser/5. Utskickade tåkar under 2013/608 Primärvården VGR PNV Tåk FM-kontor Lokalvård PV Mölnlycke barnmott, Mödrabarnhälsa i haga, Psykologi.pdf", logicalZip);
      
      logicalZip.finish();

      return zipfile;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    } finally {
      IOUtils.closeQuietly(outputStream);
    }
  }

  private void addFileToZip(String filePath, ZipArchiveOutputStream outputStream) {
    try {
      outputStream.putArchiveEntry(new ZipArchiveEntry(filePath));

      IOUtils.copy(new NullInputStream(1), outputStream);

      outputStream.closeArchiveEntry();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

}

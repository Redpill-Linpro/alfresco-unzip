package org.redpill.alfresco.unzip;

import org.alfresco.service.namespace.QName;

public interface UnzipModel {

  static final String WORKFLOW_URI = "http://www.redpill-linpro.org/unzip/model/workflow/1.0";
  static final String CORE_URI = "http://www.redpill-linpro.org/unzip/model/core/1.0";

  static final String WORKFLOW_SHORT_PREFIX = "unzipw";
  static final String CORE_SHORT_PREFIX = "unzip";

  static final QName ASSOC_UNZIPPED_DOCUMENT = QName.createQName(WORKFLOW_URI, "unzippedDocument");

  static final QName ASSOC_ZIP_DOCUMENT = QName.createQName(CORE_URI, "zipdocument");

  static final QName ASPECT_UNZIPPING_FINISHED = QName.createQName(WORKFLOW_URI, "unzippingFinished");

  static final QName ASPECT_UNZIPPED_FROM = QName.createQName(CORE_URI, "unzippedfrom");

}

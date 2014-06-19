package org.redpill.alfresco.unzip;

import org.alfresco.service.cmr.repository.NodeRef;

public interface UnzipService {

  void importZip(NodeRef zipFileNodeRef, NodeRef destinationFolderNodeRef);

}

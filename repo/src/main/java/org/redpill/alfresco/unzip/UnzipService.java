package org.redpill.alfresco.unzip;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

public interface UnzipService {

  List<NodeRef> importZip(NodeRef zipFileNodeRef, NodeRef destinationFolderNodeRef);

}

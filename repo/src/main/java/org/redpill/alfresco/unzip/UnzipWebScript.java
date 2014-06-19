/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * Copyright (C) 2013-2014 Redpill Linpro AB
 *
 * This file is part of Unzip Here module for Alfresco
 *
 * Unzip Here module for Alfresco is free software:
 * you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Unzip Here module for Alfresco is distributed in the
 * hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Unzip Here module for Alfresco.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package org.redpill.alfresco.unzip;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.util.ParameterCheck;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.util.Assert;

/**
 * WebScript for handling "unzip" requests for files
 * 
 * @author Marcus Svensson <marcus.svensson@redpill-linpro.com>
 * @author Niklas Ekman <niklas.ekman@redpill-linppro.com>
 */
public class UnzipWebScript extends DeclarativeWebScript implements InitializingBean {

  private static final Logger LOG = Logger.getLogger(UnzipWebScript.class);

  private NodeService _nodeService;

  private UnzipService _unzipService;

  @Override
  protected Map<String, Object> executeImpl(final WebScriptRequest req, final Status status, final Cache cache) {
    Map<String, Object> result = new HashMap<String, Object>();
    
    if (LOG.isTraceEnabled()) {
      LOG.trace("Entering executeImpl...");
    }

    String source = req.getParameter("source");
    String target = req.getParameter("target");

    if (LOG.isTraceEnabled()) {
      LOG.trace("Source: " + source + ", Target: " + target);
    }

    if (source == null) {
      status.setCode(400);
      status.setMessage("unzip.exception.source.null");
      return result;
    } else if (target == null) {
      status.setCode(400);
      status.setMessage("unzip.exception.target.null");
      return result;
    } else if (!NodeRef.isNodeRef(source)) {
      status.setCode(400);
      status.setMessage("unzip.exception.source.nodeRef");
      return result;
    } else if (!NodeRef.isNodeRef(target)) {
      status.setCode(400);
      status.setMessage("unzip.exception.target.nodeRef");
      return result;
    }

    NodeRef sourceNodeRef = new NodeRef(source);
    NodeRef targetNodeRef = new NodeRef(target);

    if (!_nodeService.exists(sourceNodeRef)) {
      status.setCode(404);
      status.setMessage("unzip.exception.source.notExists");
      return result;
    } else if (!_nodeService.exists(targetNodeRef)) {
      status.setCode(404);
      status.setMessage("unzip.exception.target.notExists");
      return result;
    } else {
      try {
        _unzipService.importZip(sourceNodeRef, targetNodeRef);
      } catch (AlfrescoRuntimeException re) {
        Throwable cause = re.getCause();
        if (cause instanceof FileExistsException) {
          status.setCode(500);
          status.setMessage("unzip.exception.target.alreadyExists");
          LOG.error(cause.getMessage(), cause);
          return result;
        } else {
          throw re;
        }
      }
    }

    if (LOG.isTraceEnabled()) {
      LOG.trace("Leaving executeImpl...");
    }
    return result;
  }

  public void setNodeService(NodeService nodeService) {
    _nodeService = nodeService;
  }

  public void setUnzipService(UnzipService unzipService) {
    _unzipService = unzipService;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    ParameterCheck.mandatory("unzipService", _unzipService);
    Assert.notNull(_nodeService, "NodeService must not be null");
  }

}

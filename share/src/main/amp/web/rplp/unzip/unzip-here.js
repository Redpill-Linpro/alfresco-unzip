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
(function() {

   var $html = Alfresco.util.encodeHTML;

   /**
    * Unzip a zip-archive to the current directory
    * 
    * @method onUnzipHere
    * @param record
    *           {object} record to be actioned
    */
   YAHOO.Bubbling.fire("registerAction", {
      actionName : "onUnzipHere",

      fn : function(record) {
         var executeUnzipCommand = function(async) {
            var targetNodeRef = record.parent.nodeRef;
            var sourceNodeRef = record.nodeRef;

            // loading message function
            var loadingMessage = null;
            var fnShowLoadingMessage = function() {
               loadingMessage = Alfresco.util.PopupManager.displayMessage({
                  displayTime : 0,
                  text : '<span class="wait">' + $html(this.msg("message.loading")) + '</span>',
                  noEscape : true
               });
            };

            // slow data webscript message
            var timerShowLoadingMessage = YAHOO.lang.later(1000, this, fnShowLoadingMessage);

            var successHandler = function(request, response, payload) {
               if (timerShowLoadingMessage) {
                  timerShowLoadingMessage.cancel();
               }

               if (loadingMessage) {
                  loadingMessage.destroy();
               }
            };

            var url = Alfresco.constants.PROXY_URI + "org/redpill/unzip?source=" + sourceNodeRef + "&target=" + targetNodeRef + "&async=" + (async ? "true" : "false");
            
            var successMessage = null;
            if (async) {
               successMessage = Alfresco.util.message("label.unzip.success.async");               
            } else {
               successMessage = Alfresco.util.message("label.unzip.success.sync");               
            }

            Alfresco.util.Ajax.jsonPost({
               url : url,
               successCallback : {
                  fn : function(res) {
                     Alfresco.util.PopupManager.displayMessage({
                        displayTime : 2,
                        text : successMessage,
                        noEscape : true
                     });

                     YAHOO.Bubbling.fire("folderCreated", {
                        name : "",
                        parentNodeRef : targetNodeRef
                     });

                     if (timerShowLoadingMessage) {
                        timerShowLoadingMessage.cancel();
                     }

                     if (loadingMessage) {
                        loadingMessage.destroy();
                     }
                  },
                  scope : this
               },
               failureCallback : {
                  fn : function(res) {
                     var message;

                     if (res.json.status.message == undefined || res.json.status.message == null) {
                        message = res.json.message;
                     } else {
                        message = res.json.status.message;
                     }

                     Alfresco.util.PopupManager.displayMessage({
                        displayTime : 5,
                        text : Alfresco.util.message(message),
                        noEscape : true
                     });

                     if (timerShowLoadingMessage) {
                        timerShowLoadingMessage.cancel();
                     }

                     if (loadingMessage) {
                        loadingMessage.destroy();
                     }
                  },
                  scope : this
               }
            });
         }

         Alfresco.util.PopupManager.displayPrompt({
            title : Alfresco.util.message("title.unzipDocument", this.name),
            text : Alfresco.util.message("label.confirmUnzipDocument", this.name),
            noEscape : true,
            buttons : [ {
               text : Alfresco.util.message("button.async", this.name),
               handler : function() {
                  executeUnzipCommand(true);
                  this.destroy();
               }
            }, {
               text : Alfresco.util.message("button.sync", this.name),
               handler : function() {
                  executeUnzipCommand(false);
                  this.destroy();
               },
               isDefault : true
            }, {
               text : Alfresco.util.message("button.cancel", this.name),
               handler : function() {
                  this.destroy();
               }
            } ]
         });

      }
   });
})();
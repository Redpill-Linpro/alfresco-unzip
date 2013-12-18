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
(function()
{
  var $html = Alfresco.util.encodeHTML;
  /**
   * Unzip a zip-archive to the current directory
   *
   * @method onUnzipHere
   * @param record {object} record to be actioned
   */
  YAHOO.Bubbling.fire("registerAction",
  {
    actionName: "onUnzipHere",
    fn: function rplp_onUnzipHere(record)
    {
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
      Alfresco.util.Ajax.jsonPost({
        url : Alfresco.constants.PROXY_URI + "org/redpill_linpro/unzip?source=" + sourceNodeRef + "&target=" + targetNodeRef,
        successCallback : {
          fn : function(res) {
            Alfresco.util.PopupManager.displayMessage({
              displayTime : 2,
              text : Alfresco.util.message("label.unzip.success"),
              noEscape : true
            });
            YAHOO.Bubbling.fire("folderCreated", {
              name: "",
              parentNodeRef: targetNodeRef
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
  });
})();
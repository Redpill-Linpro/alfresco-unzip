<html>
   <head>
      <style type="text/css"><!--
      body
      {
         font-family: Arial, sans-serif;
         font-size: 14px;
         color: #4c4c4c;
      }
      
      a, a:visited
      {
         color: #0072cf;
      }
      
      .document a
      {
         text-decoration: none;
      }
      
      .document a:hover
      {
         text-decoration: underline;
      }
      --></style>
   </head>
   
   <body bgcolor="#dddddd">
      <table width="100%" cellpadding="20" cellspacing="0" border="0" bgcolor="#dddddd">
         <tr>
            <td width="100%" align="center">
               <table width="70%" cellpadding="0" cellspacing="0" bgcolor="white" style="background-color: white; border: 1px solid #aaaaaa;">
                  <tr>
                     <td width="100%">
                        <table width="100%" cellpadding="0" cellspacing="0" border="0">
                           <tr>
                              <td style="padding: 20px 30px 0px;">
                                 <table width="100%" cellpadding="0" cellspacing="0" border="0">
                                    <tr>
                                       <td>
                                          <div style="font-size: 22px; padding-bottom: 4px; color: red;">
                                             ${message("unzip.email.title.failure")}
                                          </div>
                                          <div style="font-size: 13px; margin-bottom: 10px;">
                                             ${date?datetime?string.full}
                                          </div>
                                          <#if site?exists>
                                          <div style="font-size: 13px;">
                                             <#assign siteLink="<a href=\"${shareUrl}/page/site/${siteShortName}/dashboard\">${siteTitle}</a>">
                                             ${message("unzip.email.site", siteLink)}
                                          </div>
                                          </#if>
                                          <div style="font-size: 13px;">
                                             <#assign zipFileLink="<a href=\"${shareUrl}/page/document-details?nodeRef=${zipFileNodeRef}\">${zipFileName}</a>">
                                             ${message("unzip.email.zip_file", zipFileLink)}
                                          </div>
                                          <div style="font-size: 13px;">
                                             <#assign folderLink="<a href=\"${shareUrl}//page/site/${siteShortName}/documentlibrary#filter=path|${folderPath}|&page=1\">${folderName}</a>">
                                             ${message("unzip.email.folder", folderLink)}
                                          </div>
                                          <div style="font-size: 14px; margin: 18px 0px 24px 0px; padding-top: 18px; border-top: 1px solid #aaaaaa;">
                                             <p>${message("unzip.email.description.failure")}</p>
                                             <#if errorMessage?exists>
                                             <p>${message("unzip.email.error_message", errorMessage)}</p>
                                             </#if>
                                          </div>
                                       </td>
                                    </tr>
                                 </table>
                              </td>
                           </tr>
                           <tr>
                              <td>
                                 <div style="border-top: 1px solid #aaaaaa;">&nbsp;</div>
                              </td>
                           </tr>
                           <tr>
                              <td style="padding: 10px 30px;">
                                 <img src="${shareUrl}/themes/default/images/app-logo.png" alt="" width="117" height="48" border="0" />
                              </td>
                           </tr>
                        </table>
                     </td>
                  </tr>
               </table>
            </td>
         </tr>
      </table>
   </body>
</html>

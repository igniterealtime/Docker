<%@ page import="java.util.*" %>
<%@ page import="org.ifsoft.docker.openfire.*" %>
<%@ page import="org.jivesoftware.openfire.*" %>
<%@ page import="org.jivesoftware.util.*" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%

    boolean update = request.getParameter("update") != null;
    String errorMessage = null;
    
    String ipaddr = XMPPServer.getInstance().getServerInfo().getHostname();
    String httpPort = JiveGlobals.getProperty("httpbind.port.secure", "7443");
    String docserverUrl = "https://" + ipaddr + ":" + httpPort;    

    // Get handle on the plugin
    PluginImpl plugin = (PluginImpl) XMPPServer.getInstance().getPluginManager().getPlugin("docker");

    if (update)
    {                               
        String dockerEnabled = request.getParameter("dockerEnabled");
        JiveGlobals.setProperty("docker.enabled", (dockerEnabled != null && dockerEnabled.equals("on")) ? "true": "false");  
        JiveGlobals.setProperty("docker.ipaddr", request.getParameter("dockerIpAddr")); 
        
        JiveGlobals.setProperty("docker.docserver.port", request.getParameter("docserverPort"));         
        JiveGlobals.setProperty("docker.docserver.url", request.getParameter("docserverUrl"));    
        
        String archiveMetadata = request.getParameter("archiveMetadata");
        JiveGlobals.setProperty("docker.docserver.archive.metadata", (archiveMetadata != null && archiveMetadata.equals("on")) ? "true": "false");  

        String archiveChat = request.getParameter("archiveChat");
        JiveGlobals.setProperty("docker.docserver.archive.chat", (archiveChat != null && archiveChat.equals("on")) ? "true": "false");  
        
    }

%>
<html>
<head>
   <title><fmt:message key="plugin.title.description" /></title>

   <meta name="pageID" content="docker-settings"/>
</head>
<body>
<% if (errorMessage != null) { %>
<div class="error">
    <%= errorMessage%>
</div>
<br/>
<% } %>

<div class="jive-table">
<form action="docker.jsp" method="post">
    <p>
        <table class="jive-table" cellpadding="0" cellspacing="0" border="0" width="100%">
            <thead> 
            <tr>
                <th colspan="2"><fmt:message key="config.page.settings.description"/></th>
            </tr>
            </thead>
            <tbody>  
            <tr>
                <td nowrap  colspan="2">
                    <input type="checkbox" name="dockerEnabled"<%= JiveGlobals.getBooleanProperty("docker.enabled", true) ? " checked" : "" %>>
                    <fmt:message key="config.page.configuration.enabled" />       
                </td>  
            </tr>  
            <tr>
            <td align="left" width="150">
                <fmt:message key="settings.docker.ipaddr"/>
            </td>
            <td><input type="text" size="50" maxlength="100" name="dockerIpAddr"
                   value="<%= JiveGlobals.getProperty("docker.ipaddr", ipaddr) %>">
            </td>
            </tr>  
        </table>
    </p>
    <p>
        <table class="jive-table" cellpadding="0" cellspacing="0" border="0" width="100%">
            <thead> 
            <tr>
                <th colspan="2"><fmt:message key="config.page.docserver.title"/></th>
            </tr>
            </thead>
            <tbody>             
            <tr>
            <td align="left" width="150">
                <fmt:message key="settings.docker.docserver.port"/>
            </td>
            <td><input type="text" size="50" maxlength="100" name="docserverPort"
                   value="<%= JiveGlobals.getProperty("docker.docserver.port", "32771") %>">
            </td>
            </tr>   
            <tr>
            <td align="left" width="150">
                <fmt:message key="settings.docker.docserver.url"/>
            </td>
            <td><input type="text" size="50" maxlength="100" name="docserverUrl"
                   value="<%= JiveGlobals.getProperty("docker.docserver.url", docserverUrl) %>">
            </td>
            </tr>       
            <tr>
                <td nowrap  colspan="2">
                    <input type="checkbox" name="archiveMetadata"<%= JiveGlobals.getBooleanProperty("docker.docserver.archive.metadata", false) ? " checked" : "" %>>
                    <fmt:message key="settings.docker.docserver.archive.metadata" />       
                </td>  
            </tr>    
            <tr>
                <td nowrap  colspan="2">
                    <input type="checkbox" name="archiveChat"<%= JiveGlobals.getBooleanProperty("docker.docserver.archive.chat", true) ? " checked" : "" %>>
                    <fmt:message key="settings.docker.docserver.archive.chat" />       
                </td>  
            </tr>              
            </tbody>
        </table>
    </p>
    <p>
        <table class="jive-table" cellpadding="0" cellspacing="0" border="0" width="100%">
            <thead> 
            <tr>
                <th colspan="2"><fmt:message key="config.page.configuration.save.title"/></th>
            </tr>
            </thead>
            <tbody>         
            <tr>
                <th colspan="2"><input type="submit" name="update" value="<fmt:message key="config.page.configuration.submit" />"><fmt:message key="config.page.configuration.restart.warning"/></th>
            </tr>       
            </tbody>            
        </table> 
    </p>
</form>
</div>
</body>
</html>

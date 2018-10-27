<%@ page import="org.jivesoftware.util.*,
         org.jivesoftware.openfire.*,
         org.ifsoft.docker.openfire.*,
                 java.util.*,
                 java.net.URLEncoder"                 
    errorPage="error.jsp"
%>
<%@ page import="org.xmpp.packet.JID" %>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager"  />

<% 
    webManager.init(request, response, session, application, out ); 
    String hostname = JiveGlobals.getProperty("network.interface", XMPPServer.getInstance().getServerInfo().getHostname());
    String port = JiveGlobals.getBooleanProperty("docker.portainer.secure", false) ? JiveGlobals.getProperty("httpbind.port.secure", "7443") : JiveGlobals.getProperty("httpbind.port.plain", "7070");
    String protocol = JiveGlobals.getBooleanProperty("docker.portainer.secure", false) ? "https://" : "http://";
    
    String url = protocol + hostname + ":" + port + "/docker";   
%>

<html>
<head>
<title>Docker Management Admin</title>
<meta name="pageID" content="docker-webui"/>
<style type="text/css">
    #jive-main table, #jive-main-content {
        height: 92%;
    }
</style>
<meta http-equiv="refresh" content="0;URL=<%= url %>">
</head>
<body>
<!-- TODO blocked by https://github.com/portainer/portainer/issues/2279
<iframe frameborder='0' style='border:0px; border-width:0px; margin-left: 0px; margin-top: 0px; margin-right: 0px; margin-bottom: 0px; width:100%;height:100%;' src='<%= url %>'></iframe>
-->
</body>
</html>

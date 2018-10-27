package org.ifsoft.docker.openfire;

import java.io.File;
import java.io.FileInputStream;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import java.nio.file.*;
import java.nio.charset.Charset;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.http.HttpBindManager;
import org.jivesoftware.openfire.XMPPServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.PropertyEventDispatcher;
import org.jivesoftware.util.PropertyEventListener;

import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.servlets.*;
import org.eclipse.jetty.servlet.*;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.servlet.*;
import org.eclipse.jetty.websocket.server.*;
import org.eclipse.jetty.websocket.server.pathmap.ServletPathSpec;
import org.eclipse.jetty.util.security.*;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.security.authentication.*;


import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;

import java.lang.reflect.*;
import java.util.*;
import com.github.dockerjava.core.*;
import com.github.dockerjava.core.command.*;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.*;
import com.github.dockerjava.api.model.*;

import org.jitsi.util.OSUtils;
import org.ifsoft.websockets.*;

public class PluginImpl implements Plugin, PropertyEventListener
{
    private static final Logger Log = LoggerFactory.getLogger(PluginImpl.class);

    private ServletContextHandler dockerContext = null;
    private ServletContextHandler docserverContext = null;
    private ServletContextHandler docserverContext2 = null;
    private WebAppContext docappContext;
    private DockerClient dockerClient;
    private String portainerId = null;


    public void destroyPlugin()
    {
        PropertyEventDispatcher.removeListener(this);

        try {
            if (portainerId != null) dockerClient.stopContainerCmd(portainerId).exec();
            if (dockerContext != null) HttpBindManager.getInstance().removeJettyHandler(dockerContext);
            if (docserverContext != null) HttpBindManager.getInstance().removeJettyHandler(docserverContext);
            if (docserverContext2 != null) HttpBindManager.getInstance().removeJettyHandler(docserverContext2);
            if (docappContext != null) HttpBindManager.getInstance().removeJettyHandler(docappContext);
        }
        catch (Exception e) {
            //Log.error("Docker destroyPlugin ", e);
        }
    }

    public void initializePlugin(final PluginManager manager, final File pluginDirectory)
    {
        PropertyEventDispatcher.addListener(this);
        boolean dockerEnabled = JiveGlobals.getBooleanProperty("docker.enabled", true);

        if (dockerEnabled)
        {
            dockerClient = DockerClientBuilder.getInstance("tcp://localhost:2375").build();

            List<Container> containers = dockerClient.listContainersCmd().withShowSize(true).withShowAll(true).exec();

            for (Container container : containers)
            {
                Log.info("Container " +  container.getId() + " " + container.getCommand());

                if (container.getCommand().indexOf("/portainer") > -1) portainerId = container.getId();
            }

            try {
                if (portainerId == null)
                {
                    CreateVolumeResponse namedVolume = dockerClient.createVolumeCmd().withName("portainer_data").exec();

                    dockerClient.pullImageCmd("portainer/portainer")
                      .withTag("1.19.2")
                      .exec(new PullImageResultCallback())
                      .awaitCompletion(300, TimeUnit.SECONDS);

                    CreateContainerResponse container = dockerClient.createContainerCmd("portainer/portainer:1.19.2")
                        .withRestartPolicy(RestartPolicy.alwaysRestart())
                        .withName("portainer")
                        .withPortBindings(PortBinding.parse("9000:9000"))
                        .withBinds(Bind.parse("/var/run/docker.sock:/var/run/docker.sock"), Bind.parse("portainer_data:/data portainer/portainer"))
                        .exec();

                    portainerId = container.getId();
                }

                dockerClient.startContainerCmd(portainerId).exec();

            } catch (Exception e) {
                Log.error("initializePlugin", e);
            }

            addDockerProxy();
            addDocServerApp(pluginDirectory);
            addDocServerProxy();

        } else {
            Log.info("docker disabled");
        }
    }

    public String getIpAddress()
    {
        String ourHostname = XMPPServer.getInstance().getServerInfo().getHostname();
        String ourIpAddress = "127.0.0.1";

        try {
            ourIpAddress = InetAddress.getByName(ourHostname).getHostAddress();
        } catch (Exception e) {

        }

        return ourIpAddress;
    }

    private void addDocServerApp(File pluginDirectory)
    {
        Log.info("Initialize DocServerApp");

/*
        Path settingsProperties = Paths.get(pluginDirectory.getPath() + "/classes/docapp/WEB-INF/classes/settings.properties");

        String ipaddr = JiveGlobals.getProperty("docker.ipaddr", XMPPServer.getInstance().getServerInfo().getHostname());
        String httpPort = JiveGlobals.getProperty("httpbind.port.plain", "7070");
        String docserverUrl = JiveGlobals.getProperty("docker.docserver.url", "http://" + ipaddr + ":" + httpPort);

        List<String> lines = Arrays.asList(
                                            "# docapp properties",
                                            "filesize-max=5242880",
                                            "storage-folder=app_data",
                                            "files.docservice.viewed-docs=.pdf|.djvu|.xps",
                                            "files.docservice.edited-docs=.docx|.xlsx|.csv|.pptx|.ppsx|.txt",
                                            "files.docservice.convert-docs=.docm|.dotx|.dotm|.dot|.doc|.odt|.fodt|.ott|.xlsm|.xltx|.xltm|.xlt|.xls|.ods|.fods|.ots|.pptm|.ppt|.ppsm|.pps|.potx|.potm|.pot|.odp|.fodp|.otp|.rtf|.mht|.html|.htm|.epub",
                                            "files.docservice.timeout=120000",
                                            "files.docservice.url.converter=" + docserverUrl + "/v5.2.2-2/ConvertService.ashx",
                                            "files.docservice.url.tempstorage=" + docserverUrl + "/v5.2.2-2/ResourceService.ashx",
                                            "files.docservice.url.api=" + docserverUrl + "/v5.2.2-2/web-apps/apps/api/documents/api.js",
                                            "files.docservice.url.preloader=" + docserverUrl + "/v5.2.2-2/web-apps/apps/api/documents/cache-scripts.html"
                                          );

        try {
            Files.write(settingsProperties, lines, Charset.forName("UTF-8"));
        } catch (Exception e) {
            Log.error("addDocServerApp write settings.properties fail", e);
        }
*/
        docappContext = new WebAppContext(null, pluginDirectory.getPath() + "/classes/docapp", "/docapp");

        if ( JiveGlobals.getBooleanProperty("docker.security.enabled", false ) )
        {
            Log.info("Initialize Docker WebService security");
            SecurityHandler securityHandler5 = basicAuth("docker");
            if (securityHandler5 != null) docappContext.setSecurityHandler(securityHandler5);
        }

        final List<ContainerInitializer> initializers6 = new ArrayList<>();
        initializers6.add(new ContainerInitializer(new JettyJasperInitializer(), null));
        docappContext.setAttribute("org.eclipse.jetty.containerInitializers", initializers6);
        docappContext.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
        docappContext.setWelcomeFiles(new String[]{"index.jsp"});
        HttpBindManager.getInstance().addJettyHandler(docappContext);
    }

    private void addDocServerProxy()
    {
        String version = JiveGlobals.getProperty("docker.docserver.version", "v5.2.2-2");
        String ipaddr = JiveGlobals.getProperty("docker.ipaddr", getIpAddress());
        String docserverPort = JiveGlobals.getProperty("docker.docserver.port", "32771");

        Log.info("Initialize DocServerProxy http://" + ipaddr + ":" + docserverPort + "/" + version);

        docserverContext = new ServletContextHandler(null, "/" + version, ServletContextHandler.SESSIONS);

        ServletHolder proxyServlet = new ServletHolder(ProxyServlet.Transparent.class);
        proxyServlet.setInitParameter("proxyTo", "http://" + ipaddr + ":" + docserverPort + "/" + version);
        proxyServlet.setInitParameter("prefix", "/");
        docserverContext.addServlet(proxyServlet, "/*");

        try {
            WebSocketUpgradeFilter wsfilter = WebSocketUpgradeFilter.configureContext(docserverContext);
            wsfilter.getFactory().getPolicy().setIdleTimeout(60 * 60 * 1000);
            wsfilter.getFactory().getPolicy().setMaxTextMessageSize(64000000);
            wsfilter.addMapping(new ServletPathSpec("/*"), new DockerSocketCreator());

        } catch (Exception e) {
            Log.error("addDocServerProxy", e);
        }

        HttpBindManager.getInstance().addJettyHandler(docserverContext);

        docserverContext2 = new ServletContextHandler(null, "/cache", ServletContextHandler.SESSIONS);
        ServletHolder proxyServlet2 = new ServletHolder(ProxyServlet.Transparent.class);
        proxyServlet2.setInitParameter("proxyTo", "http://" + ipaddr + ":" + docserverPort + "/cache");
        proxyServlet2.setInitParameter("prefix", "/");
        docserverContext2.addServlet(proxyServlet2, "/*");

        HttpBindManager.getInstance().addJettyHandler(docserverContext2);
    }

    private void addDockerProxy()
    {
        Log.info("Initialize DockerProxy");

        dockerContext = new ServletContextHandler(null, "/docker", ServletContextHandler.SESSIONS);
        String ipaddr = JiveGlobals.getProperty("docker.ipaddr", getIpAddress());
        ServletHolder proxyServlet = new ServletHolder(ProxyServlet.Transparent.class);
        proxyServlet.setInitParameter("proxyTo", "http://" + ipaddr + ":9000");
        proxyServlet.setInitParameter("prefix", "/");
        dockerContext.addServlet(proxyServlet, "/*");

        HttpBindManager.getInstance().addJettyHandler(dockerContext);
    }

    public static class DockerSocketCreator implements WebSocketCreator
    {
        @Override public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp)
        {
            String ipaddr = JiveGlobals.getProperty("docker.ipaddr", XMPPServer.getInstance().getServerInfo().getHostname());
            String docserverPort = JiveGlobals.getProperty("docker.docserver.port", "32771");

            HttpServletRequest request = req.getHttpServletRequest();
            String path = request.getRequestURI();
            String query = request.getQueryString();
            String protocol = null;

            for (String subprotocol : req.getSubProtocols())
            {
                Log.info("WSocketCreator found protocol " + subprotocol);
                protocol = subprotocol;
            }

            if (query != null) path += "?" + query;

            Log.info("Initialize DockerProxy " + path + " " + query);

            String url = "ws://" + ipaddr + ":" + docserverPort + path;

            ProxyWebSocket socket = null;
            ProxyConnection proxyConnection = new ProxyConnection(URI.create(url), protocol, 10000);

            socket = new ProxyWebSocket();
            socket.setProxyConnection(proxyConnection);
            if (protocol != null) resp.setAcceptedSubProtocol(protocol);
            return socket;
        }
    }

    private static final SecurityHandler basicAuth(String realm) {

        OpenfireLoginService l = new OpenfireLoginService();
        l.setName(realm);

        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{"docker", "webapp-owner", "webapp-contributor", "warfile-admin"});
        constraint.setAuthenticate(true);

        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/*");

        ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
        csh.setAuthenticator(new BasicAuthenticator());
        csh.setRealmName(realm);
        csh.addConstraintMapping(cm);
        csh.setLoginService(l);

        return csh;
    }

    //-------------------------------------------------------
    //
    //
    //
    //-------------------------------------------------------


    public void propertySet(String property, Map params)
    {

    }

    public void propertyDeleted(String property, Map<String, Object> params)
    {

    }

    public void xmlPropertySet(String property, Map<String, Object> params) {

    }

    public void xmlPropertyDeleted(String property, Map<String, Object> params) {

    }

}

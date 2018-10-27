/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
 * Copyright (C) 2005-2008 Jive Software. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ifsoft.websockets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;

import java.util.concurrent.*;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.net.*;
import javax.net.ssl.*;
import javax.security.auth.callback.*;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.openfire.XMPPServer;

public class ProxyConnection
{
    private static Logger Log = LoggerFactory.getLogger( "ProxyConnection" );
    private boolean isSecure = false;
    private ProxyWebSocket socket;
    private boolean connected = false;
    private WebSocketClient client = null;
    private ProxySocket proxySocket = null;
    private String subprotocol = null;

    public ProxyConnection(URI uri, String subprotocol, int connectTimeout)
    {
        Log.info("ProxyConnection " + uri + " " + subprotocol);

        this.subprotocol = subprotocol;

        SslContextFactory sec = new SslContextFactory();

        if("wss".equals(uri.getScheme()))
        {
            sec.setValidateCerts(false);

            Log.info("ProxyConnection - SSL");
            getSSLContext();
            isSecure = true;
        }

        client = new WebSocketClient(sec);
        proxySocket = new ProxySocket(this);

        try
        {
            client.start();
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            if (subprotocol != null) request.setSubProtocols(subprotocol);
            client.connect(proxySocket, uri, request);

            Log.info("Connecting to : " + uri);
        }
        catch (Exception e)
        {
            Log.error("ProxyConnection", e);
        }
        finally
        {
            try
            {
                //client.stop();
            }
            catch (Exception e1)
            {
                Log.error("ProxyConnection", e1);
            }
        }

        connected = true;
    }

    public void setSocket( ProxyWebSocket socket ) {
        this.socket = socket;
    }

    public void deliver(String text)
    {
        Log.info("ProxyConnection - deliver " + text);

        String sendText = text;

        if (proxySocket != null)
        {
            proxySocket.deliver(sendText);
        }
    }

    public void disconnect()
    {
        Log.info("ProxyConnection - disconnect");
        if (proxySocket != null) proxySocket.disconnect();
    }

    public void onClose(int code, String reason)
    {
        Log.info("ProxyConnection - onClose " + reason + " " + code);
        connected = false;

        if (this.socket != null) this.socket.disconnect();
    }

    public void onMessage(String text) {
        Log.info("ProxyConnection - onMessage " + text);

        try {
            this.socket.deliver(text);
        }

        catch (Exception e) {
            Log.error("deliverRawText error", e);
        }
    }

    public boolean isSecure() {
        return isSecure;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setSecure(boolean isSecure) {
        this.isSecure = isSecure;
    }

    private SSLContext getSSLContext()
    {
        SSLContext sc = null;

        try {
            Log.info("ProxyConnection SSL truster");

            TrustManager[] trustAllCerts = new TrustManager[]
            {
               new X509TrustManager() {
                  public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                  }

                  public void checkClientTrusted(X509Certificate[] certs, String authType) {  }

                  public void checkServerTrusted(X509Certificate[] certs, String authType) {  }

               }
            };

            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier()
            {
                public boolean verify(String hostname, SSLSession session) {
                  return true;
                }
            };
            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        } catch (Exception e)   {
            Log.error("WireLynkComponent - getSSLContext SSL truster", e);
        }

        return sc;
    }

    @WebSocket(maxTextMessageSize = 64 * 1024) public class ProxySocket
    {
        private Session session;
        private ProxyConnection proxyConnection;
        private String lastMessage = null;
        private String ipaddr = null;
        private String docserverPort = null;
        private String httpPort = null;
        private String docserverUrl = null;

        public ProxySocket(ProxyConnection proxyConnection)
        {
            this.proxyConnection = proxyConnection;
            this.docserverPort = JiveGlobals.getProperty("docker.docserver.port", "32771");
            this.ipaddr = JiveGlobals.getProperty("docker.ipaddr", XMPPServer.getInstance().getServerInfo().getHostname());
            this.httpPort = JiveGlobals.getProperty("httpbind.port.plain", "7070");
            this.docserverUrl = JiveGlobals.getProperty("docker.docserver.url", "http://" + ipaddr + ":" + httpPort);
        }

        @OnWebSocketError public void onError(Throwable t)
        {
            Log.error("Error: "  + t.getMessage(), t);
        }

        @OnWebSocketClose public void onClose(int statusCode, String reason)
        {
            Log.info("ProxySocket onClose " + statusCode + " " + reason);
            this.session = null;
            if (proxyConnection != null) proxyConnection.onClose(statusCode, reason);
        }

        @OnWebSocketConnect public void onConnect(Session session)
        {
            Log.info("ProxySocket onConnect: " + session);
            this.session = session;

            if (lastMessage != null) deliver(lastMessage);
        }

        @OnWebSocketMessage public void onMessage(String msg)
        {
            msg = msg.replace("http://" + ipaddr + ":" + docserverPort, docserverUrl);

            Log.info("ProxySocket onMessage \n" + msg);
            if (proxyConnection != null) proxyConnection.onMessage(msg);
        }

        public void deliver(String text)
        {
            if (session != null)
            {
                try {
                    Log.info("ProxySocket deliver: \n" + text);
                    session.getRemote().sendStringByFuture(text);
                    lastMessage = null;
                } catch (Exception e) {
                    Log.error("ProxySocket deliver", e);
                }
            } else lastMessage = text;
        }

        public void disconnect()
        {
            if (session != null) session.close(StatusCode.NORMAL,"I'm done");
        }
    }
}

# Docker Management for Openfire

This plugin enables Openfire to act as a super container to Docker by providing the following features

1. Expose the Docker Remote API (REST) to web applications (7070/7443) for web page automation
2. Provide a web based UI using [Portainer](https://portainer.io/) for admin
3. Use Openfire as a multiplexing reverse proxy for all incoming Docker web services. All webservices will reuse Openfire certificates and be externally available via Jetty.
4. Provide a multiplexing reverse proxy websocket proxy or all incoming Docker web socket connections. All websockets will reuse Openfire certificates and be externally available via Jetty.

<img src="https://github.com/igniterealtime/Docker/blob/master/docs/docker_screen_1.png" />

The plugin was created to run ONLYOFFICE Document Server as a Docker instance in order to support [collaborative document editiing](https://helpcenter.onlyoffice.com/guides/collaborative-editing.aspx) from [Pade](https://github.com/igniterealtime/Pade).

<img src="https://github.com/igniterealtime/Docker/blob/master/docs/docker_screen_2.png" />

The plugin can also be used in theory to provide a hosted multi-tenancy Openfire solution by running each domain as a Docker instance.



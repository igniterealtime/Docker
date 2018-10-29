# Docker Management for Openfire

This plugin enables Openfire to act as a super container to Docker by providing the following features

1. Provide a web based UI using [Portainer](https://portainer.io/)
2. Use Openfire as a multiplexing reverse proxy for all incoming Docker web services. All webservices will reuse Openfire certificates and be externally available via Jetty.
3. Provide a multiplexing reverse proxy websocket proxy or all incoming Docker web socket connections. All websockets will reuse Openfire certificates and be externally available via Jetty.

The plugin was created to run ONLYOFFICE Document Server as a docker instance in order to support [collaborative document editiing](https://helpcenter.onlyoffice.com/guides/collaborative-editing.aspx) from [Pade](https://github.com/igniterealtime/Pade).

The plugin can also be used in theeory to provide a hosted multi-tenancy Openfire solution by running each domain as a Docker instance



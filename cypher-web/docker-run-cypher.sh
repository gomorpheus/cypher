#!/bin/bash
if [[ -f "/etc/certs/cert.p12" ]]; then
	java -XX:MaxRAMPercentage=30 -jar cypher-web.jar -micronaut.server.netty.use-native-transport=true -micronaut.ssl.enabled=true -micronaut.ssl.key=/etc/certs/cert.p12 -micronaut.ssl.key.password=$MORPHEUS_SSL_PASSWORD -micronaut.ssl.key.alias=$MORPHEUS_SSL_ALIAS -morpheus.key=$MORPHEUS_KEY morpheus.appliance.url=$MORPHEUS_URL -micronaut.http.services.morpheus.urls=$MORPHEUS_URL -morpheus.worker.key=$MORPHEUS_WORKER_KEY
elif [ "$MORPHEUS_SELF_SIGNED" = true ]; then
 	java -XX:MaxRAMPercentage=30 -jar cypher-web.jar -micronaut.server.netty.use-native-transport=true -morpheus.key=$MORPHEUS_KEY  -morpheus.appliance.url=$MORPHEUS_URL -micronaut.http.services.morpheus.urls=$MORPHEUS_URL -micronaut.ssl.enabled=true -micronaut.ssl.build-self-signed=true -morpheus.worker.key=$MORPHEUS_WORKER_KEY
else
	java -XX:MaxRAMPercentage=30 -jar cypher-web.jar -micronaut.server.netty.use-native-transport=true -morpheus.key=$MORPHEUS_KEY -morpheus.appliance.url=$MORPHEUS_URL -micronaut.http.services.morpheus.urls=$MORPHEUS_URL -morpheus.worker.key=$MORPHEUS_WORKER_KEY
fi



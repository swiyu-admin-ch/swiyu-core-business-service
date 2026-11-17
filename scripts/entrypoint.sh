#!/bin/bash

#
# SPDX-FileCopyrightText: 2025 Swiss Confederation
#
# SPDX-License-Identifier: MIT
#

# Function to handle termination signals
term_handler() {
    echo "Received termination signal, forwarding to Java process..."
    if [ "$child_pid" -ne 0 ]; then
        kill -TERM "$child_pid"
        wait "$child_pid"
    fi
    exit 0
}

# Trap termination signals
trap 'term_handler' SIGTERM SIGINT

# Add microservice CAs
echo "Adding certificates to Java truststore:"
if ls /certs-app/*.crt &> /dev/null; then
    for f in /certs-app/*.crt; do
        CERT=$f
        CERT_ALIAS=$(basename "$CERT" .crt)
        echo " => adding $CERT as $CERT_ALIAS to truststore"
        $JAVA_HOME/bin/keytool -importcert -file "$CERT" -alias "$CERT_ALIAS" -cacerts -storepass changeit -noprompt -trustcacerts
    done
else
    echo " => No certificates found, skipping"
fi

additional_spring_config="classpath:application.yml"
echo "INFO: Load secrets and config"
for configfile in /vault/secrets/*.yml; do
  [ -e "$configfile" ] || continue
  echo "INFO: Add $configfile to additional spring config"
  additional_spring_config="$additional_spring_config,file:$configfile"
done

# Start the Java application in the background and capture its PID
java -Duser.timezone=Europe/Zurich \
-Dspring.config.location=${additional_spring_config} \
-Dspring.profiles.active="${MY_SPRING_PROFILES}" \
-Djavax.net.ssl.trustStore=/usr/local/openjdk/lib/security/cacerts \
-Djavax.net.ssl.trustStorePassword=changeit \
-Dhttp.proxyHost=${HTTP_PROXY} \
-Dhttp.proxyPort=8080 \
-Dhttps.proxyHost=${HTTPS_PROXY} \
-Dhttps.proxyPort=8080 \
-Dhttp.nonProxyHosts="${NO_PROXY}" \
-jar "$1" &
child_pid=$!

# Wait for the Java process to complete
wait "$child_pid"
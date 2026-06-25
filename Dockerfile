FROM bit-base-images-docker-hosted.nexus.bit.admin.ch/bit/eclipse-temurin:25-jre-ubi9-minimal

USER 0
EXPOSE 8080

WORKDIR /app

ENV JAR_FILE=swiyu-core-business-service-app.jar

ADD https://repo.bit.admin.ch/repository/raw-jeap-hosted/rhos/entrypoint/entrypoint-1.0.0.sh entrypoint.sh

# Update Java truststore
RUN set -uxe && \
    chmod g=u ./entrypoint.sh &&\
    chmod +x ./entrypoint.sh &&\
    chmod g=u $JAVA_HOME/lib/security/cacerts

COPY swiyu-core-business-service-app/target/${JAR_FILE} /app/

USER 1001

ENTRYPOINT "./entrypoint.sh" "${JAR_FILE}"

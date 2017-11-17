FROM lifeway/alpine-java:8u102b14_server-jre-ca

ENV LANG=C

RUN adduser -s /bin/bash -D service && addgroup service service && \
    mkdir /opt/service

EXPOSE 9000

ADD ./target/universal/fraudapi-*.tgz /opt/service
RUN ls -la /opt/service && \
    chown -R service:service /opt/service && \
        ln -s /opt/service/fraudapi* /opt/service/fraudapi

USER service

ENTRYPOINT ["/opt/service/fraudapi/bin/fraudapi", \
      "-J-Djava.security.egd=file:/dev/./urandom", \
      "-Dpidfile.path=/dev/null", \
      "-J-Xms512m", \
      "-J-Xmx512m" ]

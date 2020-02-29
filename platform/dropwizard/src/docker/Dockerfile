# On OpenJDK 8 Slim Debian Image

FROM openjdk:8-jre-alpine
LABEL maintainer="Aaron Coburn <acoburn@apache.org>"

ADD trellis-*.tar /opt
RUN mv /opt/trellis-* /opt/trellis
COPY command.sh /
COPY config.yml /opt/trellis/etc/
RUN chmod +x /command.sh

CMD ["/command.sh"]

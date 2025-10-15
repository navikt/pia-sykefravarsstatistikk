FROM cgr.dev/chainguard/jre:latest
ENV TZ="Europe/Oslo"
ENV JAVA_TOOL_OPTIONS="-XX:+UseParallelGC -XX:MaxRAMPercentage=75"
COPY build/install/app/ /app/
CMD ["-jar", "pia-sykefravarsstatistikk.jar"]
ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.pia.sykefravarsstatistikk.ApplicationKt"]

FROM cgr.dev/chainguard/jre:latest
ENV TZ="Europe/Oslo"
ENV JAVA_TOOL_OPTIONS="-XX:+UseParallelGC -XX:MaxRAMPercentage=75"
COPY build/libs/pia-sykefravarsstatistikk-all.jar /app/app.jar
WORKDIR /app
CMD ["-jar", "app.jar"]

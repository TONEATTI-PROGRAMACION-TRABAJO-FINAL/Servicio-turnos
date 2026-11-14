# Build: compiles the jar without the tests, which run in CI.
FROM eclipse-temurin:25.0.5_8-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
# The Maven repository is a BuildKit cache mount: it speeds up rebuilds and ends up in no layer.
# The glob takes the executable jar, not the .jar.original that spring-boot-maven-plugin leaves next to it.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -Dmaven.test.skip=true package \
    && cp target/servicio-turnos-*.jar app.jar

# Runtime: only the JRE and the jar, as a non-root user. All the configuration comes from environment variables.
FROM eclipse-temurin:25.0.5_8-jre
RUN groupadd --system turnos \
    && useradd --system --gid turnos --no-create-home --shell /usr/sbin/nologin turnos
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
USER turnos
# Spring Boot default: the service does not set server.port.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

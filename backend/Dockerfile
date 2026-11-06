# syntax=docker/dockerfile:1
# One Dockerfile for every backend service. Pick the service with --build-arg SERVICE=<name>.

# ---- build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
ARG SERVICE
WORKDIR /workspace
COPY . .
# the cache mount shares downloaded Maven dependencies between the 5 service builds
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl ${SERVICE} -am package -DskipTests

# ---- run stage ----
FROM eclipse-temurin:17-jre
ARG SERVICE
WORKDIR /app
COPY --from=build /workspace/${SERVICE}/target/app.jar app.jar
ENV JAVA_OPTS="-Xms128m -Xmx384m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

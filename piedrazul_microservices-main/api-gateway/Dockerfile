FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl \
    && addgroup -S piedrazul && adduser -S piedrazul -G piedrazul
COPY --from=build /app/target/*.jar app.jar
USER piedrazul
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]

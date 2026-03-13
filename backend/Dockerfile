# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Install curl for Maven wrapper
RUN apk add --no-cache curl

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies first (better Docker layer caching)
RUN ./mvnw dependency:go-offline -q

COPY src ./src

# Build jar
RUN ./mvnw package -DskipTests -q


# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S recoflow && adduser -S recoflow -G recoflow

COPY --from=build /app/target/*.jar app.jar

USER recoflow

EXPOSE 8080

ENTRYPOINT ["java","-jar","-Djava.security.egd=file:/dev/./urandom","app.jar"]
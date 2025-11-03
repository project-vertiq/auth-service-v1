# Importing JDK and copying required files
FROM openjdk:21-jdk-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src src

# Copy Maven wrapper
COPY mvnw .
COPY .mvn .mvn

# Set execution permission for the Maven wrapper
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests


FROM openjdk:21-jdk-slim
VOLUME /tmp


COPY ./target/auth-service-v1-0.0.1-SNAPSHOT.jar /app/auth-service-v1.jar
ENTRYPOINT ["java","-jar","auth-service-v1.jar"]
EXPOSE 8081
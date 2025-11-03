# Use an official OpenJDK 21 image as a base
FROM openjdk:21-jdk-slim

# Set the working directory in the Docker container
WORKDIR /app

# Copy the built jar file into the Docker container
COPY ./target/auth-service-v1-0.0.1-SNAPSHOT.jar /app/auth-service-v1.jar

# Expose the port the app runs on
EXPOSE 8081

# Command to run the application
CMD ["java", "-jar", "auth-service-v1.jar"]
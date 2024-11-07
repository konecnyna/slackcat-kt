# Use Gradle with JDK 17 as the base image
FROM gradle:7.6-jdk17 AS builder

# Set the working directory in the container
WORKDIR /app

# Copy the entire project into the Docker container
COPY . /app

# Switch to a smaller JDK image for the final container
FROM openjdk:17-jdk-slim

# Set the working directory for the final container
WORKDIR /app

# Copy all files from the builder stage
COPY --from=builder /app /app
# COPY --from=builder /app/app/build/libs/*-all.jar /app/app.jar

# Expose port (optional, only if your application needs it)
EXPOSE 8080

# Start a shell so you can manually run commands inside the container
ENTRYPOINT ["/bin/sh"]
# ENTRYPOINT ["java", "-jar", "/app/app.jar"]
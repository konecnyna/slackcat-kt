# Use Gradle with JDK 17 as the base image
FROM gradle:7.6-jdk17 AS builder

# Set the working directory in the container
WORKDIR /app

# Copy Gradle wrapper and settings files first for dependency caching
COPY settings.gradle.kts build.gradle.kts /app/
COPY gradle /app/gradle

# Copy the source code of all modules
COPY . .

# Build the project, compiling all modules and creating JAR files
RUN gradle assemble --no-daemon

# Use a smaller final image
FROM openjdk:17-jdk-slim

# Set the working directory for the app
WORKDIR /app

# Copy all JARs from each module's build/libs folder
COPY --from=builder /app/*/build/libs/*.jar /app/libs/

# Expose port 8080 (adjust if needed)
EXPOSE 8080

# Run the application. Adjust to run multiple JARs if required
ENTRYPOINT ["java", "-cp", "/app/libs/*", "com.slackcat.app.MainKt"]
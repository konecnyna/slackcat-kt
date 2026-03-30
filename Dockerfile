# Use Gradle with JDK 21 as the base image
FROM gradle:8.5-jdk21 AS builder

# Set the working directory in the container
WORKDIR /app

# Copy the entire project into the Docker container
COPY . /app

RUN gradle :app:shadowJar --no-daemon

# Extract version from AppVersion.kt for labeling
RUN MAJOR=$(grep "const val MAJOR" buildSrc/src/main/kotlin/AppVersion.kt | sed 's/.*= \([0-9]*\).*/\1/') && \
    MINOR=$(grep "const val MINOR" buildSrc/src/main/kotlin/AppVersion.kt | sed 's/.*= \([0-9]*\).*/\1/') && \
    PATCH=$(grep "const val PATCH" buildSrc/src/main/kotlin/AppVersion.kt | sed 's/.*= \([0-9]*\).*/\1/') && \
    echo "$MAJOR.$MINOR.$PATCH" > /app/VERSION

# Switch to a smaller JDK image for the final container
FROM eclipse-temurin:21-jre-alpine

# Set the working directory for the final container
WORKDIR /app

# Copy all files from the builder stage
COPY --from=builder /app/app/build/libs/*.jar /app/app.jar
COPY --from=builder /app/VERSION /app/VERSION

ARG APP_VERSION=unknown
LABEL org.opencontainers.image.version="${APP_VERSION}"

# Expose port (optional, only if your application needs it)
EXPOSE 8080

# Start a shell so you can manually run commands inside the container
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
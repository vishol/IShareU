# Use a multi-arch compatible OpenJDK 17 image
FROM eclipse-temurin:17-jre

# Set the working directory
WORKDIR /app

# Copy the built jar file into the container
COPY target/file-sharing-app-0.0.1-SNAPSHOT.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"] 
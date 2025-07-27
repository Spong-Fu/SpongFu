# =================================================================================================
# Stage 1: Build the application
#
# This stage uses a full JDK image to build the Spring Boot application.
# It copies the source code and uses Maven to create an executable JAR file.
# =================================================================================================
FROM eclipse-temurin:21-jdk-jammy AS builder

# Set the working directory
WORKDIR /app

# Copy the Maven wrapper and pom.xml to leverage Docker layer caching
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download Maven dependencies
RUN ./mvnw dependency:go-offline

# Copy the rest of the application source code
COPY src ./src

# Build the application, skipping the tests
RUN ./mvnw package -DskipTests

# =================================================================================================
# Stage 2: Create the final, optimized image
#
# This stage uses a smaller JRE image for the runtime environment.
# It copies the built JAR from the 'builder' stage and sets up a non-root user for security.
# =================================================================================================
FROM eclipse-temurin:21-jre-jammy

# Set the working directory
WORKDIR /app

# Create a non-root user and group
RUN groupadd -r spring && useradd -r -s /bin/false -g spring spring

# Set the user and group
USER spring:spring

# Copy the executable JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8080

# The command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
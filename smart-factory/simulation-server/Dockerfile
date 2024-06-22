# Copied and adjusted from: https://git.uibk.ac.at/informatik/dps/dps-dc-software/cirrina/-/blob/develop/Dockerfile?ref_type=heads
# Stage 1: Build Stage
FROM gradle:8.7.0-jdk21-alpine AS build

# Install protoc
RUN apk add protoc

# Copy application source code into the container
COPY --chown=gradle:gradle . /usr/src/simulation-server

# Set working directory
WORKDIR /usr/src/simulation-server

# Build the application distribution ZIP
RUN gradle distZip

# Stage 2: Runtime Stage
FROM openjdk:21-bookworm

# Define a build argument for the application version
ARG VERSION="smart_factory-1.0-SNAPSHOT"

# Define default environment variables
ENV PROTO="true"
ENV DELAYS="false"
ENV ERROR_RATE="0"

# Copy the application distribution ZIP from the build stage
COPY --from=build /usr/src/simulation-server/build/distributions/${VERSION}.zip /tmp/${VERSION}.zip

# Unzip the application distribution to /usr/bin
RUN unzip /tmp/${VERSION}.zip -d /usr/bin \
    && mv /usr/bin/${VERSION} /usr/bin/simulation-server \
    && chmod +x /usr/bin/simulation-server/bin/smart_factory

# Set the working directory for the application
WORKDIR /usr/bin/simulation-server

# Use shell form ENTRYPOINT to execute the application
ENTRYPOINT /usr/bin/simulation-server/bin/smart_factory --useProto $PROTO --useDelays $DELAYS --errorRate $ERROR_RATE

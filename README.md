# Luxe Jewelry Store Shared Jenkins Library

This repository contains shared functions for Jenkins pipelines used across Luxe Jewelry Store projects.

## Available Functions

### `pushToNexus`

Pushes Docker images to Nexus repository.

```groovy
pushToNexus(
    registry: "192.168.1.117:8082",
    imageName: "luxe-jewelry-auth-service",
    version: "1.0.1",
    credentialsId: "Nexus-Docker"
)

// Alternative usage with sourceImage
pushToNexus(
    registry: "192.168.1.117:8082",
    sourceImage: "luxe-jewelry-auth-service:1.0.1",
    credentialsId: "Nexus-Docker"
)
```

### `buildDockerImage`

Builds Docker images with standardized options.

```groovy
buildDockerImage(
    imageName: "luxe-jewelry-auth-service",
    version: "1.0.1",
    dockerfile: "./dockerfile",
    context: ".",
    buildArgs: [BUILD_ARG1: "value1", BUILD_ARG2: "value2"]
)
```

### `pushToDockerHub`

Pushes Docker images to Docker Hub.

```groovy
pushToDockerHub(
    registry: "vixx3",
    imageName: "luxe-jewelry-auth-service",
    version: "1.0.1",
    credentialsId: "docker-hub"
)

// Alternative usage with sourceImage
pushToDockerHub(
    sourceImage: "luxe-jewelry-auth-service:1.0.1",
    registry: "vixx3",
    credentialsId: "docker-hub"
)
```

## Installation

1. In Jenkins, go to **Manage Jenkins** > **System Configuration** > **Global Pipeline Libraries**
2. Add a new library with these settings:
   - **Name**: `luxe-jewelry-lib`
   - **Default Version**: `main`
   - **Retrieval Method**: Modern SCM > GitHub
   - **Repository URL**: URL to this repository
   - **Credentials**: Add GitHub credentials if repository is private

## Usage in Jenkinsfile

```groovy
@Library('luxe-jewelry-lib') _

pipeline {
    agent any
    
    stages {
        stage('Build and Push') {
            steps {
                script {
                    // Build image
                    def image = buildDockerImage(
                        imageName: "my-app",
                        version: "1.0.${env.BUILD_NUMBER}"
                    )
                    
                    // Push to Docker Hub
                    pushToDockerHub(
                        sourceImage: image,
                        registry: "myorg",
                        credentialsId: "docker-hub"
                    )
                    
                    // Push to Nexus
                    pushToNexus(
                        sourceImage: image,
                        registry: "192.168.1.117:8082",
                        credentialsId: "Nexus-Docker"
                    )
                }
            }
        }
    }
}
```

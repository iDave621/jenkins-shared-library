#!/usr/bin/env groovy

/**
 * Push a Docker image to Docker Hub
 * 
 * @param config Map of configuration options:
 *   - registry: Docker Hub registry/namespace (default: user's Docker Hub username)
 *   - imageName: Name of the image to push without registry prefix
 *   - sourceImage: Full source image name with tag (alternative to imageName+version)
 *   - version: Image version/tag
 *   - credentialsId: Jenkins credential ID for Docker Hub
 * @return The full pushed image name with registry
 */
def call(Map config) {
    def credentialsId = config.credentialsId
    if (!credentialsId) {
        error "credentialsId is required for Docker Hub authentication"
    }
    
    // Handle different image specification methods
    def sourceImage = config.sourceImage
    def targetImageName
    def registry = config.registry
    
    if (!sourceImage && config.imageName && config.version) {
        // If registry is provided, use it as prefix
        if (registry) {
            targetImageName = "${registry}/${config.imageName}:${config.version}"
        } else {
            // No registry means we'll use the credential username
            targetImageName = "${config.imageName}:${config.version}"
        }
        sourceImage = "${config.imageName}:${config.version}"
    } else if (sourceImage) {
        // Extract image name and tag from full source image
        def parts = sourceImage.split(":")
        def name = parts[0]
        def tag = parts.size() > 1 ? parts[1] : "latest"
        if (registry) {
            targetImageName = "${registry}/${name}:${tag}"
        } else {
            targetImageName = sourceImage
        }
    } else {
        error "Either sourceImage or both imageName and version must be provided"
    }

    echo "Pushing ${sourceImage} to Docker Hub as ${targetImageName}"
    
    withCredentials([usernamePassword(credentialsId: credentialsId, 
                passwordVariable: 'DOCKER_PASSWORD', 
                usernameVariable: 'DOCKER_USERNAME')]) {
        
        // Create a temporary file to avoid variable interpolation issues
        writeFile file: 'docker_push.sh', text: '''#!/bin/bash
# Create Docker config with insecure registry settings
mkdir -p ~/.docker
echo '{"insecure-registries":["registry-1.docker.io","index.docker.io"]}' > ~/.docker/config.json

# Set Docker environment variables
export DOCKER_TLS_VERIFY=0

# Login to Docker Hub (using environment variables)
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin

# Tag and push will be added below
'''
        
        // Append tag and push commands without interpolation issues
        sh """
            # Add tag and push to script
            echo "docker tag ${sourceImage} ${targetImageName}" >> docker_push.sh
            echo "docker push ${targetImageName}" >> docker_push.sh
            
            # Make executable and run
            chmod +x docker_push.sh
            ./docker_push.sh
            
            # Clean up
            rm docker_push.sh
        """
    }
    
    return targetImageName
}

#!/usr/bin/env groovy

/**
 * Push a Docker image to Nexus repository
 * 
 * @param config Map of configuration options:
 *   - registry: Nexus registry URL with port (default: localhost:8081/repository/docker-nexus)
 *   - imageName: Name of the image to push without registry prefix
 *   - sourceImage: Full source image name with tag (alternative to imageName+version)
 *   - version: Image version/tag
 *   - credentialsId: Jenkins credential ID for Nexus (default: Nexus-Docker)
 * @return The full pushed image name with registry
 */
def call(Map config) {
    // Set default values
    def registry = config.registry ?: "localhost:8081/repository/docker-nexus"
    def credentialsId = config.credentialsId ?: "Nexus-Docker"
    
    // Handle different image specification methods
    def sourceImage = config.sourceImage
    def targetImageName
    
    if (!sourceImage && config.imageName && config.version) {
        sourceImage = "${config.imageName}:${config.version}"
        targetImageName = "${registry}/${config.imageName}:${config.version}"
    } else if (sourceImage) {
        // Extract image name and tag from full source image
        def parts = sourceImage.split(":")
        def name = parts[0]
        def tag = parts.size() > 1 ? parts[1] : "latest"
        targetImageName = "${registry}/${name}:${tag}"
    } else {
        error "Either sourceImage or both imageName and version must be provided"
    }

    echo "Pushing ${sourceImage} to Nexus as ${targetImageName}"
    
    withCredentials([usernamePassword(credentialsId: credentialsId, 
                passwordVariable: 'NEXUS_PASSWORD', 
                usernameVariable: 'NEXUS_USERNAME')]) {
        // Set environment variables for Docker to handle TLS issues
        withEnv(["DOCKER_TLS_VERIFY=0", "DOCKER_CLI_EXPERIMENTAL=enabled"]) {
            // Simple approach with direct Docker commands
            sh """
                # Tag the image for Nexus
                docker tag ${sourceImage} ${targetImageName}
                
                # Create insecure registry config
                mkdir -p ~/.docker
                echo '{"insecure-registries":["localhost:8081"]}' > ~/.docker/config.json
                
                # Login to Nexus repository
                echo "\${NEXUS_PASSWORD}" | docker login --password-stdin -u "\${NEXUS_USERNAME}" http://localhost:8081/repository/docker-nexus/
                
                # Push the image to Nexus
                docker push ${targetImageName}
            """
        }
    }
    
    return targetImageName
}

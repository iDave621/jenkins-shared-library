#!/usr/bin/env groovy

/**
 * Push a Docker image to Nexus repository
 * 
 * @param config Map of configuration options:
 *   - registry: Nexus registry URL with port (default: 192.168.1.117:8082)
 *   - imageName: Name of the image to push without registry prefix
 *   - sourceImage: Full source image name with tag (alternative to imageName+version)
 *   - version: Image version/tag
 *   - credentialsId: Jenkins credential ID for Nexus (default: Nexus-Docker)
 * @return The full pushed image name with registry
 */
def call(Map config) {
    // Set default values
    def registry = config.registry ?: "192.168.1.117:8082"
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
        sh """
            # Configure Docker for insecure registry
            mkdir -p ~/.docker
            echo '{"auths":{"'${registry}'":{"auth":"'\$(echo -n ${NEXUS_USERNAME}:${NEXUS_PASSWORD} | base64 -w 0)'"}}, "insecure-registries": ["'${registry}'"] }' > ~/.docker/config.json
            
            # Set environment variables for Docker
            export DOCKER_TLS_VERIFY=0
            
            # Tag the image for Nexus
            docker tag ${sourceImage} ${targetImageName}
            
            # Login to Nexus registry
            echo ${NEXUS_PASSWORD} | docker login -u ${NEXUS_USERNAME} --password-stdin ${registry}
            
            # Push image to Nexus
            docker push ${targetImageName}
        """
    }
    
    return targetImageName
}

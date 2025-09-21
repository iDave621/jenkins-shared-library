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
        // Use single quotes for the sh block to avoid Groovy string interpolation warning
        sh '''
            # Tag the image
            docker tag ''' + sourceImage + ' ' + targetImageName + '''
            
            # Create Docker config with insecure registry settings
            mkdir -p ~/.docker
            echo '{
  "insecure-registries": ["''' + registry + '''"],
  "experimental": "enabled"
}' > ~/.docker/config.json
            
            # Set Docker environment variables for insecure registry
            export DOCKER_TLS_VERIFY=0
            export DOCKER_CLI_EXPERIMENTAL=enabled
            
            # Login to registry (--password-stdin for secure password passing)
            echo "${NEXUS_PASSWORD}" | docker login --password-stdin -u "${NEXUS_USERNAME}" ''' + registry + ''' || true
            
            # Push image to Nexus
            docker push ''' + targetImageName + '''
        '''
    }
    
    return targetImageName
}

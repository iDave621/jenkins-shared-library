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
        
        sh """
            # Login to Docker Hub
            echo ${DOCKER_PASSWORD} | docker login -u ${DOCKER_USERNAME} --password-stdin
            
            # Tag if registry is specified and sourceImage differs from targetImageName
            if [ "${sourceImage}" != "${targetImageName}" ]; then
                docker tag ${sourceImage} ${targetImageName}
            fi
            
            # Push image to Docker Hub
            docker push ${targetImageName}
        """
    }
    
    return targetImageName
}

#!/usr/bin/env groovy

/**
 * Combined function to build and push Docker images to both Docker Hub and Nexus
 * Handles the complete CI workflow for Docker images
 * 
 * @param config Map of configuration options:
 *   - imageName: Name of the image to build (required)
 *   - version: Version/tag for the image (required)
 *   - dockerfile: Path to dockerfile (default: './dockerfile')
 *   - context: Build context (default: '.')
 *   - buildArgs: Additional build arguments as a Map
 *   - dockerHubCredentialsId: Jenkins credential ID for Docker Hub (required)
 *   - dockerHubRegistry: Docker Hub registry/namespace (default from credential)
 *   - nexusRegistry: Nexus registry URL with port (default: 192.168.1.117:8082)
 *   - nexusCredentialsId: Jenkins credential ID for Nexus (default: Nexus-Docker)
 *   - skipTests: Skip running tests on the image (default: false)
 *   - skipNexus: Skip pushing to Nexus (default: false)
 *   - skipDockerHub: Skip pushing to Docker Hub (default: false)
 * @return Map with docker hub and nexus image names
 */
def call(Map config) {
    def imageName = config.imageName
    def version = config.version
    def skipNexus = config.skipNexus ?: false
    def skipDockerHub = config.skipDockerHub ?: false
    
    if (!imageName || !version) {
        error "imageName and version parameters are required"
    }
    
    def result = [:]
    
    // Build the image
    def builtImage = buildDockerImage(
        imageName: imageName,
        version: version,
        dockerfile: config.dockerfile,
        context: config.context,
        buildArgs: config.buildArgs
    )
    
    result.builtImage = builtImage
    
    // Push to Docker Hub if not skipped
    if (!skipDockerHub) {
        if (!config.dockerHubCredentialsId) {
            error "dockerHubCredentialsId is required for pushing to Docker Hub"
        }
        
        result.dockerHubImage = pushToDockerHub(
            sourceImage: builtImage,
            registry: config.dockerHubRegistry,
            credentialsId: config.dockerHubCredentialsId
        )
    }
    
    // Push to Nexus if not skipped
    if (!skipNexus) {
        result.nexusImage = pushToNexus(
            sourceImage: builtImage,
            registry: config.nexusRegistry ?: "192.168.1.117:8082",
            credentialsId: config.nexusCredentialsId ?: "Nexus-Docker"
        )
    }
    
    return result
}

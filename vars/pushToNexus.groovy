#!/usr/bin/env groovy

def call(Map config) {
    def sourceImage = config.sourceImage
    def registry = "localhost:8081/repository/docker-nexus"
    def credentialsId = "Nexus-Docker"
    
    // Create target image name
    def parts = sourceImage.split(":")
    def name = parts[0]
    def tag = parts.size() > 1 ? parts[1] : "latest"
    def targetImage = "${registry}/${name}:${tag}"
    
    echo "Pushing ${sourceImage} to Nexus as ${targetImage}"
    
    withCredentials([usernamePassword(credentialsId: credentialsId, 
                passwordVariable: 'NEXUS_PASSWORD', 
                usernameVariable: 'NEXUS_USERNAME')]) {
        // Create a complete Docker daemon configuration to handle insecure registry
        sh """
            # Create Docker config with insecure registry settings
            mkdir -p ~/.docker
            echo '{"insecure-registries":["${registry}", "localhost:8081", "localhost:8082", "localhost:8083", "127.0.0.1:8081"]}' > ~/.docker/config.json
            
            # Set Docker environment variables
            export DOCKER_TLS_VERIFY=0
            export DOCKER_INSECURE_REGISTRY=${registry}
            
            # Try to work around TLS issues
            docker version || true
            
            # Tag the image
            docker tag ${sourceImage} ${targetImage}
            
            # Login to Nexus
            echo "\${NEXUS_PASSWORD}" | docker login --password-stdin -u "\${NEXUS_USERNAME}" ${registry}
            
            # Push the image
            docker push ${targetImage}
        """
    }
    
    return targetImage
}

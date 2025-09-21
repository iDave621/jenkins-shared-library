#!/usr/bin/env groovy

def call(Map config) {
    def sourceImage = config.sourceImage
    def credentialsId = config.credentialsId ?: "docker-hub"
    def registry = config.registry ?: "vixx3"
    
    // Create target image name
    def parts = sourceImage.split(":")
    def name = parts[0]
    def tag = parts.size() > 1 ? parts[1] : "latest"
    
    // Extract name without any existing registry prefix
    def nameParts = name.split("/")
    def baseImageName = nameParts.size() > 1 ? nameParts[nameParts.size()-1] : name
    
    def targetImage = "${registry}/${baseImageName}:${tag}"

    echo "Pushing ${sourceImage} to Docker Hub as ${targetImage}"
    
    withCredentials([usernamePassword(credentialsId: credentialsId, 
                passwordVariable: 'DOCKER_PASSWORD', 
                usernameVariable: 'DOCKER_USERNAME')]) {
        
        sh """
            # Login to Docker Hub
            echo \"${DOCKER_PASSWORD}\" | docker login -u \"${DOCKER_USERNAME}\" --password-stdin
            
            # Tag and push
            docker tag ${sourceImage} ${targetImage}
            docker push ${targetImage}
        """
    }
    
    return targetImage
}

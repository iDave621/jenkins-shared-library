#!/usr/bin/env groovy

def call(Map config) {
    // Extract required parameters
    def sourceImage = config.sourceImage
    def credentialsId = config.credentialsId
    
    // Simple validation
    if (!sourceImage) {
        error "sourceImage parameter is required"
    }
    
    echo "Pushing ${sourceImage} to Docker Hub"
    
    // Using withCredentials to get the Docker Hub password
    withCredentials([usernamePassword(credentialsId: credentialsId, 
                passwordVariable: 'DOCKER_PASSWORD', 
                usernameVariable: 'DOCKER_USERNAME')]) {
        
        // Simple direct approach - both commands in one shell script
        sh """
            # Login to Docker Hub (using the Jenkins credentials)
            echo "${DOCKER_PASSWORD}" | docker login -u "${DOCKER_USERNAME}" --password-stdin
            
            # Push the image right after logging in
            docker push ${sourceImage}
        """
    }
    
    return sourceImage
}

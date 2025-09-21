#!/usr/bin/env groovy

def call(Map config) {
    // Extract required parameters
    def sourceImage = config.sourceImage
    def credentialsId = config.credentialsId
    
    // Simple validation
    if (!sourceImage || !credentialsId) {
        error "sourceImage and credentialsId are required"
    }
    
    echo "Pushing ${sourceImage} to Docker Hub"
    
    // Use the credentials in a simple command
    withCredentials([usernamePassword(credentialsId: credentialsId, 
                passwordVariable: 'DOCKER_PASSWORD', 
                usernameVariable: 'DOCKER_USERNAME')]) {
        
        // Execute the Docker commands directly with proper environment variable handling
        sh '''
            # Login to Docker Hub using credentials
            echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
            
            # Additional debugging
            echo "Pushing image with direct command..."
        '''
        
        // Execute the push command separately to avoid variable expansion issues
        sh "docker push ${sourceImage}"
    }
    
    return sourceImage
}

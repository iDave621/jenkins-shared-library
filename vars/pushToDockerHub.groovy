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
        
        // Create a temporary script file to handle the Docker commands with proper environment
        writeFile file: 'docker_hub_push.sh', text: '''
#!/bin/bash
set -e

# Login to Docker Hub
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin

# The actual image will be passed as an argument to this script
docker push "$1"
'''
        
        // Make script executable and run it with the image name as argument
        sh "chmod +x docker_hub_push.sh && ./docker_hub_push.sh ${sourceImage} && rm docker_hub_push.sh"
    }
    
    return sourceImage
}

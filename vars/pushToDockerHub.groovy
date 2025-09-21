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
        
        // Create a temporary script with proper escaping
        def tempScript = "docker_push_${System.currentTimeMillis()}.sh"
        
        writeFile file: tempScript, text: '''
#!/bin/bash
set -e

# Login to Docker Hub
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
'''

        // Append the push command with the sourceImage variable
        sh "echo \"docker push ${sourceImage}\" >> ${tempScript}"
        
        // Run the script and clean up
        sh "chmod +x ${tempScript} && ./${tempScript} && rm ${tempScript}"
    }
    
    return sourceImage
}

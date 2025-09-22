#!/usr/bin/env groovy

def call(Map config) {
    def sourceImage = config.sourceImage
    // Registry URL with full repository path for push operations
    def registry = "localhost:8081/repository/docker-nexus"
    // Login URL with repository path
    def loginUrl = "http://localhost:8081/repository/docker-nexus/"
    def credentialsId = config.credentialsId ?: "Nexus-Docker"
    
    // Create target image name
    def parts = sourceImage.split(":")
    def name = parts[0].split("/").last() // Get just the image name without path
    def tag = parts.size() > 1 ? parts[1] : "latest"
    def targetImage = "${registry}/${name}:${tag}"
    
    echo "Pushing ${sourceImage} to Nexus as ${targetImage}"
    
    withCredentials([usernamePassword(credentialsId: credentialsId, 
                passwordVariable: 'NEXUS_PASSWORD', 
                usernameVariable: 'NEXUS_USERNAME')]) {
        // Simple 3-step approach: Tag, Login, Push
        
        // 1. Tag the image
        sh "docker tag ${sourceImage} ${targetImage}"
        
        // 2. Login to Nexus with proper URL format
        sh "echo \${NEXUS_PASSWORD} | docker login ${loginUrl} -u \${NEXUS_USERNAME} --password-stdin"
        
        // 3. Push the image
        sh "docker push ${targetImage}"
    }
    
    return targetImage
}

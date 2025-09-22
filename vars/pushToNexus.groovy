#!/usr/bin/env groovy

def call(Map config) {
    def sourceImage = config.sourceImage
    // Registry URL for Docker operations using V1 API port
    def registry = "localhost:8082"
    // Login URL for Docker (using Docker V1 API port)
    def loginUrl = "http://localhost:8082"
    def credentialsId = config.credentialsId ?: "Nexus-Docker"
    
    // Create target image name
    def parts = sourceImage.split(":")
    def name = parts[0].split("/").last() // Get just the image name without path
    def tag = parts.size() > 1 ? parts[1] : "latest"
    // Combine registry with name, avoiding path duplication
    def targetImage = "${registry}/${name}:${tag}"
    
    // Fix any double slashes that might occur from path concatenation
    targetImage = targetImage.replace("//", "/")
    
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

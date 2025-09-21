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
        withEnv(["DOCKER_TLS_VERIFY=0"]) {
            sh """
                docker tag ${sourceImage} ${targetImage}
                echo "\${NEXUS_PASSWORD}" | docker login --password-stdin -u "\${NEXUS_USERNAME}" ${registry}
                docker push ${targetImage}
            """
        }
    }
    
    return targetImage
}

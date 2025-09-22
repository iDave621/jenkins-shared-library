#!/usr/bin/env groovy

def call(Map config) {
    def sourceImage = config.sourceImage
    // Direct hardcoded URLs for simplicity
    def nexusUiUrl = "http://localhost:8081"
    def registry = "localhost:8082"
    def loginUrl = "http://localhost:8082"
    def credentialsId = config.credentialsId ?: "Nexus-Docker"
    
    // Create target image name
    def parts = sourceImage.split(":")
    def name = parts[0].split("/").last() // Get just the image name without path
    // Use provided tag or fail if not provided
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
        
        // 2. Ensure Docker is properly configured for Nexus and login
        sh """
            # Force logout first to clear any stale credentials
            docker logout ${registry} || true
            
            # Configure Docker daemon for insecure registries
            mkdir -p ~/.docker
            cat > ~/.docker/config.json << EOF
            {
                "insecure-registries": ["${registry}"],
                "auths": {
                    "${registry}": {}
                }
            }
            EOF
            chmod 600 ~/.docker/config.json
            
            # Login with explicit credentials
            echo \${NEXUS_PASSWORD} | docker login ${loginUrl} -u \${NEXUS_USERNAME} --password-stdin
            
            # Brief pause to ensure login is processed
            sleep 2
        """
        
        // 3. Push the image with retry logic
        def pushAttempts = 0
        def maxAttempts = 3
        def success = false
        
        while (!success && pushAttempts < maxAttempts) {
            pushAttempts++
            try {
                // Echo attempt number
                echo "Push attempt ${pushAttempts}/${maxAttempts} for ${targetImage}"
                
                // Try to push with increased timeout
                sh """
                    # Attempt to push with timeout
                    timeout 120s docker push ${targetImage}
                """
                
                // If we get here, push was successful
                success = true
                echo "Successfully pushed ${targetImage} on attempt ${pushAttempts}"
            } catch (Exception e) {
                if (pushAttempts >= maxAttempts) {
                    echo "All ${maxAttempts} push attempts failed. Last error: ${e.message}"
                    throw e
                } else {
                    echo "Push attempt ${pushAttempts} failed: ${e.message}. Retrying..."
                    // Wait before retry with exponential backoff
                    sleep(pushAttempts * 5)
                    
                    // Re-login before retry
                    sh "echo \${NEXUS_PASSWORD} | docker login ${loginUrl} -u \${NEXUS_USERNAME} --password-stdin"
                }
            }
        }
    }
    
    return targetImage
}

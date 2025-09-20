#!/usr/bin/env groovy

/**
 * Build a Docker image with standard options
 * 
 * @param imageName Name of the image to build (without tag)
 * @param version Version/tag for the image
 * @param dockerfile Path to dockerfile (default: './dockerfile')
 * @param context Build context (default: '.')
 * @param buildArgs Additional build arguments as a Map
 * @return The full built image name with tag
 */
def call(Map config) {
    def imageName = config.imageName
    def version = config.version ?: "latest"
    def dockerfile = config.dockerfile ?: "./dockerfile"
    def context = config.context ?: "."
    def buildArgs = config.buildArgs ?: [:]
    
    if (!imageName) {
        error "imageName parameter is required"
    }
    
    def fullImageName = "${imageName}:${version}"
    def buildArgsStr = buildArgs.collect { k, v -> "--build-arg ${k}=${v}" }.join(" ")
    
    echo "Building Docker image: ${fullImageName}"
    
    sh """
        docker build -t ${fullImageName} -f ${dockerfile} ${buildArgsStr} ${context}
    """
    
    return fullImageName
}

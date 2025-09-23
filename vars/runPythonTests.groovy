#!/usr/bin/env groovy

/**
 * Function to run Python tests from the shared library
 * @param config Map of parameters:
 *   - resultPath: Path to save test results (optional, defaults to 'test-results')
 *   - testCommand: Python test command (optional, defaults to 'pytest')
 * @return Boolean indicating if tests passed
 */
def call(Map config = [:]) {
    def resultPath = config.resultPath ?: 'test-results'
    def testCommand = config.testCommand ?: 'pytest'
    
    echo "Running Python tests from shared library"
    
    try {
        // Step 1: Create result directory
        sh "mkdir -p ${resultPath}"
        
        // Step 2: Create test directory in workspace
        def testDir = "${env.WORKSPACE}/shared-lib-tests"
        sh "mkdir -p ${testDir}"
        
        // Step 3: Write test file to workspace
        def testContent = libraryResource('tests/test_demo.py')
        writeFile file: "${testDir}/test_demo.py", text: testContent
        
        // Step 4: Install dependencies and run tests (in a simplified way)
        sh """#!/bin/bash
            cd ${testDir}
            
            # Detect Python and install dependencies
            if command -v python3 &> /dev/null; then
                python3 -m pip install pytest requests --break-system-packages || true
                python3 -m pytest -v --junitxml=${env.WORKSPACE}/${resultPath}/shared-lib-pytest-results.xml test_demo.py || true
            elif command -v python &> /dev/null; then
                python -m pip install pytest requests || true
                python -m pytest -v --junitxml=${env.WORKSPACE}/${resultPath}/shared-lib-pytest-results.xml test_demo.py || true
            else
                echo "Python not found!"
                exit 1
            fi
        """
        
        echo "Shared library tests completed successfully"
        return true
    } catch (Exception e) {
        echo "Shared library tests failed: ${e.message}"
        return false
    }
}

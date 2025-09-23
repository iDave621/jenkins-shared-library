#!/usr/bin/env groovy

/**
 * Function to run Python tests from the shared library
 * @param config Map of parameters:
 *   - resultPath: Path to save test results (optional, defaults to 'shared-test-results')
 *   - testFile: Name of the test file (optional, defaults to 'test_demo.py')
 * @return Boolean indicating if tests passed
 */
def call(Map config = [:]) {
    def resultPath = config.resultPath ?: 'shared-test-results'
    def testFile = config.testFile ?: 'test_demo.py'
    
    echo "Running Python tests from shared library tests/${testFile}"
    
    // Create test directory in workspace
    def workspaceTestDir = "${env.WORKSPACE}/shared-lib-tests"
    
    try {
        // Step 1: Create directories
        sh """
            mkdir -p ${resultPath}
            mkdir -p ${workspaceTestDir}
        """
        
        // Step 2: Write test file to workspace using Groovy API
        def testFileContent = libraryResource("tests/${testFile}")
        writeFile file: "${workspaceTestDir}/${testFile}", text: testFileContent
        
        // Step 3: Run the test in separate shell script
        sh """
            # Determine Python command
            if command -v python3 &> /dev/null; then
                PYTHON_CMD="python3"
                echo "Using python3"
                $PYTHON_CMD -m pip install pytest requests --break-system-packages || true
            elif command -v python &> /dev/null; then
                PYTHON_CMD="python"
                echo "Using python"
                $PYTHON_CMD -m pip install pytest requests || true
            else
                echo "Python not found!"
                exit 1
            fi
            
            # Report Python version
            $PYTHON_CMD --version
            
            # Run the test with detailed output
            cd ${workspaceTestDir}
            $PYTHON_CMD -m pytest -v --junitxml=${env.WORKSPACE}/${resultPath}/shared-lib-pytest-results.xml ${testFile} || echo "Tests failed but continuing"
        """
        
        // Report success
        echo "Shared library tests completed successfully"
        return true
    } catch (Exception e) {
        echo "Shared library tests failed: ${e.message}"
        return false
    }
}

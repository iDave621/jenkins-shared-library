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
        // Create results and test directories
        sh """
            mkdir -p ${resultPath}
            mkdir -p ${workspaceTestDir}
        """
        
        // Write the test file content directly to workspace
        writeFile file: "${workspaceTestDir}/${testFile}", text: libraryResource("tests/${testFile}")
        
        // Run the test
        sh """
            # Determine Python command
            if command -v python3 &> /dev/null; then
                python3 -m pip install pytest requests --break-system-packages || true
                TEST_PY="python3"
            elif command -v python &> /dev/null; then
                python -m pip install pytest requests || true
                TEST_PY="python"
            else
                echo "Python not found!"
                exit 1
            fi
            
            # Report Python version
            $TEST_PY --version
            
            # Run the test with detailed output
            cd ${workspaceTestDir}
            $TEST_PY -m pytest -v --junitxml=${env.WORKSPACE}/${resultPath}/shared-lib-pytest-results.xml ${testFile}
        """
        
        // Report success
        echo "Shared library tests completed successfully"
        return true
    } catch (Exception e) {
        echo "Shared library tests failed: ${e.message}"
        return false
    }
}

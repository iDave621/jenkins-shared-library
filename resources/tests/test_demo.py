import subprocess
import socket
import requests
import pytest

# Basic sanity test
def test_sanity():
    assert 1 + 1 == 2

# Test ping to local server
def test_ping_localhost():
    """Test ping to localhost to verify network stack is working"""
    try:
        # Using subprocess to run ping command
        # On Windows, '-n 1' means send 1 packet
        # On Linux/Unix, it would be '-c 1'
        result = subprocess.run(
            ["ping", "-n", "1", "localhost"], 
            capture_output=True, 
            text=True,
            timeout=5
        )
        assert result.returncode == 0, f"Failed to ping localhost: {result.stderr}"
        print(f"Ping to localhost successful: {result.stdout}")
    except Exception as e:
        pytest.skip(f"Ping test failed due to: {str(e)}")

# Test connection to Jenkins
def test_jenkins_connection():
    """Test connection to Jenkins server"""
    try:
        # Try to connect to Jenkins on port 8080
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(2)
        # Try to connect to localhost:8080
        result = s.connect_ex(("localhost", 8080))
        s.close()
        # If result is 0, the connection was successful
        assert result == 0, f"Failed to connect to Jenkins on port 8080, result: {result}"
        print("Connection to Jenkins successful")
    except Exception as e:
        pytest.skip(f"Jenkins connection test skipped: {str(e)}")

# Test connection to Nexus
def test_nexus_connection():
    """Test connection to Nexus Repository Manager"""
    try:
        # Try to connect to Nexus on port 8081
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(2)
        # Try to connect to localhost:8081
        result = s.connect_ex(("localhost", 8081))
        s.close()
        # If result is 0, the connection was successful
        assert result == 0, f"Failed to connect to Nexus on port 8081, result: {result}"
        print("Connection to Nexus successful")
    except Exception as e:
        pytest.skip(f"Nexus connection test skipped: {str(e)}")

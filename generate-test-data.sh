#!/bin/bash

# Script to run the TorrentTestDataGenerator utility
# This script generates test data for torrent testing

# Default output directory
DEFAULT_OUTPUT_DIR="/tmp/torrent_test_data"

# Display usage information
function show_usage {
    echo "Usage: $0 [output_directory]"
    echo ""
    echo "Arguments:"
    echo "  output_directory    Optional: Directory where test data will be created"
    echo "                      Default: $DEFAULT_OUTPUT_DIR"
    echo ""
    echo "Example:"
    echo "  $0 /path/to/test/data"
    echo ""
    echo "Note: This script requires mktorrent to be installed."
    echo "      On Fedora, install with: sudo dnf install mktorrent"
}

# Check if help is requested
if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    show_usage
    exit 0
fi

# Set output directory
OUTPUT_DIR=${1:-$DEFAULT_OUTPUT_DIR}

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed. Please install Maven to run this script."
    exit 1
fi

# Build the project if needed
echo "Building project..."
mvn compile test-compile

# Run the TorrentTestDataGenerator
echo "Generating test data in $OUTPUT_DIR..."
mvn exec:java -Dexec.classpathScope=test -Dexec.mainClass="dev.llaith.utils.TorrentTestDataGenerator" -Dexec.args="$OUTPUT_DIR"

# Check if the command was successful
if [ $? -eq 0 ]; then
    echo "Test data generation completed successfully."
    echo "Test data is available in: $OUTPUT_DIR"
else
    echo "Error: Test data generation failed."
    exit 1
fi
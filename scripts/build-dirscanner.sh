#!/bin/bash
#
# Build script for dirscanner framework with all available plugins
# Creates a multi-plugin native executable
#

set -e  # Exit on any error

echo "🔨 Building DirScanner Framework..."

# Clean and build all modules
echo "📦 Compiling all modules..."
mvn clean package -q

# Create fat JAR with all plugins
echo "🎯 Creating fat JAR with all plugins..."
cd torrentscanner
mvn assembly:single -q

echo "✅ Build completed successfully!"
echo "📍 Fat JAR location: torrentscanner/target/torrentscanner-1.0-SNAPSHOT-jar-with-dependencies.jar"
echo ""
echo "Usage examples:"
echo "  java -jar torrentscanner/target/torrentscanner-1.0-SNAPSHOT-jar-with-dependencies.jar --help"
echo "  java -jar torrentscanner/target/torrentscanner-1.0-SNAPSHOT-jar-with-dependencies.jar scan-torrents /path/to/torrents"
echo ""
echo "For native executable, use: scripts/build-native.sh"
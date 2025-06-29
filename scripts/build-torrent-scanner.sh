#!/bin/bash
#
# Build script for standalone torrent-scanner executable
# Creates a native executable with only the torrentscanner plugin
#

set -e  # Exit on any error

echo "🔨 Building Torrent Scanner..."

# Clean and build the project
echo "📦 Compiling modules..."
mvn clean package -q

# Build native executable with torrentscanner plugin
echo "🏗️  Creating native executable..."
cd torrentscanner

mvn -Pnative package -q

echo "✅ Build completed successfully!"
echo "📍 Executable location: torrentscanner/target/torrentscanner"
echo ""
echo "Usage examples:"
echo "  ./torrentscanner/target/torrentscanner scan-torrents /path/to/torrents"
echo "  ./torrentscanner/target/torrentscanner scan-torrents --help"
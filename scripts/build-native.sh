#!/bin/bash

#
# Copyright 2025 Nos Doughty.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Script to run the TorrentTestDataGenerator utility
# This script generates test data for torrent testing


# Script to build a native image of the torrent-scanner application
# This script automates the steps described in the README.md

set -e  # Exit immediately if a command exits with a non-zero status

# Create dist directory if it doesn't exist
mkdir -p dist

# Build the project with Maven
echo "Building the project with Maven..."
mvn clean package

# Generate reflection configuration for picocli
echo "Generating reflection configuration for picocli..."
mkdir -p src/main/resources/META-INF/native-image
java -cp target/torrent-scanner-1.0-SNAPSHOT-jar-with-dependencies.jar:target/classes \
  picocli.codegen.aot.graalvm.ReflectionConfigGenerator \
  dev.llaith.TorrentToCsvExporterCli \
  -o src/main/resources/META-INF/native-image/reflect-config.json

# Build the native image
echo "Building the native image..."
native-image \
  --no-fallback \
  -H:+UnlockExperimentalVMOptions \
  --gc=G1 \
  -march=native \
  -cp target/torrent-scanner-1.0-SNAPSHOT-jar-with-dependencies.jar \
  -H:ReflectionConfigurationFiles=src/main/resources/META-INF/native-image/reflect-config.json \
  -H:Name=dist/torrent-scanner \
  dev.llaith.TorrentToCsvExporterCli

echo "Native image build complete. The executable is located at: $(pwd)/dist/torrent-scanner"
echo "You can run it with: ./dist/torrent-scanner [options] <path-to-scan>"

# TorrentScanner

A Java utility for scanning directories for torrent files and exporting their information to CSV format.

## Description

TorrentScanner is a command-line tool that recursively scans a directory for `.torrent` files and exports their information to CSV files. For each torrent file, it extracts:

- Info hash
- File paths
- File sizes (where available)

By default, the extracted information is saved to CSV files in the same directory as the torrent files. Alternatively, you can specify an output directory where all CSV files will be created. Filenames are based on the original torrent filename and its info hash.

## Features

- Recursive directory scanning
- Detailed CSV export with info hash, file paths, and sizes
- Proper CSV escaping according to RFC 4180
- Comprehensive logging
- Error handling

## Requirements

- Java 24 or higher
- Maven 3.6 or higher (for building)

## Building

To build the project, run:

```bash
mvn clean package
```

This will create an executable JAR file in the `target` directory named `torrent-scanner-1.0-SNAPSHOT-jar-with-dependencies.jar`.

## Usage

```bash
java -jar target/torrent-scanner-1.0-SNAPSHOT-jar-with-dependencies.jar [options] <path-to-scan>
```

Replace `<path-to-scan>` with the path to the directory you want to scan for torrent files.

### Options

- `-v, --verbose`: Show debug logs
- `--quiet`: Do not show info logs
- `--dry-run`: Do not write any files, just log what would happen
- `--output-dir=<dir>`: Directory to write CSV files to (instead of next to torrent files)
- `--clobber`: Overwrite existing files instead of skipping them
- `--generate-index=<path>`: Generate an index file with all torrent information
- `--skip-scanning`: Skip scanning mode, only generate index if --generate-index is specified

### Example

```bash
java -jar target/torrent-scanner-1.0-SNAPSHOT-jar-with-dependencies.jar /path/to/torrents
```

## Output

By default, for each torrent file found, a CSV file will be created in the same directory as the torrent file. If you specify an output directory using the `--output-dir` option, all CSV files will be created in that directory instead.

The CSV files use the following naming convention:

```
<original-filename>.<info-hash>.csv
```

When using an output directory, if a file with the same name already exists (which can happen if two torrent files have the same name but are in different directories), the duplicate file will be skipped with a warning message.

The CSV file contains the following columns:

1. **InfoHash**: The SHA-1 hash of the torrent's info dictionary
2. **Path**: The path of the file within the torrent
3. **Size**: The size of the file in bytes

### Index File

When using the `--generate-index=<path>` option, an index file will be created at the specified path. This file contains information from all processed torrent files in a single CSV file.

The index file contains the following columns:

1. **InfoHash**: The SHA-1 hash of the torrent's info dictionary
2. **Path**: The path of the file within the torrent
3. **Size**: The size of the file in bytes
4. **TorrentFile**: The name of the original torrent file

The index file is recreated each time the application is run with the `--generate-index` option. If the file already exists, it will be overwritten if the `--clobber` flag is set, otherwise an error will be thrown.

### Skip Scanning Mode

When using the `--skip-scanning` option, the application will skip the normal scanning mode and will not generate individual CSV files for each torrent. This option is useful when you only want to generate the index file.

If `--skip-scanning` is used without `--generate-index`, the application will terminate with a "Nothing to do" message.

#### Examples

Generate an index file while also performing normal scanning:
```bash
java -jar target/torrent-scanner-1.0-SNAPSHOT-jar-with-dependencies.jar --generate-index=/path/to/index.csv /path/to/torrents
```

Generate only an index file without performing normal scanning:
```bash
java -jar target/torrent-scanner-1.0-SNAPSHOT-jar-with-dependencies.jar --skip-scanning --generate-index=/path/to/index.csv /path/to/torrents
```

## Logging

Logs are written to both the console and a log file in the `logs` directory. The log level for the application is set to DEBUG by default.

## Development

### Project Structure

```
torrent-scanner/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── dev/
│   │   │       └── llaith/
│   │   │           ├── TorrentToCsvExporterCli.java
│   │   │           └── TorrentToCsvExporter.java
│   │   └── resources/
│   │       └── logback.xml
│   └── test/
│       └── java/
│           └── dev/
│               └── llaith/
│                   ├── TorrentToCsvExporterCliTest.java
│                   ├── TorrentToCsvExporterJimfsTest.java
│                   └── TorrentToCsvExporterTest.java
├── pom.xml
└── README.md
```

### Running Tests

To run the tests, use:

```bash
mvn test
```

The project includes both unit tests and integration tests:

- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test the full functionality using a mock filesystem (Jimfs)

The integration tests use Google's Jimfs library to create an in-memory filesystem for testing. This allows testing the full functionality of the application without creating actual files on disk. The tests:

1. Set up a mock Unix-like filesystem
2. Copy test torrent files from resources to the mock filesystem
3. Run the TorrentToCsvExporter on the mock filesystem
4. Verify that the expected CSV files are created with the correct content

This approach ensures that the application works correctly with different filesystem implementations and provides comprehensive test coverage.

### Generating Test Data

The project includes a utility class `TorrentTestDataGenerator` for generating test data for torrent tests. This utility:

- Creates test files and directories for torrent testing
- Generates actual torrent files using the mktorrent command
- Generates dummy CSV files that would be expected output

#### Prerequisites

The test data generator requires the `mktorrent` package to be installed on your system. On Fedora, you can install it with:

```bash
sudo dnf install mktorrent
```

For other distributions, use the appropriate package manager.

#### Usage

You can use the `TorrentTestDataGenerator` class in your tests or run it directly:

```java
// Create a generator with a specific base directory
TorrentTestDataGenerator generator = new TorrentTestDataGenerator("/path/to/test/data");

// Create test files and directories
generator.createTestData();

// Create torrent files (requires mktorrent)
generator.createTorrentFiles();
```

#### Running with the Bash Script

The easiest way to generate test data is to use the provided bash script:

```bash
./generate-test-data.sh [output_directory]
```

This script will:
1. Build the project if needed
2. Run the TorrentTestDataGenerator with the specified output directory
3. Provide feedback on the success or failure of the operation

To see usage information:

```bash
./generate-test-data.sh --help
```

#### Running Manually

Alternatively, you can run the class directly with Maven:

```bash
mvn exec:java -Dexec.classpathScope=test -Dexec.mainClass="dev.llaith.utils.TorrentTestDataGenerator" -Dexec.args="[baseDir]"
```

Or with Java:

```bash
java -cp target/torrent-scanner-1.0-SNAPSHOT-jar-with-dependencies.jar dev.llaith.utils.TorrentTestDataGenerator [baseDir]
```

If no base directory is specified, it defaults to `/tmp/torrent_test_data`.

## License

This project is open source and available under the [Apache License 2.0](LICENSE).

## Native Image Building with GraalVM

You can build a native executable of this application using GraalVM. Native executables start faster and use less memory compared to running on the JVM.

### Prerequisites

- GraalVM 24 or higher with `native-image` tool installed
- Maven 3.6 or higher

### Building a Native Image

1. Install / Switch to GraalVM (if you're using SDKMAN):

```bash
sdk install java 24-graal
sdk use java 24-graal
```

2. Build the project with Maven:

```bash
mvn clean package
```

3. Generate reflection configuration for picocli:

```bash
mkdir -p src/main/resources/META-INF/native-image
java -cp target/torrent-scanner-1.0-SNAPSHOT-jar-with-dependencies.jar:target/classes \
  picocli.codegen.aot.graalvm.ReflectionConfigGenerator \
  dev.llaith.TorrentToCsvExporterCli \
  -o src/main/resources/META-INF/native-image/reflect-config.json
```

4. Build the native image:

```bash
native-image \
  --no-fallback \
  -H:+UnlockExperimentalVMOptions \
  -cp target/torrent-scanner-1.0-SNAPSHOT-jar-with-dependencies.jar \
  -H:ReflectionConfigurationFiles=src/main/resources/META-INF/native-image/reflect-config.json \
  -H:Name=torrent-scanner \
  dev.llaith.TorrentToCsvExporterCli
```

5. Run the native executable:

```bash
./torrent-scanner [options] <path-to-scan>
```

The native executable will be much faster to start and use less memory than the Java version. For example, to display the help information:

```bash
./torrent-scanner --help
```

For improved performance, you can add additional options to the native-image command:

```bash
native-image \
  --no-fallback \
  -H:+UnlockExperimentalVMOptions \
  --gc=G1 \
  -march=native \
  -cp target/torrent-scanner-1.0-SNAPSHOT-jar-with-dependencies.jar \
  -H:ReflectionConfigurationFiles=src/main/resources/META-INF/native-image/reflect-config.json \
  -H:Name=torrent-scanner \
  dev.llaith.TorrentToCsvExporterCli
```

### Using the Build Script

For convenience, a build script is provided to automate the native image building process:

```bash
./scripts/build-native.sh
```

This script will:
1. Build the project with Maven
2. Generate the reflection configuration
3. Build the native image with optimized settings
4. Place the executable in the `dist` folder

After running the script, you can find the executable at `dist/torrent-scanner`.

## Dependencies

- [bt-core](https://github.com/atomashpolskiy/bt) - A BitTorrent library for Java
- SLF4J and Logback for logging
- JUnit 5 for testing
- [Jimfs](https://github.com/google/jimfs) - An in-memory file system for testing

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

This will create an executable JAR file in the `target` directory named `TorrentScanner-1.0-SNAPSHOT-jar-with-dependencies.jar`.

## Usage

```bash
java -jar target/TorrentScanner-1.0-SNAPSHOT-jar-with-dependencies.jar [options] <path-to-scan>
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
java -jar target/TorrentScanner-1.0-SNAPSHOT-jar-with-dependencies.jar /path/to/torrents
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
java -jar target/TorrentScanner-1.0-SNAPSHOT-jar-with-dependencies.jar --generate-index=/path/to/index.csv /path/to/torrents
```

Generate only an index file without performing normal scanning:
```bash
java -jar target/TorrentScanner-1.0-SNAPSHOT-jar-with-dependencies.jar --skip-scanning --generate-index=/path/to/index.csv /path/to/torrents
```

## Logging

Logs are written to both the console and a log file in the `logs` directory. The log level for the application is set to DEBUG by default.

## Development

### Project Structure

```
TorrentScanner/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── dev/
│   │   │       └── llaith/
│   │   │           ├── Main.java
│   │   │           └── TorrentToCsvExporter.java
│   │   └── resources/
│   │       └── logback.xml
│   └── test/
│       └── java/
│           └── dev/
│               └── llaith/
│                   └── TorrentToCsvExporterTest.java
├── pom.xml
└── README.md
```

### Running Tests

To run the tests, use:

```bash
mvn test
```

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
java -cp target/TorrentScanner-1.0-SNAPSHOT-jar-with-dependencies.jar dev.llaith.utils.TorrentTestDataGenerator [baseDir]
```

If no base directory is specified, it defaults to `/tmp/torrent_test_data`.

## License

This project is open source and available under the [Apache License 2.0](LICENSE).

## Dependencies

- [jlibtorrent](https://github.com/aldenml/libtorrent4j) - A Java wrapper for libtorrent
- SLF4J and Logback for logging
- JUnit 5 for testing

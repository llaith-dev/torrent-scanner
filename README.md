# DirScanner Framework

A plugin-based directory scanning framework with multiple transport layers.

## Architecture Overview

This project has been transformed from a single-purpose torrent scanner into a **multi-dimensional plugin framework** with:

### 🔧 **Core Framework** (`dirscanner-core`)
- **Plugin System**: ServiceLoader-based plugin discovery with external JAR support
- **Transport Layers**: Separated CLI, Events (future), and custom transports  
- **Configuration**: YAML-based plugin configuration with framework services
- **Lifecycle Management**: Plugin initialization/cleanup with timeout handling

### 🔌 **Plugins**
- **`torrentscanner`**: Scans .torrent files and exports metadata to CSV format
- _Future plugins can be added easily_

### 📐 **Design Principles**
- **Horizontal separation**: Core Framework → Business Logic Plugins
- **Vertical separation**: Business Logic → Transport Layers (CLI, Events, etc.)
- **Configuration as framework service**
- **Native compilation compatibility**
- **Plugin command registration**
- **Timeout-based cleanup**

## Quick Start

### Build the Project
```bash
# Build everything
mvn clean package

# Or use convenience scripts  
./scripts/build-dirscanner.sh       # Fat JAR with all plugins
./scripts/build-torrent-scanner.sh  # Native executable (requires GraalVM)
```

### Run Torrent Scanner
```bash
# Using fat JAR
java -jar torrentscanner/target/torrentscanner-1.0-SNAPSHOT-jar-with-dependencies.jar \
  scan-torrents /path/to/torrents

# Using native executable (after build-torrent-scanner.sh)
./torrentscanner/target/torrentscanner scan-torrents /path/to/torrents

# With options
java -jar torrentscanner/target/torrentscanner-1.0-SNAPSHOT-jar-with-dependencies.jar \
  scan-torrents /path/to/torrents \
  --output-directory /path/to/output \
  --index-file /path/to/index.csv \
  --verbose --dry-run
```

### Available Commands
```bash
# List available plugins and commands
java -jar torrentscanner/target/torrentscanner-1.0-SNAPSHOT-jar-with-dependencies.jar

# Get help for specific command
java -jar torrentscanner/target/torrentscanner-1.0-SNAPSHOT-jar-with-dependencies.jar \
  scan-torrents --help
```

## Plugin Development

### Creating a New Plugin

1. **Create plugin module**:
   ```xml
   <!-- pom.xml -->
   <parent>
       <groupId>dev.llaith</groupId>
       <artifactId>dirscanner-parent</artifactId>
       <version>1.0-SNAPSHOT</version>
   </parent>
   
   <artifactId>myplugin</artifactId>
   
   <dependencies>
       <dependency>
           <groupId>dev.llaith</groupId>
           <artifactId>dirscanner-core</artifactId>
           <version>${project.version}</version>
       </dependency>
   </dependencies>
   ```

2. **Implement plugin interface**:
   ```java
   @DirScannerPluginInfo(
       name = "myplugin",
       description = "My custom plugin",
       version = "1.0.0"
   )
   public class MyPlugin implements DirScannerPlugin {
       
       @Override
       public void initialize(PluginContext context) throws PluginException {
           // Plugin initialization
       }
       
       @Override
       public List<Class<? extends Callable<Integer>>> getCommandClasses() {
           return List.of(MyCommand.class);
       }
       
       @Override
       public DirScannerHandler createHandler() throws PluginException {
           return new MyHandler();
       }
   }
   ```

3. **Create ServiceLoader registration**:
   ```
   # src/main/resources/META-INF/services/dev.llaith.dirscanner.plugin.DirScannerPlugin
   com.example.MyPlugin
   ```

4. **Add commands**:
   ```java
   @Command(name = "my-command", description = "My custom command")
   public class MyCommand implements Callable<Integer> {
       @Parameters(index = "0", description = "Directory to scan")
       private Path directory;
       
       @Override
       public Integer call() {
           // Command implementation
           return 0;
       }
   }
   ```

### Plugin Configuration

Plugins can use YAML configuration files with automatic discovery:

```yaml
# myplugin.yaml
batch-size: 100
timeout: 30s
output-format: json
custom-settings:
  feature-enabled: true
  max-threads: 4
```

Access via ConfigurationService:
```java
public class MyConfig {
    private int batchSize = 50;
    private String timeout = "10s";
    // getters/setters
}

@Override
public void initialize(PluginContext context) {
    Optional<MyConfig> config = context.getConfigurationService()
        .loadConfig("myplugin", MyConfig.class);
    
    if (config.isPresent()) {
        // Use configuration
    } else {
        // Use defaults or fail if required
    }
}
```

## Transport Layers

### CLI Transport (Current)
- **PicoCLI-based** with dynamic plugin command registration
- **Colored output** with verbosity levels (`--verbose`, `--quiet`)
- **Common options**: `--dry-run`, `--clobber`, `--no-color`
- **Self-discovering**: Lists available plugins and commands

### Future Transports
- **Events/Message Bus**: For integration with Apache Kafka, RabbitMQ, etc.
- **REST API**: HTTP-based service interface
- **gRPC**: High-performance RPC interface

## Configuration System

### Plugin Configuration Files
- **Default**: `<plugin-name>.yaml` in current directory
- **Override**: `--config=path/to/config.yaml` 
- **Fallback**: `ext/<plugin-name>.yaml`

### Framework Behavior
- Framework **never fails** on missing configuration
- Plugins decide whether to fail if configuration is required
- YAML format with Jackson parsing
- Full object mapping support

## External Plugins

Place plugin JAR files in the `ext/` directory for runtime discovery:

```
ext/
├── custom-plugin.jar        # External plugin
├── another-plugin.jar       # Another external plugin
├── torrentscanner.yaml      # Plugin configurations
├── custom-plugin.yaml
└── another-plugin.yaml
```

## Native Compilation

### ServiceLoader Plugins
✅ **Fully supported** - compiled into executable

### External JAR Plugins  
⚠️ **Limited** - requires build-time configuration for native builds

For maximum dynamic capability, use fat JAR distribution:
```bash
./scripts/build-dirscanner.sh
```

## Module Structure

```
dirscanner-parent/              # Parent POM with dependency management
├── dirscanner-core/            # Core framework
│   ├── src/main/java/dev/llaith/dirscanner/
│   │   ├── core/               # Core scanning logic (transport-agnostic)
│   │   ├── plugin/             # Plugin interfaces and management
│   │   └── transport/cli/      # CLI transport layer
│   └── src/main/resources/
│       └── logback.xml         # Shared logging configuration
├── torrentscanner/             # Torrent scanning plugin
│   ├── src/main/java/dev/llaith/torrentscanner/
│   │   ├── domain/             # Business logic (TorrentParser, etc.)
│   │   ├── commands/           # CLI commands (ScanTorrentsCommand)
│   │   ├── TorrentScanner.java # Core scanner
│   │   └── TorrentScannerPlugin.java # Plugin implementation  
│   └── src/main/resources/META-INF/services/
│       └── dev.llaith.dirscanner.plugin.DirScannerPlugin
├── ext/                        # External plugin JARs and configs
├── scripts/                    # Build scripts
│   ├── build-dirscanner.sh     # Multi-plugin fat JAR
│   └── build-torrent-scanner.sh # Native executable
└── README.md                   # This file
```

## Torrent Scanner Features

The `torrentscanner` plugin provides:

### Core Functionality
- **Recursive scanning** of directories for `.torrent` files
- **Metadata extraction**: Info hash, file paths, sizes
- **CSV export** with proper RFC 4180 escaping
- **Individual files** or **consolidated index** output
- **Batch processing** for large collections

### Command Options
```bash
scan-torrents [options] <directory>

Options:
  --output-directory DIR     Write CSV files to specific directory
  --index-file FILE         Create consolidated index file
  -d, --dry-run            Show what would be done without writing
  -c, --clobber            Overwrite existing files
  -v, --verbose            Detailed output
  -q, --quiet              Minimal output
  --no-color               Disable colored output
  -h, --help               Show help
```

### Output Formats

**Individual CSV files** (default):
```
<torrent-name>.<info-hash>.csv
```

**Index file** (with `--index-file`):
- Consolidates all torrents into single CSV
- Includes source torrent filename
- Batch processing for memory efficiency

## Development

### Building and Testing
```bash
# Build all modules
mvn clean package

# Run tests
mvn test

# Build with native profile (requires GraalVM)
mvn clean package -Pnative
```

### Adding New Commands to Existing Plugin

```java
// In your plugin's getCommandClasses()
@Override
public List<Class<? extends Callable<Integer>>> getCommandClasses() {
    return List.of(
        ScanTorrentsCommand.class,
        NewCommand.class,          // Add new command
        AnotherCommand.class       // Add another command  
    );
}
```

Commands are automatically registered and available via CLI.

### Plugin Lifecycle

```java
@Override
public void initialize(PluginContext context) throws PluginException {
    // Called once during plugin discovery
    // Setup resources, validate configuration
}

@Override  
public void shutdown(Duration timeout) throws PluginException {
    // Called during framework shutdown
    // Cleanup resources (databases, files, etc.)
    // Timeout enforced for hanging resources
}
```

## Migration from v1.0

The original torrent-scanner functionality is **fully preserved** as the `torrentscanner` plugin. 

### Old Usage
```bash
java -jar torrent-scanner.jar [options] <directory>
```

### New Usage  
```bash
java -jar torrentscanner.jar scan-torrents [options] <directory>
```

All original options and functionality remain unchanged.

## Requirements

- **Java 21** or higher
- **Maven 3.6+** for building
- **GraalVM 24+** for native compilation (optional)

## Dependencies

### Core Framework
- **SLF4J/Logback**: Logging
- **PicoCLI**: CLI framework  
- **Jackson**: YAML configuration
- **JUnit 5**: Testing

### Torrent Plugin
- **bt-core**: BitTorrent library
- **Jimfs**: In-memory filesystem for testing

## License

Apache License 2.0 - see [LICENSE](LICENSE) file for details.

## Future Development

### Planned Plugins
- **PDF Scanner**: Extract metadata from PDF files
- **Image Scanner**: Process image files and extract EXIF data
- **Archive Scanner**: Handle ZIP, TAR, etc. files

### Transport Layer Extensions
- **Message Bus Integration**: Apache Kafka, RabbitMQ support
- **REST API**: HTTP service interface
- **gRPC**: High-performance RPC
- **Batch Processing**: Large-scale file processing

### Framework Enhancements
- **Plugin marketplace**: Discover and install plugins
- **Configuration validation**: Schema-based validation
- **Metrics collection**: Plugin performance monitoring
- **Event system**: Inter-plugin communication
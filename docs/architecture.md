# DirScanner Framework Architecture

## Executive Summary

The DirScanner framework represents a sophisticated transformation from a single-purpose torrent scanner into a **multi-dimensional plugin-based architecture** that demonstrates enterprise-grade design patterns and principles. The architecture implements a **two-axis separation strategy** that provides both horizontal scalability (core framework → business logic plugins) and vertical scalability (business logic → transport layers), enabling true **channel-agnostic** design.

This architectural approach ensures that the same business logic can be accessed through multiple channels (CLI, REST API, gRPC, event-driven systems) without code duplication or tight coupling, making it suitable for enterprise environments where consistent functionality across multiple interfaces is critical.

## Architectural Perspectives

### 1. Logical View - Plugin-Based Separation

The logical architecture demonstrates clear **bounded contexts** and **separation of concerns**:

```mermaid
graph TB
    subgraph "Transport Layer"
        CLI[CLI Transport]
        REST[REST Transport<br/>Future]
        GRPC[gRPC Transport<br/>Future]
        Events[Event Transport<br/>Future]
    end
    
    subgraph "Core Framework"
        DS[DirScanner Engine]
        PM[PluginManager]
        CS[ConfigurationService]
        PR[ProgressReporter Interface]
    end
    
    subgraph "Plugin Ecosystem"
        TS[TorrentScanner Plugin]
        IS[ImageScanner Plugin<br/>Future]
        PS[PDFScanner Plugin<br/>Future]
    end
    
    subgraph "Domain Logic"
        TH[TorrentHandler]
        TP[TorrentParser]
        CW[CsvWriter]
    end
    
    CLI --> DS
    REST -.-> DS
    GRPC -.-> DS
    Events -.-> DS
    
    DS --> PM
    DS --> CS
    DS --> PR
    
    PM --> TS
    PM -.-> IS
    PM -.-> PS
    
    TS --> TH
    TH --> TP
    TH --> CW
```

**Key Principles:**
- **Plugin Isolation**: Each plugin is a separate bounded context
- **Framework Core**: Provides common services without business logic coupling
- **Transport Independence**: Business logic accessible from any transport layer

### 2. Development View - Module Structure

The development view shows the Maven modular structure that supports the logical separation:

```mermaid
graph TD
    subgraph "Build Structure"
        Parent[dirscanner-parent<br/>Maven Parent POM]
        Core[dirscanner-core<br/>Framework Module]
        Torrent[torrentscanner<br/>Plugin Module]
        External[ext/<br/>External Plugins]
    end
    
    subgraph "Core Dependencies"
        PicoCLI[PicoCLI<br/>Command Framework]
        Jackson[Jackson<br/>YAML Configuration]
        SLF4J[SLF4J/Logback<br/>Logging]
    end
    
    subgraph "Plugin Dependencies"
        BTCore[bt-core<br/>BitTorrent Library]
        Custom[Custom Libraries<br/>Per Plugin]
    end
    
    Parent --> Core
    Parent --> Torrent
    Parent -.-> External
    
    Core --> PicoCLI
    Core --> Jackson
    Core --> SLF4J
    
    Torrent --> BTCore
    Torrent -.-> Custom
    
    External -.-> Custom
```

**Benefits:**
- **Dependency Isolation**: Plugins don't pollute core framework with domain-specific libraries
- **Independent Deployment**: Plugins can be developed and deployed separately
- **Version Management**: Centralized dependency management with plugin autonomy

### 3. Process View - Plugin Lifecycle and Scanning Operations

The process view illustrates how the framework orchestrates plugin discovery, initialization, and execution:

```mermaid
sequenceDiagram
    participant CLI as CLI Transport
    participant PM as PluginManager
    participant Core as DirScanner Core
    participant Plugin as Plugin Instance
    participant Handler as Domain Handler
    
    Note over CLI,Handler: Plugin Discovery Phase
    CLI->>PM: Initialize Framework
    PM->>PM: ServiceLoader Discovery
    PM->>PM: External JAR Discovery
    PM->>Plugin: Create Instance
    PM->>Plugin: initialize(context)
    Plugin->>Plugin: Load Configuration
    PM->>CLI: Register Commands
    
    Note over CLI,Handler: Execution Phase
    CLI->>Core: scan(request)
    Core->>Plugin: createHandler()
    Plugin->>Handler: new Handler()
    Core->>Handler: Process Files
    Handler->>Handler: Business Logic
    Handler->>CLI: Progress Updates
    
    Note over CLI,Handler: Cleanup Phase
    CLI->>PM: Shutdown
    PM->>Plugin: shutdown(timeout)
    Plugin->>Plugin: Resource Cleanup
```

**Key Characteristics:**
- **Lifecycle Management**: Proper initialization and cleanup
- **Error Isolation**: Plugin failures don't crash the framework
- **Resource Management**: Timeout-based cleanup prevents hanging

### 4. Physical View - Deployment Models

The physical architecture supports multiple deployment scenarios:

#### Build-Time Integration
```mermaid
graph LR
    subgraph "Fat JAR Distribution"
        CoreJAR[dirscanner-core.jar]
        PluginJAR[torrentscanner.jar]
        CombinedJAR[combined-executable.jar]
    end
    
    subgraph "Native Compilation"
        CoreNative[Core Classes]
        PluginNative[Plugin Classes]
        NativeExe[Native Executable]
    end
    
    CoreJAR --> CombinedJAR
    PluginJAR --> CombinedJAR
    
    CoreNative --> NativeExe
    PluginNative --> NativeExe
```

#### Runtime Plugin Loading
```mermaid
graph LR
    subgraph "Runtime Environment"
        Base[Base Application]
        ExtDir[ext/ Directory]
        Config[Configuration Files]
    end
    
    subgraph "External Plugins"
        CustomJAR[custom-plugin.jar]
        PluginConfig[plugin-config.yaml]
    end
    
    Base --> ExtDir
    ExtDir --> CustomJAR
    ExtDir --> PluginConfig
    Config --> PluginConfig
```

**Deployment Flexibility:**
- **Fat JAR**: Maximum compatibility, single file distribution
- **Native Binary**: Fast startup, reduced memory footprint
- **Plugin Directory**: Dynamic loading, no restart required

### 5. Channel Agnostic Design - Transport Independence

The architecture's most significant achievement is **channel agnostic design**, where the same business logic can serve multiple client interfaces without modification:

```mermaid
graph TD
    subgraph "Client Channels"
        CLIClient[CLI User]
        WebClient[Web Interface]
        APIClient[REST API Client]
        ServiceClient[Microservice]
        EventClient[Event Consumer]
    end
    
    subgraph "Transport Adapters"
        CLIAdapter[CLI Transport<br/>PicoCLI Commands]
        HTTPAdapter[HTTP Transport<br/>Spring Boot]
        gRPCAdapter[gRPC Transport<br/>Protocol Buffers]
        EventAdapter[Event Transport<br/>Kafka/RabbitMQ]
    end
    
    subgraph "Business Logic Core"
        Scanner[DirScanner Engine]
        Plugins[Plugin Ecosystem]
        Handlers[Domain Handlers]
    end
    
    CLIClient --> CLIAdapter
    WebClient --> HTTPAdapter
    APIClient --> HTTPAdapter
    ServiceClient --> gRPCAdapter
    EventClient --> EventAdapter
    
    CLIAdapter --> Scanner
    HTTPAdapter -.-> Scanner
    gRPCAdapter -.-> Scanner
    EventAdapter -.-> Scanner
    
    Scanner --> Plugins
    Plugins --> Handlers
```

**Channel Independence Benefits:**
- **Consistent Behavior**: Same business logic across all channels
- **Reduced Duplication**: Single implementation serves multiple interfaces
- **Testing Simplification**: Test business logic once, transport adapters separately
- **Enterprise Integration**: Easy addition of new channels without core changes

## Two-Axis Separation Strategy

The framework implements a sophisticated **two-axis separation** that provides multiple dimensions of scalability and maintainability:

### Horizontal Axis: Core → Plugins
```mermaid
graph LR
    subgraph "Horizontal Separation"
        Core[Core Framework<br/>Generic Scanning]
        Arrow1[→]
        Plugin1[TorrentScanner<br/>Torrent Logic]
        Arrow2[→]
        Plugin2[ImageScanner<br/>Image Logic]
        Arrow3[→]
        Plugin3[PDFScanner<br/>PDF Logic]
    end
    
    Core --> Arrow1
    Arrow1 --> Plugin1
    Plugin1 --> Arrow2
    Arrow2 --> Plugin2
    Plugin2 --> Arrow3
    Arrow3 --> Plugin3
```

### Vertical Axis: Business Logic → Transport
```mermaid
graph TD
    subgraph "Vertical Separation"
        BL[Business Logic<br/>Domain Processing]
        Arrow1[↓]
        CLI[CLI Transport<br/>Command Line]
        Arrow2[↓]
        REST[REST Transport<br/>HTTP API]
        Arrow3[↓]
        Events[Event Transport<br/>Message Queue]
    end
    
    BL --> Arrow1
    Arrow1 --> CLI
    CLI --> Arrow2
    Arrow2 --> REST
    REST --> Arrow3
    Arrow3 --> Events
```

### Combined Multi-Dimensional Matrix
```mermaid
graph TD
    subgraph "Transport Dimensions"
        subgraph "CLI Layer"
            CLI_T[CLI Torrent]
            CLI_I[CLI Image]
            CLI_P[CLI PDF]
        end
        
        subgraph "REST Layer"
            REST_T[REST Torrent]
            REST_I[REST Image]
            REST_P[REST PDF]
        end
        
        subgraph "Event Layer"
            EVENT_T[Event Torrent]
            EVENT_I[Event Image]
            EVENT_P[Event PDF]
        end
    end
    
    subgraph "Business Logic"
        TorrentLogic[Torrent Processing]
        ImageLogic[Image Processing]
        PDFLogic[PDF Processing]
    end
    
    CLI_T --> TorrentLogic
    CLI_I --> ImageLogic
    CLI_P --> PDFLogic
    
    REST_T -.-> TorrentLogic
    REST_I -.-> ImageLogic
    REST_P -.-> PDFLogic
    
    EVENT_T -.-> TorrentLogic
    EVENT_I -.-> ImageLogic
    EVENT_P -.-> PDFLogic
```

## Enterprise Architecture Readiness

### Hexagonal Architecture Integration

The framework is designed to integrate seamlessly into hexagonal (ports and adapters) architectures:

```mermaid
graph TB
    subgraph "External World"
        CLI[CLI Interface]
        REST[REST API]
        gRPC[gRPC Service]
        Events[Event Bus]
        Files[File System]
        Config[Configuration]
    end
    
    subgraph "Ports (Interfaces)"
        CommandPort[Command Port]
        ScanPort[Scan Port]
        ConfigPort[Config Port]
        FilePort[File Port]
    end
    
    subgraph "Core Business Logic"
        Scanner[DirScanner Engine]
        Plugins[Plugin System]
        Domain[Domain Logic]
    end
    
    subgraph "Adapters"
        CLIAdapter[CLI Adapter]
        HTTPAdapter[HTTP Adapter]
        gRPCAdapter[gRPC Adapter]
        EventAdapter[Event Adapter]
        FileAdapter[File Adapter]
        ConfigAdapter[Config Adapter]
    end
    
    CLI --> CLIAdapter
    REST --> HTTPAdapter
    gRPC --> gRPCAdapter
    Events --> EventAdapter
    Files --> FileAdapter
    Config --> ConfigAdapter
    
    CLIAdapter --> CommandPort
    HTTPAdapter --> ScanPort
    gRPCAdapter --> ScanPort
    EventAdapter --> ScanPort
    FileAdapter --> FilePort
    ConfigAdapter --> ConfigPort
    
    CommandPort --> Scanner
    ScanPort --> Scanner
    ConfigPort --> Scanner
    FilePort --> Scanner
    
    Scanner --> Plugins
    Plugins --> Domain
```

### Microservices Architecture Support

The plugin system naturally supports microservices decomposition:

```mermaid
graph TD
    subgraph "Microservices Ecosystem"
        subgraph "Torrent Service"
            TorrentAPI[Torrent REST API]
            TorrentCore[Torrent Business Logic]
            TorrentCLI[Torrent CLI]
        end
        
        subgraph "Image Service"
            ImageAPI[Image REST API]
            ImageCore[Image Business Logic]
            ImageCLI[Image CLI]
        end
        
        subgraph "Orchestration Service"
            Gateway[API Gateway]
            Registry[Service Registry]
            Config[Config Service]
        end
    end
    
    subgraph "Shared Components"
        Framework[DirScanner Framework]
        CommonLib[Common Libraries]
    end
    
    TorrentAPI --> TorrentCore
    TorrentCLI --> TorrentCore
    TorrentCore --> Framework
    
    ImageAPI --> ImageCore
    ImageCLI --> ImageCore
    ImageCore --> Framework
    
    Gateway --> TorrentAPI
    Gateway --> ImageAPI
    
    Framework --> CommonLib
```

## Design Decision Rationale

### 1. ServiceLoader vs. Custom Plugin Discovery
**Decision**: Use Java ServiceLoader with extension for external JARs  
**Rationale**: 
- Standard Java mechanism for plugin discovery
- Native compilation compatibility
- Extensible for runtime loading scenarios

### 2. Transport Layer Separation
**Decision**: Complete isolation of transport concerns from business logic  
**Rationale**:
- Enables channel agnostic design
- Supports enterprise integration patterns
- Facilitates testing and maintenance

### 3. Plugin Lifecycle Management
**Decision**: Explicit initialization and shutdown with timeout handling  
**Rationale**:
- Prevents resource leaks in long-running applications
- Supports graceful degradation
- Enterprise-grade reliability requirements

### 4. Configuration as Framework Service
**Decision**: YAML-based configuration with automatic object mapping  
**Rationale**:
- Consistent configuration approach across plugins
- Type-safe configuration objects
- Environment-specific deployment support

### 5. Progress Reporting Abstraction
**Decision**: Interface-based progress reporting with transport-specific implementations  
**Rationale**:
- Decouples business logic from UI concerns
- Enables programmatic monitoring
- Supports different user interface paradigms

## Future Evolution Paths

### 1. Additional Transport Layers
- **GraphQL API**: Query-based data access
- **WebSocket**: Real-time streaming updates
- **Message Queues**: Asynchronous processing
- **gRPC Streaming**: High-performance bidirectional communication

### 2. Enterprise Features
- **Security Layer**: Authentication and authorization
- **Monitoring**: Metrics collection and observability
- **Distributed Processing**: Multi-node scanning capabilities
- **Caching**: Result caching for improved performance

### 3. Plugin Ecosystem
- **Plugin Marketplace**: Discovery and installation of community plugins
- **Plugin Versioning**: Compatibility management
- **Hot Reloading**: Dynamic plugin updates without restart
- **Plugin Dependencies**: Inter-plugin communication and dependencies

## Conclusion

The DirScanner framework architecture demonstrates a sophisticated understanding of enterprise software design principles. The two-axis separation strategy, combined with channel agnostic design, creates a foundation that can evolve to support complex enterprise requirements while maintaining clean separation of concerns and high maintainability.

The architecture's strength lies in its ability to provide **consistent business logic access** across multiple channels while maintaining **plugin-based extensibility** and **transport independence**. This makes it an excellent foundation for enterprise applications that require multiple user interfaces and integration points.
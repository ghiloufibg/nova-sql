# Nova SQL Database Engine

A modern, lightweight SQL database engine built with Java 21, featuring ACID transactions, B-tree indexing, and comprehensive SQL support.

## Features

### Core Database Engine
- **ACID Transactions**: Full transaction support with commit/rollback
- **B-Tree Indexing**: Automatic primary key indexing with support for secondary indexes
- **SQL Parser**: Support for CREATE TABLE, INSERT, UPDATE, DELETE, SELECT statements
- **Query Execution**: Optimized query execution with proper locking
- **Buffer Pool**: Memory management with configurable buffer sizes
- **Persistent Storage**: Page-based disk storage with crash recovery

### Advanced Features
- **Multiple Data Types**: INTEGER, VARCHAR, BOOLEAN, DATE, DECIMAL
- **JOIN Operations**: INNER, LEFT, RIGHT, FULL JOINs
- **Aggregation Functions**: COUNT, SUM, AVG, MIN, MAX with GROUP BY
- **Secondary Indexes**: CREATE INDEX support for performance optimization
- **UPDATE/DELETE**: Full CRUD operations with WHERE clause support

### Utilities & Tools
- **Interactive CLI Shell**: Command-line interface for database interaction
- **CSV Import/Export**: Easy data migration with CSV files
- **Database Backup/Export**: Export entire database to SQL files
- **Query Performance Statistics**: Track execution time and performance metrics
- **Configuration Management**: Configurable buffer sizes, directories, and options

## Quick Start

### 1. Build the Project
```bash
mvn clean compile
```

### 2. Run Tests
```bash
mvn test
```

### 3. Start the Interactive Shell
```bash
java -cp target/classes com.novasql.cli.NovaShell [database_name] [data_directory]
```

### 4. Example Usage
```sql
-- Create a table
CREATE TABLE users (id INTEGER PRIMARY KEY, name VARCHAR(50), email VARCHAR(100));

-- Insert data
INSERT INTO users (id, name, email) VALUES (1, 'John Doe', 'john@example.com');
INSERT INTO users (id, name, email) VALUES (2, 'Jane Smith', 'jane@example.com');

-- Query data
SELECT * FROM users;
SELECT name FROM users WHERE id = 1;

-- Update data
UPDATE users SET email = 'john.doe@example.com' WHERE id = 1;

-- Create secondary index
CREATE INDEX idx_email ON users(email);

-- Delete data
DELETE FROM users WHERE id = 2;
```

## Shell Commands

In the interactive shell, you can use these commands:

- `help` - Show available commands
- `status` - Show database status
- `tables` - List all tables
- `desc <table>` - Describe table structure
- `exit` or `quit` - Exit the shell

## API Usage

```java
// Initialize the engine
DatabaseEngine engine = new DatabaseEngine();
engine.start("my_database", "./data");

// Execute SQL
QueryResult result = engine.executeSQL("SELECT * FROM users");

// Work with results
if (result.hasRecords()) {
    for (Record record : result.getRecords()) {
        System.out.println(record.getValue("name"));
    }
}

// Export/Import utilities
engine.exportCSV("users", "users.csv");
engine.importCSV("users.csv", "users");
engine.exportDatabase("backup.sql");

// Get performance statistics
List<QueryStats> stats = engine.getQueryHistory();

// Cleanup
engine.stop();
```

## Configuration

Create a `nova.properties` file in your classpath:

```properties
# Buffer pool size (number of pages)
buffer.pool.size=1000

# Maximum connections
max.connections=100

# Log level
log.level=INFO

# Data directory
data.directory=./data

# Page size in bytes
page.size=4096

# Enable write-ahead logging
enable.wal=true

# Auto-create indexes
auto.create.indexes=true
```

## Architecture

### Storage Layer
- **Page-based storage**: 4KB pages with header information
- **Buffer pool**: LRU cache for frequently accessed pages
- **Disk manager**: Handles persistent storage and file management

### Transaction Management
- **ACID compliance**: Atomicity, Consistency, Isolation, Durability
- **Lock manager**: Read/write locks with deadlock prevention
- **Transaction coordinator**: Manages transaction lifecycle

### Query Processing
- **SQL parser**: Regex-based parser with support for complex queries
- **Query executor**: Optimized execution with index usage
- **Result processing**: Efficient result set handling

### Indexing
- **B-tree implementation**: Self-balancing tree structures
- **Primary key indexes**: Automatic indexing for primary keys
- **Secondary indexes**: Manual index creation for performance

## Performance

- **Fast queries**: Index-based lookups and optimized scans
- **Concurrent access**: Multiple readers, exclusive writers
- **Memory efficient**: Configurable buffer pool sizes
- **Disk efficient**: Page-based storage with compression

## Limitations

- Single-node deployment (no clustering)
- Basic SQL feature set (no advanced functions)
- File-based storage (no network protocols)
- Limited concurrent connections

## Contributing

This is a demonstration project showcasing database internals and SQL implementation.

## License

Educational/Demonstration purposes.
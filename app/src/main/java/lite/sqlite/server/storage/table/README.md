# Table Layer Implementation

This document describes the implementation of the Table layer in the Lite-SQLite database system. The Table layer provides a higher-level abstraction over the storage layer, allowing for the management of records within tables.

## Key Classes

### `RecordId`

The `RecordId` class identifies a specific record within a table. It consists of:

- A `Block` that specifies the physical location of the page containing the record
- A slot number that specifies the record's position within the page

This class provides a way to uniquely identify and retrieve records in the database.

### `Table`

The `Table` class represents a table in the database and provides methods to:

- Insert records into the table
- Retrieve records using their RecordId
- Update existing records
- Delete records
- Iterate through all records in the table

The Table class uses the BufferPool to manage pages and RecordPage to handle record storage within pages.

### `TableInfo`

The `TableInfo` class stores metadata about a table, including:

- Table name
- Schema information
- The file where the table data is stored
- Creation and modification timestamps
- Record count

## Usage

To use the Table layer, you need to:

1. Create a Schema defining the columns of your table
2. Create a TableInfo object with the schema and table name
3. Create a Table object using the TableInfo and BufferPool
4. Use the Table methods to insert, retrieve, update, and delete records

## Example

Here's a simple example of how to use the Table layer:

```java
// Set up the database components
File dbDir = new File("my_database");
FileManager fileManager = new BasicFileManager(dbDir);
BufferPool bufferPool = new BufferPool(10, fileManager);

// Create a schema for the users table
Schema schema = new Schema();
schema.addColumn("id", DataType.INTEGER);
schema.addColumn("name", DataType.VARCHAR, 50);
schema.addColumn("age", DataType.INTEGER);

// Create a table info object
TableInfo tableInfo = new TableInfo("users", schema, "users.tbl");

// Create the table
Table table = new Table(tableInfo, bufferPool);

// Insert a record
Record record = new Record(new Object[] { 1, "Alice", 25 });
RecordId rid = table.insertRecord(record);

// Retrieve the record
Record retrievedRecord = table.getRecord(rid);

// Update the record
Record updatedRecord = new Record(new Object[] { 1, "Alice Smith", 26 });
table.updateRecord(rid, updatedRecord);

// Delete the record
table.deleteRecord(rid);

// Iterate through all records
for (Record r : table) {
    System.out.println(r);
}
```

## Testing

The `TableTest` and `TableDemo` classes provide examples and tests for the Table layer functionality.

## Future Improvements

1. Add support for multiple blocks per table
2. Implement indexing for faster record retrieval
3. Add transaction support for ACID compliance
4. Implement constraints (primary key, foreign key, unique, etc.)
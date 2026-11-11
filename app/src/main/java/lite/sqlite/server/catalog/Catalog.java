package lite.sqlite.server.catalog;

import java.util.List;

import lite.sqlite.server.storage.record.DataType;

/**
 * Everything the engine must remember about its tables across a restart.
 * Row data and index contents are not here: rows live in the .tbl files and
 * indexes are rebuilt from them on open.
 */
public record Catalog(List<TableDef> tables) {

    public record TableDef(String name, List<ColumnDef> columns, List<IndexDef> indexes) {}

    public record ColumnDef(String name, DataType type, int maxLength) {}

    public record IndexDef(String name, String columnName, boolean unique, int maxDegree) {}
}

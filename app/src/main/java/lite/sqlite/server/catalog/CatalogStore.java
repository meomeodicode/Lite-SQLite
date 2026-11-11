package lite.sqlite.server.catalog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32C;

import lite.sqlite.server.catalog.Catalog.ColumnDef;
import lite.sqlite.server.catalog.Catalog.IndexDef;
import lite.sqlite.server.catalog.Catalog.TableDef;
import lite.sqlite.server.storage.record.DataType;

/**
 * Persists the catalog as catalog.dat.
 *
 * File layout: magic(int) | version(short) | payloadLength(int) | payload | crc32c(int),
 * where the checksum covers every byte before it.
 *
 * Save writes catalog.tmp, forces it to disk, then renames it over catalog.dat, so a
 * crash leaves either the complete old file or the complete new one.
 */
public class CatalogStore {

    static final int MAGIC = 0x4C534354; // "LSCT"
    static final short VERSION = 1;
    private static final int HEADER_BYTES = 4 + 2 + 4;
    private static final int CRC_BYTES = 4;

    private static final byte TYPE_INTEGER = 1;
    private static final byte TYPE_VARCHAR = 2;

    private final Path file;
    private final Path tmp;
    private final Runnable beforeRename;

    public CatalogStore(Path dir) {
        this(dir, () -> {});
    }

    /** beforeRename runs after the temp file is durable and before the switch; tests use it to simulate a crash. */
    CatalogStore(Path dir, Runnable beforeRename) {
        this.file = dir.resolve("catalog.dat");
        this.tmp = dir.resolve("catalog.tmp");
        this.beforeRename = beforeRename;
    }

    public Catalog load() throws IOException {
        if (Files.notExists(file)) {
            return new Catalog(List.of());
        }
        return decode(Files.readAllBytes(file));
    }

    public void save(Catalog catalog) throws IOException {
        byte[] bytes = encode(catalog);

        try (FileChannel channel = FileChannel.open(tmp,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }

        beforeRename.run();

        try {
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static byte[] encode(Catalog catalog) throws IOException {
        ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(payloadBytes)) {
            out.writeInt(catalog.tables().size());
            for (TableDef table : catalog.tables()) {
                out.writeUTF(table.name());
                out.writeInt(table.columns().size());
                for (ColumnDef column : table.columns()) {
                    out.writeUTF(column.name());
                    out.writeByte(typeCode(column.type()));
                    out.writeInt(column.maxLength());
                }
                out.writeInt(table.indexes().size());
                for (IndexDef index : table.indexes()) {
                    out.writeUTF(index.name());
                    out.writeUTF(index.columnName());
                    out.writeBoolean(index.unique());
                    out.writeInt(index.maxDegree());
                }
            }
        }
        byte[] payload = payloadBytes.toByteArray();

        ByteArrayOutputStream whole = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(whole)) {
            out.writeInt(MAGIC);
            out.writeShort(VERSION);
            out.writeInt(payload.length);
            out.write(payload);
        }
        byte[] withoutCrc = whole.toByteArray();

        ByteBuffer result = ByteBuffer.allocate(withoutCrc.length + CRC_BYTES);
        result.put(withoutCrc);
        result.putInt(checksum(withoutCrc, withoutCrc.length));
        return result.array();
    }

    static Catalog decode(byte[] bytes) throws IOException {
        if (bytes.length < HEADER_BYTES + CRC_BYTES) {
            throw new CatalogCorruptException("catalog too short: " + bytes.length + " bytes");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        if (buffer.getInt() != MAGIC) {
            throw new CatalogCorruptException("bad magic");
        }
        short version = buffer.getShort();
        if (version != VERSION) {
            throw new CatalogCorruptException("unsupported catalog version " + version);
        }
        int payloadLength = buffer.getInt();
        if (payloadLength < 0 || bytes.length != HEADER_BYTES + payloadLength + CRC_BYTES) {
            throw new CatalogCorruptException("length mismatch: header says " + payloadLength
                    + " payload bytes, file has " + bytes.length + " bytes in total");
        }
        int crcOffset = HEADER_BYTES + payloadLength;
        int stored = ByteBuffer.wrap(bytes, crcOffset, CRC_BYTES).getInt();
        if (stored != checksum(bytes, crcOffset)) {
            throw new CatalogCorruptException("checksum mismatch");
        }

        try (DataInputStream in = new DataInputStream(
                new ByteArrayInputStream(bytes, HEADER_BYTES, payloadLength))) {
            int tableCount = in.readInt();
            List<TableDef> tables = new ArrayList<>();
            for (int t = 0; t < tableCount; t++) {
                String tableName = in.readUTF();
                int columnCount = in.readInt();
                List<ColumnDef> columns = new ArrayList<>();
                for (int c = 0; c < columnCount; c++) {
                    columns.add(new ColumnDef(in.readUTF(), dataType(in.readByte()), in.readInt()));
                }
                int indexCount = in.readInt();
                List<IndexDef> indexes = new ArrayList<>();
                for (int i = 0; i < indexCount; i++) {
                    indexes.add(new IndexDef(in.readUTF(), in.readUTF(), in.readBoolean(), in.readInt()));
                }
                tables.add(new TableDef(tableName, List.copyOf(columns), List.copyOf(indexes)));
            }
            return new Catalog(List.copyOf(tables));
        } catch (EOFException e) {
            throw new CatalogCorruptException("payload ended early");
        }
    }

    private static int checksum(byte[] bytes, int length) {
        CRC32C crc = new CRC32C();
        crc.update(bytes, 0, length);
        return (int) crc.getValue();
    }

    private static byte typeCode(DataType type) {
        return switch (type) {
            case INTEGER -> TYPE_INTEGER;
            case VARCHAR -> TYPE_VARCHAR;
        };
    }

    private static DataType dataType(byte code) throws CatalogCorruptException {
        return switch (code) {
            case TYPE_INTEGER -> DataType.INTEGER;
            case TYPE_VARCHAR -> DataType.VARCHAR;
            default -> throw new CatalogCorruptException("unknown column type code " + code);
        };
    }
}

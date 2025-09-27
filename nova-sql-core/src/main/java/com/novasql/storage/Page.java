package com.novasql.storage;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a database page in the Nova SQL storage system.
 *
 * <p>A page is the fundamental unit of storage in the Nova SQL database engine.
 * Each page has a fixed size (4KB by default) and contains:</p>
 * <ul>
 *   <li>Header information (page ID, record count, free space)</li>
 *   <li>Variable number of records stored sequentially</li>
 *   <li>Free space tracking for efficient storage management</li>
 * </ul>
 *
 * <p>Pages support basic operations such as:</p>
 * <ul>
 *   <li>Record insertion with automatic space management</li>
 *   <li>Record retrieval and iteration</li>
 *   <li>Dirty page tracking for write optimization</li>
 *   <li>Free space calculation and management</li>
 * </ul>
 *
 * <p>The page format includes a 16-byte header followed by record data.
 * Each record is prefixed with its length for efficient parsing.</p>
 *
 * @author Nova SQL Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class Page {
    public static final int PAGE_SIZE = 4096;
    public static final int HEADER_SIZE = 16;

    private final int pageId;
    private final ByteBuffer data;
    private boolean dirty;
    private int recordCount;
    private int freeSpace;

    public Page(int pageId) {
        this.pageId = pageId;
        this.data = ByteBuffer.allocate(PAGE_SIZE);
        this.dirty = false;
        this.recordCount = 0;
        this.freeSpace = PAGE_SIZE - HEADER_SIZE;
        initializeHeader();
    }

    public Page(int pageId, byte[] pageData) {
        this.pageId = pageId;
        this.data = ByteBuffer.wrap(pageData);
        this.dirty = false;
        loadHeader();
    }

    private void initializeHeader() {
        data.position(0);
        data.putInt(pageId);
        data.putInt(recordCount);
        data.putInt(freeSpace);
        data.putInt(0); // reserved
    }

    private void loadHeader() {
        data.position(0);
        int storedPageId = data.getInt();
        this.recordCount = data.getInt();
        this.freeSpace = data.getInt();
        data.getInt(); // reserved

        if (storedPageId != pageId) {
            throw new IllegalStateException("Page ID mismatch");
        }
    }

    public boolean insertRecord(byte[] record) {
        if (record.length + 4 > freeSpace) { // 4 bytes for record length
            return false;
        }

        int insertPosition = HEADER_SIZE + (PAGE_SIZE - HEADER_SIZE - freeSpace);
        data.position(insertPosition);
        data.putInt(record.length);
        data.put(record);

        recordCount++;
        freeSpace -= (record.length + 4);
        dirty = true;
        updateHeader();

        return true;
    }

    public List<byte[]> getAllRecords() {
        List<byte[]> records = new ArrayList<>();
        data.position(HEADER_SIZE);

        for (int i = 0; i < recordCount; i++) {
            int recordLength = data.getInt();
            byte[] record = new byte[recordLength];
            data.get(record);
            records.add(record);
        }

        return records;
    }

    private void updateHeader() {
        int currentPosition = data.position();
        data.position(4); // skip pageId
        data.putInt(recordCount);
        data.putInt(freeSpace);
        data.position(currentPosition);
    }

    public int getPageId() {
        return pageId;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markClean() {
        dirty = false;
    }

    public byte[] getData() {
        return data.array();
    }

    public int getFreeSpace() {
        return freeSpace;
    }

    public int getRecordCount() {
        return recordCount;
    }
}
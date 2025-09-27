package com.novasql.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class BufferPool {
    private static final Logger logger = LoggerFactory.getLogger(BufferPool.class);
    private static final int DEFAULT_BUFFER_SIZE = 100;

    private final int maxPages;
    private final Map<Integer, Page> buffer;
    private final DiskManager diskManager;

    public BufferPool(DiskManager diskManager, int maxPages) {
        this.diskManager = diskManager;
        this.maxPages = maxPages;
        this.buffer = new LinkedHashMap<Integer, Page>(maxPages, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, Page> eldest) {
                if (size() > BufferPool.this.maxPages) {
                    Page evicted = eldest.getValue();
                    if (evicted.isDirty()) {
                        BufferPool.this.diskManager.writePage(evicted);
                        evicted.markClean();
                    }
                    logger.debug("Evicted page {} from buffer pool", evicted.getPageId());
                    return true;
                }
                return false;
            }
        };
    }

    public BufferPool(DiskManager diskManager) {
        this(diskManager, DEFAULT_BUFFER_SIZE);
    }

    public Page getPage(int pageId) {
        Page page = buffer.get(pageId);
        if (page != null) {
            logger.debug("Page {} found in buffer pool", pageId);
            return page;
        }

        logger.debug("Loading page {} from disk", pageId);
        page = diskManager.readPage(pageId);
        if (page == null) {
            page = new Page(pageId);
        }

        buffer.put(pageId, page);
        return page;
    }

    public void flushPage(int pageId) {
        Page page = buffer.get(pageId);
        if (page != null && page.isDirty()) {
            diskManager.writePage(page);
            page.markClean();
            logger.debug("Flushed page {} to disk", pageId);
        }
    }

    public void flushAll() {
        logger.info("Flushing all dirty pages to disk");
        for (Page page : buffer.values()) {
            if (page.isDirty()) {
                diskManager.writePage(page);
                page.markClean();
            }
        }
    }

    public int getBufferSize() {
        return buffer.size();
    }

    public int getMaxPages() {
        return maxPages;
    }
}
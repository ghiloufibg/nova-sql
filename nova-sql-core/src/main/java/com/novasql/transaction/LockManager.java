package com.novasql.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class LockManager {
    private static final Logger logger = LoggerFactory.getLogger(LockManager.class);

    private final ConcurrentMap<String, ReadWriteLock> resourceLocks;
    private final ConcurrentMap<Long, Set<String>> transactionLocks;

    public LockManager() {
        this.resourceLocks = new ConcurrentHashMap<>();
        this.transactionLocks = new ConcurrentHashMap<>();
    }

    public boolean acquireSharedLock(long transactionId, String resource) {
        try {
            ReadWriteLock lock = resourceLocks.computeIfAbsent(resource, k -> new ReentrantReadWriteLock());
            lock.readLock().lock();

            transactionLocks.computeIfAbsent(transactionId, k -> new ConcurrentSkipListSet<>()).add(resource);

            logger.debug("Acquired shared lock on {} for transaction {}", resource, transactionId);
            return true;

        } catch (Exception e) {
            logger.error("Failed to acquire shared lock on {} for transaction {}", resource, transactionId, e);
            return false;
        }
    }

    public boolean acquireExclusiveLock(long transactionId, String resource) {
        try {
            ReadWriteLock lock = resourceLocks.computeIfAbsent(resource, k -> new ReentrantReadWriteLock());
            lock.writeLock().lock();

            transactionLocks.computeIfAbsent(transactionId, k -> new ConcurrentSkipListSet<>()).add(resource);

            logger.debug("Acquired exclusive lock on {} for transaction {}", resource, transactionId);
            return true;

        } catch (Exception e) {
            logger.error("Failed to acquire exclusive lock on {} for transaction {}", resource, transactionId, e);
            return false;
        }
    }

    public void releaseLock(long transactionId, String resource) {
        try {
            ReadWriteLock lock = resourceLocks.get(resource);
            if (lock == null) {
                logger.warn("No lock found for resource: {}", resource);
                return;
            }

            // Try to release both read and write locks (one will succeed, one will fail silently)
            try {
                lock.readLock().unlock();
            } catch (IllegalMonitorStateException ignored) {
                // Not holding read lock
            }

            try {
                lock.writeLock().unlock();
            } catch (IllegalMonitorStateException ignored) {
                // Not holding write lock
            }

            Set<String> locks = transactionLocks.get(transactionId);
            if (locks != null) {
                locks.remove(resource);
                if (locks.isEmpty()) {
                    transactionLocks.remove(transactionId);
                }
            }

            logger.debug("Released lock on {} for transaction {}", resource, transactionId);

        } catch (Exception e) {
            logger.error("Failed to release lock on {} for transaction {}", resource, transactionId, e);
        }
    }

    public void releaseAllLocks(long transactionId) {
        Set<String> locks = transactionLocks.get(transactionId);
        if (locks == null) {
            return;
        }

        for (String resource : locks) {
            releaseLock(transactionId, resource);
        }

        logger.debug("Released all locks for transaction {}", transactionId);
    }
}
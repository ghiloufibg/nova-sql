package com.novasql.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages database transactions and provides ACID compliance for the Nova SQL database engine.
 *
 * <p>The TransactionManager is responsible for:</p>
 * <ul>
 *   <li>Creating and managing transaction lifecycles</li>
 *   <li>Coordinating with the lock manager for concurrency control</li>
 *   <li>Ensuring atomicity and consistency of database operations</li>
 *   <li>Handling transaction commits and rollbacks</li>
 * </ul>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Thread-safe transaction management</li>
 *   <li>Automatic lock acquisition and release</li>
 *   <li>Transaction state tracking</li>
 *   <li>Support for concurrent read/write operations</li>
 * </ul>
 *
 * @author Nova SQL Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class TransactionManager {
    private static final Logger logger = LoggerFactory.getLogger(TransactionManager.class);

    private final ConcurrentMap<Long, Transaction> activeTransactions;
    private final LockManager lockManager;

    public TransactionManager() {
        this.activeTransactions = new ConcurrentHashMap<>();
        this.lockManager = new LockManager();
    }

    public Transaction beginTransaction() {
        Transaction transaction = new Transaction(this);
        activeTransactions.put(transaction.getTransactionId(), transaction);

        logger.debug("Started transaction {}", transaction.getTransactionId());
        return transaction;
    }

    public void commitTransaction(Transaction transaction) {
        if (!activeTransactions.containsKey(transaction.getTransactionId())) {
            throw new IllegalArgumentException("Transaction not found: " + transaction.getTransactionId());
        }

        try {
            // Release all locks held by this transaction
            lockManager.releaseAllLocks(transaction.getTransactionId());

            activeTransactions.remove(transaction.getTransactionId());
            transaction.setState(Transaction.TransactionState.COMMITTED);

            logger.debug("Committed transaction {}", transaction.getTransactionId());

        } catch (Exception e) {
            logger.error("Failed to commit transaction {}", transaction.getTransactionId(), e);
            abortTransaction(transaction);
            throw new RuntimeException("Transaction commit failed", e);
        }
    }

    public void abortTransaction(Transaction transaction) {
        if (!activeTransactions.containsKey(transaction.getTransactionId())) {
            logger.warn("Attempting to abort unknown transaction: {}", transaction.getTransactionId());
            return;
        }

        try {
            // Release all locks held by this transaction
            lockManager.releaseAllLocks(transaction.getTransactionId());

            activeTransactions.remove(transaction.getTransactionId());
            transaction.setState(Transaction.TransactionState.ABORTED);

            logger.debug("Aborted transaction {}", transaction.getTransactionId());

        } catch (Exception e) {
            logger.error("Failed to abort transaction {}", transaction.getTransactionId(), e);
        }
    }

    public boolean acquireSharedLock(long transactionId, String resource) {
        return lockManager.acquireSharedLock(transactionId, resource);
    }

    public boolean acquireExclusiveLock(long transactionId, String resource) {
        return lockManager.acquireExclusiveLock(transactionId, resource);
    }

    public void releaseLock(long transactionId, String resource) {
        lockManager.releaseLock(transactionId, resource);
    }

    public int getActiveTransactionCount() {
        return activeTransactions.size();
    }

    public Transaction getTransaction(long transactionId) {
        return activeTransactions.get(transactionId);
    }
}
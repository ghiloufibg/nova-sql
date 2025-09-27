package com.novasql.transaction;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class Transaction {
    private static final AtomicLong transactionIdGenerator = new AtomicLong(1);

    private final long transactionId;
    private final Instant startTime;
    private TransactionState state;
    private final TransactionManager manager;

    public enum TransactionState {
        ACTIVE, COMMITTED, ABORTED
    }

    public Transaction(TransactionManager manager) {
        this.transactionId = transactionIdGenerator.getAndIncrement();
        this.startTime = Instant.now();
        this.state = TransactionState.ACTIVE;
        this.manager = manager;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public TransactionState getState() {
        return state;
    }

    public void commit() {
        if (state != TransactionState.ACTIVE) {
            throw new IllegalStateException("Cannot commit transaction in state: " + state);
        }

        manager.commitTransaction(this);
        state = TransactionState.COMMITTED;
    }

    public void abort() {
        if (state != TransactionState.ACTIVE) {
            throw new IllegalStateException("Cannot abort transaction in state: " + state);
        }

        manager.abortTransaction(this);
        state = TransactionState.ABORTED;
    }

    void setState(TransactionState state) {
        this.state = state;
    }

    public boolean isActive() {
        return state == TransactionState.ACTIVE;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + transactionId +
                ", state=" + state +
                ", startTime=" + startTime +
                '}';
    }
}
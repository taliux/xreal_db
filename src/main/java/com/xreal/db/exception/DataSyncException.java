package com.xreal.db.exception;

public class DataSyncException extends RuntimeException {
    public DataSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
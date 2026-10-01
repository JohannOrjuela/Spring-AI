package com.example.chat.exception;

/** Safe boundary exception; provider payload and validation values are deliberately not retained. */
public final class ModelOperationException extends RuntimeException {
    private final long elapsedMs;
    public ModelOperationException(long startNanos) {
        super("Model operation failed");
        this.elapsedMs = Math.max(0, (System.nanoTime() - startNanos) / 1_000_000);
    }
    public long getElapsedMs() { return elapsedMs; }
}

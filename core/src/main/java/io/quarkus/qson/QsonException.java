package io.quarkus.qson;

public class QsonException extends RuntimeException {
    public QsonException() {
    }

    public QsonException(String message) {
        super(message);
    }

    public QsonException(String message, Throwable cause) {
        super(message, cause);
    }

    public QsonException(Throwable cause) {
        super(cause);
    }
}

package com.tailoredbrands.pipeline.error;

public class ProcessingException extends RuntimeException {

    private final ErrorType type;

    public ProcessingException(ErrorType type) {
        super();
        this.type = type;
    }

    public ProcessingException(ErrorType type, String message) {
        super(message);
        this.type = type;
    }

    public ProcessingException(ErrorType type, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
    }

    public ProcessingException(ErrorType type, Throwable cause) {
        super(cause);
        this.type = type;
    }

    public ErrorType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "ProcessingException{" +
                "type=" + type +
                ", message=" + getMessage() +
                '}';
    }
}


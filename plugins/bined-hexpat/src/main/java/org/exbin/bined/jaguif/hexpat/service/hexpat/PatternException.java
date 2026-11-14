package org.exbin.bined.jaguif.hexpat.service.hexpat;

public class PatternException extends RuntimeException {
    public final int line;

    public PatternException(String message, int line) {
        super(message);
        this.line = line;
    }

    @Override
    public String toString() {
        return line > 0 ? "line " + line + ": " + getMessage() : getMessage();
    }
}

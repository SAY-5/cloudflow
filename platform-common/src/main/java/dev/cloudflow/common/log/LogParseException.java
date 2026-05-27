package dev.cloudflow.common.log;

/** Raised when a log line cannot be parsed into the canonical shape. */
public class LogParseException extends RuntimeException {

  public LogParseException(String message) {
    super(message);
  }

  public LogParseException(String message, Throwable cause) {
    super(message, cause);
  }
}

package ru.last.lastitems.debug;

public interface DebugType {
    void info(String message);
    void warn(String message);
    void error(String message);
    void error(String message, Throwable t);
    void critical(String message);
}
package com.example.timesheet.exceptions;


import lombok.Getter;

@Getter
public class TimeSheetException extends RuntimeException {
    private final String errorCode;

    public TimeSheetException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TimeSheetException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}


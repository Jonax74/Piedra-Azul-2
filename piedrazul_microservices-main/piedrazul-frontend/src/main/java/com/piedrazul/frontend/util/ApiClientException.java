package com.piedrazul.frontend.util;

public class ApiClientException extends RuntimeException {

    private final ApiErrorParser.ParsedApiError parsedError;

    public ApiClientException(ApiErrorParser.ParsedApiError parsedError, Throwable cause) {
        super(parsedError.message(), cause);
        this.parsedError = parsedError;
    }

    public ApiErrorParser.ParsedApiError getParsedError() {
        return parsedError;
    }
}

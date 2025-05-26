package com.example.timesheet.config;


import com.example.timesheet.common.constants.ErrorCode;
import com.example.timesheet.dto.ErrorResponse;
import com.example.timesheet.exceptions.TimeSheetException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

// FeignErrorDecoder.java
@Slf4j
@Component
@RequiredArgsConstructor
public class FeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    @Override
    public Exception decode(String methodKey, Response response) {
        try {
            String body = Util.toString(response.body().asReader());
            log.error("Feign client error: {}", body);

            // Try to parse the error response
            ErrorResponse errorResponse = objectMapper.readValue(body, ErrorResponse.class);

            return new TimeSheetException(
                    errorResponse.getError_code(),
                    errorResponse.getMessage()
            );
        } catch (IOException e) {
            log.error("Failed to process Feign error response", e);
            return new TimeSheetException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Error communicating with Identity Service"
            );
        }
    }
}
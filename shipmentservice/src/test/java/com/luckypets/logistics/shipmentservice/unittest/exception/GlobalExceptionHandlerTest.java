package com.luckypets.logistics.shipmentservice.unittest.exception;

import com.luckypets.logistics.shipmentservice.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleIllegalArgument_shouldReturnBadRequestAndMessage() {
        String message = "Ungültige Eingabe";
        IllegalArgumentException ex = new IllegalArgumentException(message);

        ResponseEntity<String> response = handler.handleIllegalArgument(ex);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals(message, response.getBody());
    }

    @Test
    void handleUnexpectedException_shouldReturnInternalServerError() {
        Exception ex = new Exception("Interner Fehler");
        ResponseEntity<String> response = handler.handleUnexpectedException(ex);

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("Unbekannter Fehler aufgetreten"));
        assertTrue(response.getBody().contains("Interner Fehler"));
    }

    @Test
    void handleMissingRequestParam_shouldReturnBadRequestAndMessage() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("destination", "String");
        ResponseEntity<String> response = handler.handleMissingRequestParam(ex);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Zieladresse darf nicht leer sein.", response.getBody());
    }
}

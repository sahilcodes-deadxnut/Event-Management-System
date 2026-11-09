package com.eventhub.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        log.warn("[CLIENT ERROR] Bad request on {}: {}", request.getRequestURI(), ex.getMessage());

        // Return JSON response if requested via REST API
        if (isJsonRequest(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Bad Request", "message", ex.getMessage()));
        }

        // Redirect back with flash error message for web requests
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/events");
    }

    @ExceptionHandler(IllegalStateException.class)
    public Object handleIllegalState(IllegalStateException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        log.warn("[BUSINESS RULE VIOLATION] Conflict on {}: {}", request.getRequestURI(), ex.getMessage());

        if (isJsonRequest(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Conflict", "message", ex.getMessage()));
        }

        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/events");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Object handleGenericException(Exception ex, HttpServletRequest request, Model model) {
        log.error("[SYSTEM ERROR] Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        if (isJsonRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal Server Error", "message", "An unexpected error occurred. Please try again."));
        }

        model.addAttribute("errorTitle", "Unexpected Application Error");
        model.addAttribute("errorMessage", "The system encountered an unexpected error while processing your request. Please try again or contact the event administrator.");
        model.addAttribute("requestUri", request.getRequestURI());
        return "error";
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();
        return (accept != null && accept.contains("application/json")) || uri.startsWith("/api/");
    }
}

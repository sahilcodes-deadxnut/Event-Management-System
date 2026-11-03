package com.eventhub.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SecuritySanitizer {

    // Regex pattern matching HTML script tags to block Cross-Site Scripting (XSS)
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE);
    // Regex pattern matching HTML tags
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    public static String sanitize(String input) {
        // Return empty string if input is null
        if (input == null) {
            return "";
        }

        // Trim leading and trailing whitespace
        String sanitized = input.trim();
        // Remove script tags and contents
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        // Strip general HTML tags to prevent HTML injection in text fields
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll("");
        // Normalize multiple spaces into single space
        sanitized = sanitized.replaceAll("\\s+", " ");

        return sanitized;
    }

    public static String escapeForHtml(String input) {
        // Return empty string if input is null
        if (input == null) {
            return "";
        }

        // Escape standard HTML special characters
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}

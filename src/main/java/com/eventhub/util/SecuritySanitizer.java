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
        if (input == null) {
            return "";
        }

        String sanitized = input.trim();
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = sanitized.replaceAll("\\s+", " ");

        return sanitized;
    }

    public static String escapeForHtml(String input) {
        if (input == null) {
            return "";
        }

        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }

    /**
     * Normalizes an email address consistently across all layers:
     * trims whitespace, removes control characters, and converts to lowercase.
     */
    public static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.replaceAll("[\\p{Cntrl}]", "").trim().toLowerCase();
    }
}

package com.eventhub.security;

import com.eventhub.servlet.ParticipantCsvExportServlet;
import com.eventhub.util.SecuritySanitizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityAndValidationTest {

    private final PasswordHasher passwordHasher = new PasswordHasher();

    @Test
    @DisplayName("PasswordHasher should generate unique salts and successfully verify correct passwords")
    void testPasswordHashingAndVerification() {
        String rawPassword = "SuperSecretPassword123!";
        String hash1 = passwordHasher.hashPassword(rawPassword);
        String hash2 = passwordHasher.hashPassword(rawPassword);

        // Salt uniqueness
        assertThat(hash1).isNotEqualTo(hash2);

        // Verification
        assertThat(passwordHasher.verifyPassword(rawPassword, hash1)).isTrue();
        assertThat(passwordHasher.verifyPassword(rawPassword, hash2)).isTrue();
        assertThat(passwordHasher.verifyPassword("WrongPassword", hash1)).isFalse();
        assertThat(passwordHasher.verifyPassword("", hash1)).isFalse();
        assertThat(passwordHasher.verifyPassword(null, hash1)).isFalse();
    }

    @Test
    @DisplayName("PasswordHasher rejects empty or null passwords during hashing")
    void testPasswordHashingValidation() {
        assertThatThrownBy(() -> passwordHasher.hashPassword(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> passwordHasher.hashPassword(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SecuritySanitizer should strip script tags and HTML injection to prevent XSS")
    void testXssSanitization() {
        String malicious = "<script>alert('pwned')</script>John Doe <b>Developer</b>";
        String sanitized = SecuritySanitizer.sanitize(malicious);

        assertThat(sanitized).doesNotContain("<script>");
        assertThat(sanitized).doesNotContain("</script>");
        assertThat(sanitized).doesNotContain("<b>");
        assertThat(sanitized).isEqualTo("John Doe Developer");
    }

    @Test
    @DisplayName("SecuritySanitizer correctly normalizes email addresses across variations")
    void testEmailNormalization() {
        String input1 = "   User.Name@Example.COM   ";
        String input2 = "user.name@example.com\n";

        assertThat(SecuritySanitizer.normalizeEmail(input1)).isEqualTo("user.name@example.com");
        assertThat(SecuritySanitizer.normalizeEmail(input2)).isEqualTo("user.name@example.com");
        assertThat(SecuritySanitizer.normalizeEmail(null)).isEmpty();
    }

    @Test
    @DisplayName("CSV export escaping immunizes against CSV Formula Injection (DDE attacks)")
    void testCsvFormulaInjectionDefense() {
        String formula1 = "=cmd|' /C calc'!A0";
        String formula2 = "+123456";
        String formula3 = "-SUM(A1:A10)";
        String formula4 = "@evil_payload";
        String normal = "John \"The Coder\" Doe";

        // Dangerous formula prefixes must be sanitized with a leading single quote
        assertThat(ParticipantCsvExportServlet.escapeCsv(formula1)).startsWith("\"'=");
        assertThat(ParticipantCsvExportServlet.escapeCsv(formula2)).startsWith("\"'+");
        assertThat(ParticipantCsvExportServlet.escapeCsv(formula3)).startsWith("\"'-");
        assertThat(ParticipantCsvExportServlet.escapeCsv(formula4)).startsWith("\"'@");

        // Normal text with double quotes doubled
        assertThat(ParticipantCsvExportServlet.escapeCsv(normal)).isEqualTo("\"John \"\"The Coder\"\" Doe\"");
        assertThat(ParticipantCsvExportServlet.escapeCsv(null)).isEqualTo("\"\"");
    }
}

package com.siberanka.interactiveholograms.display;

public final class TextLayoutScanner {

    private TextLayoutScanner() {
    }

    public static int visibleLength(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }

        int length = content.length();
        int visibleCount = 0;
        int i = 0;

        while (i < length) {
            char c = content.charAt(i);

            // 1. Check legacy color / formatting (& or §)
            if ((c == '&' || c == '§') && i + 1 < length) {
                char next = content.charAt(i + 1);
                // Hex color check: &#RRGGBB or §#RRGGBB
                if (next == '#' && i + 7 < length && isHex(content, i + 2, 6)) {
                    i += 8;
                    continue;
                }
                // Legacy single-character code: &0-9a-fA-FK-ORk-or
                if (isLegacyCode(next)) {
                    i += 2;
                    continue;
                }
            }

            // 2. Check MiniMessage tag <...>
            if (c == '<') {
                int closingIndex = content.indexOf('>', i + 1);
                if (closingIndex != -1 && closingIndex - i <= 256) {
                    i = closingIndex + 1;
                    continue;
                }
            }

            // 3. Check Placeholder %...%
            if (c == '%') {
                int closingIndex = content.indexOf('%', i + 1);
                if (closingIndex != -1 && closingIndex - i <= 128) {
                    // Check no newline inside placeholder
                    boolean validPlaceholder = true;
                    for (int p = i + 1; p < closingIndex; p++) {
                        char pc = content.charAt(p);
                        if (pc == '\r' || pc == '\n') {
                            validPlaceholder = false;
                            break;
                        }
                    }
                    if (validPlaceholder) {
                        visibleCount += 16; // Standard estimated length for dynamic placeholder
                        i = closingIndex + 1;
                        continue;
                    }
                }
            }

            // Normal unicode code point
            int codePoint = content.codePointAt(i);
            visibleCount++;
            i += Character.charCount(codePoint);
        }

        return visibleCount;
    }

    private static boolean isLegacyCode(char c) {
        return (c >= '0' && c <= '9') ||
               (c >= 'a' && c <= 'f') ||
               (c >= 'A' && c <= 'F') ||
               (c >= 'k' && c <= 'o') ||
               (c >= 'K' && c <= 'O') ||
               c == 'r' || c == 'R';
    }

    private static boolean isHex(String s, int start, int len) {
        for (int i = start; i < start + len; i++) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }
}

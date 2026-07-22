package com.siberanka.interactiveholograms.api.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateCheckerTest {
    @Test
    void parsesAndPinsOfficialReleaseUrl() throws Exception {
        UpdateChecker.ReleaseInfo release = UpdateChecker.parseLatestRelease(
                "{\"tag_name\":\"v3.2.0\",\"html_url\":\"https://github.com/siberanka/InteractiveHolograms/releases/tag/v3.2.0\"}");
        assertEquals("3.2.0", release.getVersion());
        assertEquals("https://github.com/siberanka/InteractiveHolograms/releases/tag/v3.2.0", release.getUrl());
    }

    @Test
    void rejectsUntrustedReleaseLink() {
        assertThrows(IOException.class, () -> UpdateChecker.parseLatestRelease(
                "{\"tag_name\":\"v99.0.0\",\"html_url\":\"https://example.com/malware.jar\"}"));
    }

    @Test
    void rejectsMalformedOrNonSemanticVersion() {
        assertThrows(IOException.class, () -> UpdateChecker.parseLatestRelease(
                "{\"tag_name\":\"latest\",\"html_url\":\"https://github.com/siberanka/InteractiveHolograms/releases/tag/latest\"}"));
    }
}

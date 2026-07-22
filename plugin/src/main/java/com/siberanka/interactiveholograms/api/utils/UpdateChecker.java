package com.siberanka.interactiveholograms.api.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Checks the official GitHub latest-release endpoint without blocking the server thread. */
public final class UpdateChecker {
    public static final String RELEASES_URL = "https://github.com/siberanka/InteractiveHolograms/releases";
    public static final String LATEST_RELEASE_URL = RELEASES_URL + "/latest";
    private static final String API_URL = "https://api.github.com/repos/siberanka/InteractiveHolograms/releases/latest";
    private static final int MAX_RESPONSE_CHARS = 256 * 1024;
    private static final Pattern TAG = Pattern.compile("\\\"tag_name\\\"\\s*:\\s*\\\"([^\\\"]{1,64})\\\"");
    private static final Pattern HTML_URL = Pattern.compile("\\\"html_url\\\"\\s*:\\s*\\\"([^\\\"]{1,512})\\\"");
    private final JavaPlugin plugin;

    public UpdateChecker(JavaPlugin plugin) {
        if (plugin == null) throw new IllegalArgumentException("plugin cannot be null");
        this.plugin = plugin;
    }

    public void getLatestRelease(Consumer<ReleaseInfo> consumer) {
        if (consumer == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                consumer.accept(fetch());
            } catch (IOException | RuntimeException exception) {
                Log.info("Unable to check GitHub Releases for updates: " + exception.getMessage());
            }
        });
    }

    private ReleaseInfo fetch() throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(API_URL).openConnection();
        connection.setConnectTimeout(5_000); connection.setReadTimeout(5_000);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        connection.setRequestProperty("User-Agent", "InteractiveHolograms-UpdateChecker");
        connection.setInstanceFollowRedirects(false);
        try {
            if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                throw new IOException("GitHub API returned HTTP " + connection.getResponseCode());
            }
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                char[] buffer = new char[4096]; int read;
                while ((read = reader.read(buffer)) != -1) {
                    if (response.length() + read > MAX_RESPONSE_CHARS) throw new IOException("GitHub response exceeded safety limit");
                    response.append(buffer, 0, read);
                }
            }
            return parseLatestRelease(response.toString());
        } finally {
            connection.disconnect();
        }
    }

    static ReleaseInfo parseLatestRelease(String json) throws IOException {
        if (json == null) throw new IOException("Empty GitHub response");
        Matcher tag = TAG.matcher(json); Matcher url = HTML_URL.matcher(json);
        if (!tag.find() || !url.find()) throw new IOException("Malformed GitHub release response");
        String version = tag.group(1).replaceFirst("^[vV]", "");
        String releaseUrl = url.group(1).replace("\\/", "/");
        String prefix = RELEASES_URL + "/tag/";
        if (!version.matches("[0-9]+\\.[0-9]+\\.[0-9]+(?:[-+][A-Za-z0-9.-]+)?") || !releaseUrl.startsWith(prefix)) {
            throw new IOException("GitHub release response failed validation");
        }
        return new ReleaseInfo(version, releaseUrl);
    }

    public static final class ReleaseInfo {
        private final String version; private final String url;
        private ReleaseInfo(String version, String url) { this.version = version; this.url = url; }
        public String getVersion() { return version; }
        public String getUrl() { return url; }
    }
}

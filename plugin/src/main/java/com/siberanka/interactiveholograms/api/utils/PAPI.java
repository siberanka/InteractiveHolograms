package com.siberanka.interactiveholograms.api.utils;

import com.siberanka.interactiveholograms.integration.Integration;
import lombok.experimental.UtilityClass;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class PAPI {

    /**
     * Check if PlaceholderAPI is available.
     *
     * @return True if PlaceholderAPI is available.
     */
    public static boolean isAvailable() {
        return Integration.PLACEHOLDER_API.isAvailable();
    }

    /**
     * Set placeholders to given String for a given Player.
     *
     * @param player The player.
     * @param string The string.
     * @return The string with replaced placeholders.
     */
    public static String setPlaceholders(Player player, String string) {
        if (isAvailable() && string != null) {
            String result = string;
            try {
                result = PlaceholderAPI.setPlaceholders(player, result);
                if (result == null) result = string;
            } catch (RuntimeException e) {
                logFailure(e, string, player, "standard");
                return string;
            }

            try {
                String bracketResult = PlaceholderAPI.setBracketPlaceholders(player, result);
                if (bracketResult != null) result = bracketResult;
            } catch (RuntimeException e) {
                logFailure(e, result, player, "bracket");
            } catch (LinkageError ignored) {
                // Older PlaceholderAPI builds may not expose bracket placeholders.
            }

            try {
                if (player != null && containsRelationalPlaceholders(result)) {
                    String relationalResult = PlaceholderAPI.setRelationalPlaceholders(player, player, result);
                    if (relationalResult != null) result = relationalResult;
                }
            } catch (RuntimeException e) {
                logFailure(e, result, player, "relational");
            } catch (LinkageError ignored) {
                // Retain standard/bracket output when an older PAPI has no relational API.
            }
            return result;
        }
        return string;
    }

    /**
     * Set placeholders to the given List of Strings for a given Player.
     *
     * @param player     The player.
     * @param stringList The string list.
     * @return The string with replaced placeholders.
     */
    public static List<String> setPlaceholders(Player player, List<String> stringList) {
        if (isAvailable()) {
            return stringList.stream().map(s -> setPlaceholders(player, s)).collect(Collectors.toList());
        }
        return stringList;
    }

    /**
     * Check if the given string contains any placeholders.
     *
     * @param string The string.
     * @return True if the string contains any placeholders, false otherwise.
     */
    public static boolean containsPlaceholders(String string) {
        if (isAvailable() && string != null) {
            try {
                return PlaceholderAPI.containsPlaceholders(string)
                        || PlaceholderAPI.containsBracketPlaceholders(string)
                        || containsRelationalPlaceholders(string);
            } catch (RuntimeException | LinkageError ignored) {
                // Syntax pre-checks preserve compatibility with older or partially loaded PAPI builds.
                return string.indexOf('%') >= 0 || (string.indexOf('{') >= 0 && string.indexOf('}') >= 0);
            }
        }
        return false;
    }

    private static boolean containsRelationalPlaceholders(String string) {
        if (string == null || string.indexOf("%rel_") < 0) return false;
        return PlaceholderAPI.getRelationalPlaceholderPattern() != null
                && PlaceholderAPI.getRelationalPlaceholderPattern().matcher(string).find();
    }

    private static void logFailure(Throwable failure, String string, Player player, String syntax) {
        Log.warn("Failed to replace %s placeholders in string '%s' for player '%s'."
                        + " This issue likely originates from a placeholder provided by another plugin."
                        + " Please contact the developer(s) of any plugin mentioned in the stack trace.",
                failure, syntax, string, player == null ? "console" : player.getName());
    }

}

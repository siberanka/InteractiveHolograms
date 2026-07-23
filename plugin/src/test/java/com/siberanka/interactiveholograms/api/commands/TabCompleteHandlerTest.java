package com.siberanka.interactiveholograms.api.commands;

import com.siberanka.interactiveholograms.api.InteractiveHolograms;
import com.siberanka.interactiveholograms.api.InteractiveHologramsAPI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class TabCompleteHandlerTest {

    private static MockedStatic<InteractiveHologramsAPI> interactiveHologramsApi;

    @BeforeAll
    static void setUpApi() {
        InteractiveHolograms plugin = mock(InteractiveHolograms.class);
        interactiveHologramsApi = mockStatic(InteractiveHologramsAPI.class);
        interactiveHologramsApi.when(InteractiveHologramsAPI::get).thenReturn(plugin);
    }

    @AfterAll
    static void tearDownApi() {
        interactiveHologramsApi.close();
    }

    @Test
    void currentValueIsFirstAndDuplicatesAreRemoved() {
        List<String> result = TabCompleteHandler.getPartialMatchesWithCurrent(
                "", "true", Arrays.asList("false", "true"));

        assertEquals(Arrays.asList("true", "false"), result);
    }

    @Test
    void currentValueSupportsSpacesFormattingAndPlaceholders() {
        String current = "<aqua>Welcome %player_name%</aqua> &7to spawn";

        assertEquals(Collections.singletonList(current),
                TabCompleteHandler.getPartialMatchesWithCurrent("", current, Collections.emptyList()));
        assertEquals(Collections.singletonList(current),
                TabCompleteHandler.getPartialMatchesWithCurrent("<aqua>", current, Collections.emptyList()));
    }

    @Test
    void unsafeOrOversizedCurrentValuesAreNotSentToClients() {
        StringBuilder oversized = new StringBuilder();
        for (int i = 0; i <= TabCompleteHandler.MAX_SUGGESTION_LENGTH; i++) {
            oversized.append('a');
        }

        assertEquals(Collections.singletonList("fallback"),
                TabCompleteHandler.getPartialMatchesWithCurrent(
                        "", "unsafe\ncommand", Collections.singletonList("fallback")));
        assertEquals(Collections.singletonList("fallback"),
                TabCompleteHandler.getPartialMatchesWithCurrent(
                        "", oversized.toString(), Collections.singletonList("fallback")));
    }
}

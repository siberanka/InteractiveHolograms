package com.siberanka.interactiveholograms.display.command;

import com.siberanka.interactiveholograms.api.InteractiveHolograms;
import com.siberanka.interactiveholograms.api.InteractiveHologramsAPI;
import com.siberanka.interactiveholograms.display.DisplayService;
import com.siberanka.interactiveholograms.display.TextDisplay;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class DisplayTabCompleteHelperTest {

    private static MockedStatic<InteractiveHologramsAPI> interactiveHologramsApi;
    private DisplayService displayService;
    private TextDisplay textDisplay;
    private DisplayTabCompleteHelper helper;

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

    @BeforeEach
    void setUp() {
        displayService = mock(DisplayService.class);
        textDisplay = mock(TextDisplay.class);
        helper = new DisplayTabCompleteHelper(displayService);
        when(displayService.getDisplay("welcome")).thenReturn(textDisplay);
        when(textDisplay.getLines()).thenReturn(Arrays.asList(
                "<gold>InteractiveHolograms</gold>",
                "<gray>Welcome, <aqua>%player_name%</aqua>!"
        ));
    }

    @Test
    void returnsTheExactSelectedLineAsOneSuggestion() {
        assertEquals(Collections.singletonList("<gray>Welcome, <aqua>%player_name%</aqua>!"),
                helper.getLineContent("welcome", "2", ""));
    }

    @Test
    void setLineCommandExposesCurrentContentAfterTheLineIndex() {
        TextDisplaySetLineCommand command = new TextDisplaySetLineCommand(displayService, helper);

        List<String> result = command.getTabCompleteHandler().handleTabComplete(
                mock(CommandSender.class), new String[]{"welcome", "2", ""});

        assertEquals(Collections.singletonList("<gray>Welcome, <aqua>%player_name%</aqua>!"), result);
    }

    @Test
    void invalidDisplayOrLineDoesNotLeakUnrelatedContent() {
        assertEquals(Collections.emptyList(), helper.getLineContent("missing", "1", ""));
        assertEquals(Collections.emptyList(), helper.getLineContent("welcome", "not-a-number", ""));
        assertEquals(Collections.emptyList(), helper.getLineContent("welcome", "3", ""));
    }

    @Test
    void swapLineFiltersTheSecondIndexWithItsOwnToken() {
        when(textDisplay.getLines()).thenReturn(Arrays.asList(
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"
        ));
        TextDisplaySwapLineCommand command = new TextDisplaySwapLineCommand(displayService, helper);

        List<String> result = command.getTabCompleteHandler().handleTabComplete(
                mock(CommandSender.class), new String[]{"welcome", "1", "2"});

        assertEquals(Collections.singletonList("2"), result);
    }
}

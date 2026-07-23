package com.siberanka.interactiveholograms.display.config;

import com.siberanka.interactiveholograms.display.TextDisplay;
import com.siberanka.interactiveholograms.display.attribute.AttributeConfigMapper;
import com.siberanka.interactiveholograms.display.config.dto.ConfigDecentLocation;
import com.siberanka.interactiveholograms.display.config.dto.ConfigDisplay;
import com.siberanka.interactiveholograms.display.config.dto.ConfigTextLine;
import com.siberanka.interactiveholograms.display.config.dto.ConfigTextPage;
import com.siberanka.interactiveholograms.platform.api.capability.PlatformMaterialService;
import com.siberanka.interactiveholograms.platform.api.data.display.DisplayType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DisplayConfigMapperPagesTest {

    @Test
    void mapsEveryPageLineHeightAndActionBothWays() {
        ConfigDisplay dto = new ConfigDisplay();
        dto.setName("pages");
        dto.setType(DisplayType.TEXT);
        ConfigDecentLocation location = new ConfigDecentLocation();
        location.setWorld("world");
        location.setY(64.0d);
        dto.setLocation(location);
        ConfigTextPage first = page("first", 0.5d, "NEXT_PAGE");
        ConfigTextPage second = page("second", 0.2d, "PAGE:1");
        dto.setPages(Arrays.asList(first, second));

        DisplayConfigMapper mapper = new DisplayConfigMapper(
                mock(AttributeConfigMapper.class), mock(PlatformMaterialService.class));
        TextDisplay display = (TextDisplay) mapper.toDomain(dto);

        assertEquals(2, display.getPageCount());
        assertEquals(0.5d, display.getPages().get(0).getLines().get(0).getHeight());
        assertEquals("NEXT_PAGE",
                display.getPages().get(0).getActions().values().iterator().next().get(0).toString());
        UUID viewer = UUID.randomUUID();
        assertTrue(display.nextPage(viewer));
        assertEquals(Collections.singletonList("second"), display.getLines(viewer));

        ConfigDisplay roundTrip = mapper.toDto(display);
        assertEquals(2, roundTrip.getPages().size());
        assertEquals("second", roundTrip.getPages().get(1).getLines().get(0).getContent());
        assertEquals(0.2d, roundTrip.getPages().get(1).getLines().get(0).getHeight());
        assertEquals("PAGE:1", roundTrip.getPages().get(1).getActions().get("LEFT").get(0));
    }

    private ConfigTextPage page(String content, double height, String action) {
        ConfigTextLine line = new ConfigTextLine();
        line.setContent(content);
        line.setHeight(height);
        ConfigTextPage page = new ConfigTextPage();
        page.setLines(Collections.singletonList(line));
        page.setActions(Collections.singletonMap("LEFT", Collections.singletonList(action)));
        return page;
    }
}

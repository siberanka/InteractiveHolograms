package com.siberanka.interactiveholograms.display.attribute;

import com.siberanka.interactiveholograms.api.commands.DecentCommandException;
import com.siberanka.interactiveholograms.display.DisplayBase;
import com.siberanka.interactiveholograms.display.attribute.command.handler.AttributeCommandHandlerRegistry;
import com.siberanka.interactiveholograms.display.attribute.defaults.AttributeDefaultService;
import com.siberanka.interactiveholograms.display.attribute.definition.AttributeDefinition;
import com.siberanka.interactiveholograms.display.attribute.definition.AttributeDefinitionRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AttributeCommandServiceTest {

    @Test
    void setAttributeThrowsDecentCommandExceptionWhenHandlerIsMissing() {
        AttributeDefinitionRegistry definitionRegistry = mock(AttributeDefinitionRegistry.class);
        AttributeCommandHandlerRegistry handlerRegistry = mock(AttributeCommandHandlerRegistry.class);
        AttributeDefaultService defaultService = mock(AttributeDefaultService.class);
        AttributeDefinition definition = mock(AttributeDefinition.class);
        DisplayBase display = mock(DisplayBase.class);

        AttributeKey<String> key = AttributeKey.of("test_attr", String.class);
        when(definition.getKey()).thenReturn(key);
        when(definition.getName()).thenReturn("test_attr");
        when(handlerRegistry.getHandler(key, new String[]{"val"})).thenReturn(null);

        AttributeCommandService service = new AttributeCommandService(definitionRegistry, handlerRegistry, defaultService);

        assertThrows(DecentCommandException.class, () -> service.setAttribute(display, definition, new String[]{"val"}));
    }
}

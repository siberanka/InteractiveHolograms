/*
 * This file is part of InteractiveHolograms, licensed under the GNU GPL v3.0 License.
 * Copyright (C) DecentSoftware.eu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.siberanka.interactiveholograms.display;

import com.siberanka.interactiveholograms.api.animations.AnimationManager;
import com.siberanka.interactiveholograms.api.animations.compile.AnimationCompiler;
import com.siberanka.interactiveholograms.display.attribute.AttributeCommandService;
import com.siberanka.interactiveholograms.display.attribute.AttributeConfigMapper;
import com.siberanka.interactiveholograms.display.attribute.DisplayAttributeService;
import com.siberanka.interactiveholograms.display.attribute.command.handler.AttributeCommandHandlerRegistry;
import com.siberanka.interactiveholograms.display.attribute.command.handler.DefaultBooleanHandler;
import com.siberanka.interactiveholograms.display.attribute.command.handler.DefaultBrightnessHandler;
import com.siberanka.interactiveholograms.display.attribute.command.handler.DefaultColorHandler;
import com.siberanka.interactiveholograms.display.attribute.command.handler.DefaultEnumHandler;
import com.siberanka.interactiveholograms.display.attribute.command.handler.DefaultLineWidthHandler;
import com.siberanka.interactiveholograms.display.attribute.command.handler.DefaultOpacityHandler;
import com.siberanka.interactiveholograms.display.attribute.command.handler.DefaultPitchHandler;
import com.siberanka.interactiveholograms.display.attribute.command.handler.DefaultScaleHandler;
import com.siberanka.interactiveholograms.display.attribute.command.handler.DefaultShadowRadiusHandler;
import com.siberanka.interactiveholograms.display.attribute.command.handler.DefaultShadowStrengthHandler;
import com.siberanka.interactiveholograms.display.attribute.command.handler.DefaultSkullTextureHandler;
import com.siberanka.interactiveholograms.display.attribute.command.handler.DefaultTranslationHandler;
import com.siberanka.interactiveholograms.display.attribute.command.handler.DefaultYawHandler;
import com.siberanka.interactiveholograms.display.attribute.command.handler.ChromaHandler;
import com.siberanka.interactiveholograms.display.attribute.defaults.AttributeDefaultRegistry;
import com.siberanka.interactiveholograms.display.attribute.defaults.AttributeDefaultRepository;
import com.siberanka.interactiveholograms.display.attribute.defaults.AttributeDefaultService;
import com.siberanka.interactiveholograms.display.attribute.definition.AttributeDefinitionRegistry;
import com.siberanka.interactiveholograms.display.attribute.definition.BillboardAttributeDefinition;
import com.siberanka.interactiveholograms.display.attribute.definition.BrightnessAttributeDefinition;
import com.siberanka.interactiveholograms.display.attribute.definition.GlowColorAttributeDefinition;
import com.siberanka.interactiveholograms.display.attribute.definition.ItemDisplayTypeAttributeDefinition;
import com.siberanka.interactiveholograms.display.attribute.definition.ItemEnchantedAttributeDefinition;
import com.siberanka.interactiveholograms.display.attribute.definition.ItemLeatherColorAttributeDefinition;
import com.siberanka.interactiveholograms.display.attribute.definition.ItemSkullTextureAttributeDefinition;
import com.siberanka.interactiveholograms.display.attribute.definition.PitchAttributeDefinition;
import com.siberanka.interactiveholograms.display.attribute.definition.ScaleAttributeDefinition;
import com.siberanka.interactiveholograms.display.attribute.definition.ShadowRadiusAttributeDefinition;
import com.siberanka.interactiveholograms.display.attribute.definition.ShadowStrengthAttributeDefinition;
import com.siberanka.interactiveholograms.display.attribute.definition.TextAlignmentAttributeDefinition;
import com.siberanka.interactiveholograms.display.attribute.definition.TextBackgroundColorAttributeDefinition;
import com.siberanka.interactiveholograms.display.attribute.definition.TextLineWidthAttributeDefinition;
import com.siberanka.interactiveholograms.display.attribute.definition.TextOpacityAttributeDefinition;
import com.siberanka.interactiveholograms.display.attribute.definition.TextSeeThroughAttributeDefinition;
import com.siberanka.interactiveholograms.display.attribute.definition.TextShadowAttributeDefinition;
import com.siberanka.interactiveholograms.display.attribute.definition.TranslationAttributeDefinition;
import com.siberanka.interactiveholograms.display.attribute.definition.YawAttributeDefinition;
import com.siberanka.interactiveholograms.display.attribute.value.AttributeValueSerializer;
import com.siberanka.interactiveholograms.display.attribute.value.AttributeValueTypeRegistry;
import com.siberanka.interactiveholograms.display.attribute.value.color.ChromaValueType;
import com.siberanka.interactiveholograms.display.attribute.value.color.RgbaValueType;
import com.siberanka.interactiveholograms.display.attribute.value.display.BillboardConstraintsValue;
import com.siberanka.interactiveholograms.display.attribute.value.display.BillboardConstraintsValueType;
import com.siberanka.interactiveholograms.display.attribute.value.display.BrightnessValueType;
import com.siberanka.interactiveholograms.display.attribute.value.display.ItemDisplayTypeValue;
import com.siberanka.interactiveholograms.display.attribute.value.display.ItemDisplayTypeValueType;
import com.siberanka.interactiveholograms.display.attribute.value.display.TextAlignmentValue;
import com.siberanka.interactiveholograms.display.attribute.value.display.TextAlignmentValueType;
import com.siberanka.interactiveholograms.display.attribute.value.primitives.BooleanValueType;
import com.siberanka.interactiveholograms.display.attribute.value.primitives.FloatValueType;
import com.siberanka.interactiveholograms.display.attribute.value.primitives.IntegerValueType;
import com.siberanka.interactiveholograms.display.attribute.value.primitives.StringValueFactory;
import com.siberanka.interactiveholograms.display.attribute.value.primitives.StringValueType;
import com.siberanka.interactiveholograms.display.attribute.value.primitives.Vector3fValueType;
import com.siberanka.interactiveholograms.display.command.DisplaysCommand;
import com.siberanka.interactiveholograms.display.config.DisplayConfigMapper;
import com.siberanka.interactiveholograms.display.config.DisplayPersistenceService;
import com.siberanka.interactiveholograms.display.config.DisplayRepository;
import com.siberanka.interactiveholograms.display.config.HologramImportService;
import com.siberanka.interactiveholograms.display.config.YamlConfigurationLoaderFactory;
import com.siberanka.interactiveholograms.display.config.dto.ConfigAttribute;
import com.siberanka.interactiveholograms.display.config.dto.ConfigDefaultAttribute;
import com.siberanka.interactiveholograms.display.config.serializer.ConfigAttributeSerializer;
import com.siberanka.interactiveholograms.display.config.serializer.ConfigDefaultAttributeSerializer;
import com.siberanka.interactiveholograms.display.config.serializer.DecentLocationSerializer;
import com.siberanka.interactiveholograms.display.config.serializer.DisplayBrightnessSerializer;
import com.siberanka.interactiveholograms.display.config.serializer.DisplayColorSerializer;
import com.siberanka.interactiveholograms.display.config.serializer.DisplayVector3fSerializer;
import com.siberanka.interactiveholograms.display.render.DisplayPostProcessor;
import com.siberanka.interactiveholograms.display.render.DisplayRenderCoordinator;
import com.siberanka.interactiveholograms.display.render.DisplayRenderIntentMaterializer;
import com.siberanka.interactiveholograms.display.render.DisplayRenderService;
import com.siberanka.interactiveholograms.display.render.DisplayVisibilityService;
import com.siberanka.interactiveholograms.display.interaction.DisplayInteractionService;
import com.siberanka.interactiveholograms.display.integration.ModelCatalogService;
import com.siberanka.interactiveholograms.display.integration.ModelDisplayService;
import com.siberanka.interactiveholograms.api.InteractiveHologramsAPI;
import com.siberanka.interactiveholograms.display.render.TextPostProcessor;
import com.siberanka.interactiveholograms.display.render.placeholder.DisplayPlaceholderService;
import com.siberanka.interactiveholograms.display.render.state.LogicalRenderStateManager;
import com.siberanka.interactiveholograms.display.render.state.LogicalRenderStateService;
import com.siberanka.interactiveholograms.display.render.state.PresentedRenderStateManager;
import com.siberanka.interactiveholograms.display.type.BlockDisplayTypeDefinition;
import com.siberanka.interactiveholograms.display.type.DisplayTypeRegistry;
import com.siberanka.interactiveholograms.display.type.ItemDisplayTypeDefinition;
import com.siberanka.interactiveholograms.display.type.TextDisplayTypeDefinition;
import com.siberanka.interactiveholograms.platform.api.PlatformAdapter;
import com.siberanka.interactiveholograms.platform.api.data.DecentColor;
import com.siberanka.interactiveholograms.platform.api.data.DecentLocation;
import com.siberanka.interactiveholograms.platform.api.data.DecentVector3f;
import com.siberanka.interactiveholograms.platform.api.data.display.DisplayBillboardConstraints;
import com.siberanka.interactiveholograms.platform.api.data.display.DisplayBrightness;
import com.siberanka.interactiveholograms.platform.api.data.display.DisplayType;
import com.siberanka.interactiveholograms.platform.api.data.display.ItemDisplayType;
import com.siberanka.interactiveholograms.platform.api.data.display.TextDisplayAlignment;
import com.siberanka.interactiveholograms.platform.api.player.PlatformPlayerService;
import com.siberanka.interactiveholograms.platform.bukkit.text.LegacyCachingBukkitTextFormatter;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

/**
 * Manages the display module, including initialization, reloading, and shutdown.
 *
 * @author d0by
 * @since 2.10.0
 */
public class DisplayModule {

    private final JavaPlugin plugin;
    private final DisplayService displayService;
    private final DisplayUpdateScheduler displayUpdateScheduler;
    private final DisplayListener displayListener;
    private final DisplaysCommand displaysCommand;
    private final AttributeDefaultService attributeDefaultService;
    private final LegacyCachingBukkitTextFormatter textFormatter;
    private final DisplayInteractionService interactionService;
    private final ModelDisplayService modelDisplayService;
    private final HologramImportService hologramImportService;

    public DisplayModule(JavaPlugin plugin, AnimationManager animationManager, PlatformAdapter platformAdapter) {
        this.plugin = plugin;
        DisplayVisibilityService visibilityService = new DisplayVisibilityService();
        DisplayRenderIntentMaterializer renderDiffService = new DisplayRenderIntentMaterializer();
        PresentedRenderStateManager renderStateManager = new PresentedRenderStateManager();
        DisplayPlaceholderService displayPlaceholderService = new DisplayPlaceholderService(platformAdapter);
        AnimationCompiler animationCompiler = new AnimationCompiler(animationManager);
        this.textFormatter = new LegacyCachingBukkitTextFormatter();
        DisplayTypeRegistry displayTypeRegistry = createDisplayTypeRegistry(displayPlaceholderService, animationCompiler);
        AttributeDefinitionRegistry attributeDefinitionRegistry = new AttributeDefinitionRegistry();
        TextPostProcessor textPostProcessor = new TextPostProcessor(animationManager, textFormatter);
        DisplayPostProcessor postProcessingService = new DisplayPostProcessor(attributeDefinitionRegistry, textPostProcessor);
        DisplayRenderService renderService = new DisplayRenderService(renderDiffService, platformAdapter.getRenderService(), renderStateManager, postProcessingService);
        LogicalRenderStateService stateService = new LogicalRenderStateService(displayTypeRegistry);
        LogicalRenderStateManager logicalRenderStateManager = new LogicalRenderStateManager();
        PlatformPlayerService playerService = platformAdapter.getPlayerService();
        DisplayRenderCoordinator renderCoordinator = new DisplayRenderCoordinator(
                visibilityService, playerService, stateService, renderService, logicalRenderStateManager);
        AttributeValueTypeRegistry attributeValueTypeRegistry = createAttributeValueTypeRegistry(displayPlaceholderService);
        AttributeValueSerializer attributeValueSerializer = new AttributeValueSerializer(attributeValueTypeRegistry);
        YamlConfigurationLoaderFactory yamlConfigurationLoaderFactory = new YamlConfigurationLoaderFactory(createTypeSerializers(attributeValueSerializer));
        DisplayRepository configService = new DisplayRepository(plugin.getDataFolder().toPath(), yamlConfigurationLoaderFactory);
        AttributeConfigMapper attributeConfigMapper = new AttributeConfigMapper(attributeDefinitionRegistry, attributeValueTypeRegistry);
        DisplayConfigMapper configMapper = new DisplayConfigMapper(attributeConfigMapper, platformAdapter.getMaterialService());
        DisplayPersistenceService persistenceService = new DisplayPersistenceService(configService, configMapper);
        DisplayCloneService displayCloneService = new DisplayCloneService();
        this.displayService = new DisplayService(persistenceService, renderCoordinator, platformAdapter.getEventListener());
        this.displayListener = new DisplayListener(displayService, playerService);
        AttributeCommandHandlerRegistry commandHandlerRegistry = createCommandHandlerRegistry(displayPlaceholderService);
        AttributeDefaultRegistry attributeDefaultRegistry = new AttributeDefaultRegistry();
        AttributeDefaultRepository attributeDefaultRepository = new AttributeDefaultRepository(
                yamlConfigurationLoaderFactory, platformAdapter.getSaveResourceService(), attributeDefinitionRegistry, attributeValueTypeRegistry, plugin.getDataFolder().toPath());
        this.attributeDefaultService = new AttributeDefaultService(attributeDefaultRegistry, attributeDefinitionRegistry, attributeDefaultRepository);
        AttributeCommandService attributeCommandService = new AttributeCommandService(
                attributeDefinitionRegistry, commandHandlerRegistry, attributeDefaultService);
        DisplayAttributeService displayAttributeService = new DisplayAttributeService(attributeDefinitionRegistry);
        java.nio.file.Path serverRoot = resolveServerRoot(plugin);
        this.hologramImportService = new HologramImportService(serverRoot, plugin.getDataFolder().toPath());
        ModelCatalogService modelCatalog = new ModelCatalogService();
        this.displaysCommand = new DisplaysCommand(
                displayService, displayCloneService, attributeCommandService, attributeDefaultService, displayAttributeService,
                platformAdapter.getMaterialService(), hologramImportService, modelCatalog);
        this.displayUpdateScheduler = new DisplayUpdateScheduler(plugin, displayService, renderCoordinator);
        this.interactionService = new DisplayInteractionService(
                plugin,
                displayService,
                visibilityService,
                InteractiveHologramsAPI.get().getNmsAdapter().getHologramComponentFactory()
        );
        this.modelDisplayService = new ModelDisplayService(plugin, displayService);
    }

    private AttributeCommandHandlerRegistry createCommandHandlerRegistry(DisplayPlaceholderService placeholderService) {
        AttributeCommandHandlerRegistry registry = new AttributeCommandHandlerRegistry();
        registry.register(BillboardAttributeDefinition.KEY, new DefaultEnumHandler<>(DisplayBillboardConstraints.class, BillboardConstraintsValue::new));
        registry.register(BrightnessAttributeDefinition.KEY, new DefaultBrightnessHandler());
        DefaultColorHandler defaultColorHandler = new DefaultColorHandler();
        ChromaHandler chromaHandler = new ChromaHandler();
        registry.register(GlowColorAttributeDefinition.KEY, defaultColorHandler, chromaHandler);
        registry.register(ItemDisplayTypeAttributeDefinition.KEY, new DefaultEnumHandler<>(ItemDisplayType.class, ItemDisplayTypeValue::new));
        registry.register(ItemEnchantedAttributeDefinition.KEY, new DefaultBooleanHandler());
        registry.register(ItemLeatherColorAttributeDefinition.KEY, defaultColorHandler, chromaHandler);
        registry.register(ItemSkullTextureAttributeDefinition.KEY, new DefaultSkullTextureHandler(new StringValueFactory(placeholderService)));
        registry.register(PitchAttributeDefinition.KEY, new DefaultPitchHandler());
        registry.register(ScaleAttributeDefinition.KEY, new DefaultScaleHandler());
        registry.register(ShadowRadiusAttributeDefinition.KEY, new DefaultShadowRadiusHandler());
        registry.register(ShadowStrengthAttributeDefinition.KEY, new DefaultShadowStrengthHandler());
        registry.register(TextAlignmentAttributeDefinition.KEY, new DefaultEnumHandler<>(TextDisplayAlignment.class, TextAlignmentValue::new));
        registry.register(TextBackgroundColorAttributeDefinition.KEY, defaultColorHandler, chromaHandler);
        registry.register(TextLineWidthAttributeDefinition.KEY, new DefaultLineWidthHandler());
        registry.register(TextOpacityAttributeDefinition.KEY, new DefaultOpacityHandler());
        registry.register(TextSeeThroughAttributeDefinition.KEY, new DefaultBooleanHandler());
        registry.register(TextShadowAttributeDefinition.KEY, new DefaultBooleanHandler());
        registry.register(TranslationAttributeDefinition.KEY, new DefaultTranslationHandler());
        registry.register(YawAttributeDefinition.KEY, new DefaultYawHandler());
        return registry;
    }

    private AttributeValueTypeRegistry createAttributeValueTypeRegistry(DisplayPlaceholderService placeholderService) {
        AttributeValueTypeRegistry registry = new AttributeValueTypeRegistry();
        registry.register(new BooleanValueType());
        registry.register(new FloatValueType());
        registry.register(new IntegerValueType());
        registry.register(new StringValueType(new StringValueFactory(placeholderService)));
        registry.register(new Vector3fValueType());
        registry.register(new BillboardConstraintsValueType());
        registry.register(new BrightnessValueType());
        registry.register(new ItemDisplayTypeValueType());
        registry.register(new TextAlignmentValueType());
        registry.register(new RgbaValueType());
        registry.register(new ChromaValueType());
        return registry;
    }

    private TypeSerializerCollection createTypeSerializers(AttributeValueSerializer attributeValueSerializer) {
        return TypeSerializerCollection.builder()
                .register(DecentLocation.class, new DecentLocationSerializer())
                .register(DecentVector3f.class, new DisplayVector3fSerializer())
                .register(DecentColor.class, new DisplayColorSerializer())
                .register(DisplayBrightness.class, new DisplayBrightnessSerializer())
                .register(ConfigAttribute.class, new ConfigAttributeSerializer(attributeValueSerializer))
                .register(ConfigDefaultAttribute.class, new ConfigDefaultAttributeSerializer(attributeValueSerializer))
                .build();
    }

    private DisplayTypeRegistry createDisplayTypeRegistry(DisplayPlaceholderService displayPlaceholderService,
                                                          AnimationCompiler animationCompiler) {
        DisplayTypeRegistry registry = new DisplayTypeRegistry();
        registry.registerDisplayType(DisplayType.TEXT, initializeTextDisplayType(displayPlaceholderService, animationCompiler));
        registry.registerDisplayType(DisplayType.ITEM, new ItemDisplayTypeDefinition());
        registry.registerDisplayType(DisplayType.BLOCK, new BlockDisplayTypeDefinition());
        return registry;
    }

    private TextDisplayTypeDefinition initializeTextDisplayType(DisplayPlaceholderService displayPlaceholderService,
                                                                AnimationCompiler animationCompiler) {
        return new TextDisplayTypeDefinition(displayPlaceholderService, animationCompiler);
    }

    public void initialize() {
        java.io.File example = new java.io.File(plugin.getDataFolder(), "hologram-example.yml");
        if (!example.exists()) {
            plugin.saveResource("hologram-example.yml", false);
        }
        this.attributeDefaultService.reload();
        this.displayService.reload();
        this.displayUpdateScheduler.start();
        this.interactionService.start();
        this.modelDisplayService.start();
        Bukkit.getPluginManager().registerEvents(displayListener, plugin);
    }

    public void reload() {
        this.attributeDefaultService.reload();
        this.displayService.reload();
    }

    public void shutdown() {
        HandlerList.unregisterAll(displayListener);
        this.interactionService.shutdown();
        this.modelDisplayService.shutdown();
        this.displayUpdateScheduler.shutdown();
        this.displayService.shutdown();
        this.attributeDefaultService.shutdown();
        this.textFormatter.invalidate();
    }

    public DisplayService getDisplayService() {
        return displayService;
    }

    private static java.nio.file.Path resolveServerRoot(JavaPlugin plugin) {
        java.nio.file.Path pluginsDirectory = plugin.getDataFolder().toPath().toAbsolutePath().getParent();
        return pluginsDirectory == null || pluginsDirectory.getParent() == null
                ? java.nio.file.Paths.get(".").toAbsolutePath() : pluginsDirectory.getParent();
    }

    public DisplayInteractionService getInteractionService() {
        return interactionService;
    }

    public DisplaysCommand getDisplaysCommand() {
        return displaysCommand;
    }

    public HologramImportService getHologramImportService() {
        return hologramImportService;
    }
}

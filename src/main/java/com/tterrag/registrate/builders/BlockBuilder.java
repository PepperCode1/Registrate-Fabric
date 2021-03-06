package com.tterrag.registrate.builders;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.MaterialColor;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.BlockItem;
import net.minecraft.tag.Tag.Identified;
import net.minecraft.util.DyeColor;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.fabric.EnvExecutor;
import com.tterrag.registrate.fabric.RegistryObject;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullBiFunction;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;

/**
 * A builder for blocks, allows for customization of the {@link FabricBlockSettings}, creation of block items, and configuration of data associated with blocks (loot tables, recipes, etc.).
 * 
 * @param <T>
 *            The type of block being built
 * @param <P>
 *            Parent object type
 */
public class BlockBuilder<T extends Block, P> extends AbstractBuilder<Block, T, P, BlockBuilder<T, P>> {

    /**
     * Create a new {@link BlockBuilder} and configure data. Used in lieu of adding side-effects to constructor, so that alternate initialization strategies can be done in subclasses.
     * <p>
     * The block will be assigned the following data:
     * <ul>
     * <li>A default blockstate file mapping all states to one model (via {@link #defaultBlockstate()})</li>
     * <li>A simple cube_all model (used in the blockstate) with one texture (via {@link #defaultBlockstate()})</li>
     * <li>A self-dropping loot table (via {@link #defaultLoot()})</li>
     * <li>The default translation (via {@link #defaultLang()})</li>
     * </ul>
     * 
     * @param <T>
     *            The type of the builder
     * @param <P>
     *            Parent object type
     * @param owner
     *            The owning {@link AbstractRegistrate} object
     * @param parent
     *            The parent object
     * @param name
     *            Name of the entry being built
     * @param callback
     *            A callback used to actually register the built entry
     * @param factory
     *            Factory to create the block
     * @param material
     *            The {@link Material} to use for the initial {@link FabricBlockSettings} object
     * @return A new {@link BlockBuilder} with reasonable default data generators.
     */
    public static <T extends Block, P> BlockBuilder<T, P> create(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, NonNullFunction<FabricBlockSettings, T> factory, Material material) {
        return new BlockBuilder<>(owner, parent, name, callback, factory, () -> FabricBlockSettings.of(material))
                /*.defaultBlockstate().defaultLoot().defaultLang()*/;
    }

    private final NonNullFunction<FabricBlockSettings, T> factory;
    
    private NonNullSupplier<FabricBlockSettings> initialProperties;
    private NonNullFunction<FabricBlockSettings, FabricBlockSettings> propertiesCallback = NonNullUnaryOperator.identity();
    private List<Supplier<Supplier<RenderLayer>>> renderLayers = new ArrayList<>(1);
    
    @Nullable
    private NonNullSupplier<Supplier<BlockColorProvider>> colorHandler;

    protected BlockBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, NonNullFunction<FabricBlockSettings, T> factory, NonNullSupplier<FabricBlockSettings> initialProperties) {
        super(owner, parent, name, callback, Block.class);
        this.factory = factory;
        this.initialProperties = initialProperties;
    }

    /**
     * Modify the properties of the block. Modifications are done lazily, but the passed function is composed with the current one, and as such this method can be called multiple times to perform
     * different operations.
     * <p>
     * If a different properties instance is returned, it will replace the existing one entirely.
     * 
     * @param func
     *            The action to perform on the properties
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> properties(NonNullUnaryOperator<FabricBlockSettings> func) {
        propertiesCallback = propertiesCallback.andThen(func);
        return this;
    }

    /**
     * Replace the initial state of the block properties, without replacing or removing any modifications done via {@link #properties(NonNullUnaryOperator)}.
     *
     * @param material
     *            The material of the initial properties
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> initialProperties(Material material) {
        initialProperties = () -> FabricBlockSettings.of(material);
        return this;
    }

    /**
     * Replace the initial state of the block properties, without replacing or removing any modifications done via {@link #properties(NonNullUnaryOperator)}.
     * 
     * @param material
     *            The material of the initial properties
     * @param color
     *            The color of the intial properties
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> initialProperties(Material material, DyeColor color) {
        initialProperties = () -> FabricBlockSettings.of(material, color);
        return this;
    }

    /**
     * Replace the initial state of the block properties, without replacing or removing any modifications done via {@link #properties(NonNullUnaryOperator)}.
     * 
     * @param material
     *            The material of the initial properties
     * @param color
     *            The color of the intial properties
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> initialProperties(Material material, MaterialColor color) {
        initialProperties = () -> FabricBlockSettings.of(material, color);
        return this;
    }

    /**
     * Replace the initial state of the block properties, without replacing or removing any modifications done via {@link #properties(NonNullUnaryOperator)}.
     * 
     * @param block
     *            The block to create the initial properties from (via {@link FabricBlockSettings#copy(AbstractBlock)})
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> initialProperties(NonNullSupplier<? extends Block> block) {
        initialProperties = () -> FabricBlockSettings.copyOf(block.get());
        return this;
    }

    public BlockBuilder<T, P> addLayer(Supplier<Supplier<RenderLayer>> layer) {
        EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
            Preconditions.checkArgument(RenderLayer.getBlockLayers().contains(layer.get().get()), "Invalid block layer: " + layer);
        });
        if (this.renderLayers.isEmpty()) {
            onRegister(this::registerLayers);
        }
        this.renderLayers.add(layer);
        return this;
    }

    protected void registerLayers(T entry) {
        EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
//            if (renderLayers.size() == 1) {
                final RenderLayer layer = renderLayers.get(0).get().get();
                BlockRenderLayerMap.INSTANCE.putBlock(entry, layer);
//            } else if (renderLayers.size() > 1) {
//                final Set<RenderLayer> layers = renderLayers.stream()
//                        .map(s -> s.get().get())
//                        .collect(Collectors.toSet());
//                RenderLayers.setRenderLayer(entry, layers::contains);
//            }
        });
    }

    /**
     * Create a standard {@link BlockItem} for this block, building it immediately, and not allowing for further configuration.
     * <p>
     * The item will have no lang entry (since it would duplicate the block's) and a simple block item model (via {@link RegistrateItemModelProvider#blockItem(NonNullSupplier)}).
     *
     * @return this {@link BlockBuilder}
     * @see #item()
     */
    public BlockBuilder<T, P> simpleItem() {
        return item().build();
    }

    /**
     * Create a standard {@link BlockItem} for this block, and return the builder for it so that further customization can be done.
     * <p>
     * The item will have no lang entry (since it would duplicate the block's) and a simple block item model (via {@link RegistrateItemModelProvider#blockItem(NonNullSupplier)}).
     * 
     * @return the {@link ItemBuilder} for the {@link BlockItem}
     */
    public ItemBuilder<BlockItem, BlockBuilder<T, P>> item() {
        return item(BlockItem::new);
    }

    /**
     * Create a {@link BlockItem} for this block, which is created by the given factory, and return the builder for it so that further customization can be done.
     * <p>
     * By default, the item will have no lang entry (since it would duplicate the block's) and a simple block item model (via {@link RegistrateItemModelProvider#blockItem(NonNullSupplier)}).
     * 
     * @param <I>
     *            The type of the item
     * @param factory
     *            A factory for the item, which accepts the block object and properties and returns a new item
     * @return the {@link ItemBuilder} for the {@link BlockItem}
     */
    public <I extends BlockItem> ItemBuilder<I, BlockBuilder<T, P>> item(NonNullBiFunction<? super T, FabricItemSettings, ? extends I> factory) {
        return getOwner().<I, BlockBuilder<T, P>> item(this, getName(), p -> factory.apply(getEntry(), p))
                /*.setData(ProviderType.LANG, NonNullBiConsumer.noop()) // FIXME Need a better API for "unsetting" providers
                .model((ctx, prov) -> {
                    Optional<String> model = getOwner().getDataProvider(ProviderType.BLOCKSTATE)
                            .flatMap(p -> p.getExistingVariantBuilder(getEntry()))
                            .map(b -> b.getModels().get(b.partialState()))
                            .map(ConfiguredModelList::toJSON)
                            .filter(JsonElement::isJsonObject)
                            .map(j -> j.getAsJsonObject().get("model"))
                            .map(JsonElement::getAsString);
                    if (model.isPresent()) {
                        prov.withExistingParent(ctx.getName(), model.get());
                    } else {
                        prov.blockItem(asSupplier());
                    }
                })*/;
    }
    
    /**
     * Create a {@link BlockEntity} for this block, which is created by the given factory, and assigned this block as its one and only valid block.
     * 
     * @param <TE>
     *            The type of the tile entity
     * @param factory
     *            A factory for the tile entity
     * @return this {@link BlockBuilder}
     * @deprecated Use {@link #simpleTileEntity(NonNullFunction)}
     */
    @Deprecated
    public <TE extends BlockEntity> BlockBuilder<T, P> simpleTileEntity(NonNullSupplier<? extends TE> factory) {
        return tileEntity(factory).build();
    }

    /**
     * Create a {@link BlockEntity} for this block, which is created by the given factory, and assigned this block as its one and only valid block.
     * 
     * @param <TE>
     *            The type of the tile entity
     * @param factory
     *            A factory for the tile entity
     * @return this {@link BlockBuilder}
     */
    public <TE extends BlockEntity> BlockBuilder<T, P> simpleTileEntity(NonNullFunction<BlockEntityType<TE>, ? extends TE> factory) {
        return tileEntity(factory).build();
    }

    /**
     * Create a {@link BlockEntity} for this block, which is created by the given factory, and assigned this block as its one and only valid block.
     * <p>
     * The created {@link TileEntityBuilder} is returned for further configuration.
     * 
     * @param <TE>
     *            The type of the tile entity
     * @param factory
     *            A factory for the tile entity
     * @return the {@link TileEntityBuilder}
     * @deprecated Use {@link #tileEntity(NonNullFunction)}
     */
    @Deprecated
    public <TE extends BlockEntity> TileEntityBuilder<TE, BlockBuilder<T, P>> tileEntity(NonNullSupplier<? extends TE> factory) {
        return getOwner().<TE, BlockBuilder<T, P>> tileEntity(this, getName(), factory).validBlock(asSupplier());
    }

    /**
     * Create a {@link BlockEntity} for this block, which is created by the given factory, and assigned this block as its one and only valid block.
     * <p>
     * The created {@link TileEntityBuilder} is returned for further configuration.
     * 
     * @param <TE>
     *            The type of the tile entity
     * @param factory
     *            A factory for the tile entity
     * @return the {@link TileEntityBuilder}
     */
    public <TE extends BlockEntity> TileEntityBuilder<TE, BlockBuilder<T, P>> tileEntity(NonNullFunction<BlockEntityType<TE>, ? extends TE> factory) {
        return getOwner().<TE, BlockBuilder<T, P>> tileEntity(this, getName(), factory).validBlock(asSupplier());
    }
    
    /**
     * Register a block color handler for this block. The {@link BlockColorProvider} instance can be shared across many blocks.
     * 
     * @param colorHandler
     *            The color handler to register for this block
     * @return this {@link BlockBuilder}
     */
    // TODO it might be worthwhile to abstract this more and add the capability to automatically copy to the item
    public BlockBuilder<T, P> color(NonNullSupplier<Supplier<BlockColorProvider>> colorHandler) {
        if (this.colorHandler == null) {
            EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::registerBlockColor);
        }
        this.colorHandler = colorHandler;
        return this;
    }
    
    protected void registerBlockColor() {
//        OneTimeEventReceiver.addModListener(ColorHandlerEvent.Block.class, e -> {
//            NonNullSupplier<Supplier<BlockColorProvider>> colorHandler = this.colorHandler;
//            if (colorHandler != null) {
//                e.getBlockColors().registerColorProvider(colorHandler.get().get(), getEntry());
//            }
//        });
        onRegister(entry -> {
            ColorProviderRegistry.BLOCK.register(colorHandler.get().get(), entry);
        });
    }

    /**
     * Assign the default blockstate, which maps all states to a single model file (via {@link RegistrateBlockstateProvider#simpleBlock(Block)}). This is the default, so it is generally not necessary
     * to call, unless for undoing previous changes.
     * 
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> defaultBlockstate() {
        return this/*blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry()))*/;
    }

    /**
     * Configure the blockstate/models for this block.
     * 
     * @param cons
     *            The callback which will be invoked during data generation.
     * @return this {@link BlockBuilder}
     * @see #setData(ProviderType, NonNullBiConsumer)
     */
//    public BlockBuilder<T, P> blockstate(NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> cons) {
//        return setData(ProviderType.BLOCKSTATE, cons);
//    }

    /**
     * Assign the default translation, as specified by {@link RegistrateLangProvider#getAutomaticName(NonNullSupplier)}. This is the default, so it is generally not necessary to call, unless for undoing
     * previous changes.
     * 
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> defaultLang() {
        return lang(Block::getTranslationKey);
    }

    /**
     * Set the translation for this block.
     * 
     * @param name
     *            A localized English name
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> lang(String name) {
        return lang(Block::getTranslationKey, name);
    }

    /**
     * Assign the default loot table, as specified by {@link RegistrateBlockLootTables#addDrop(Block)}. This is the default, so it is generally not necessary to call, unless for
     * undoing previous changes.
     * 
     * @return this {@link BlockBuilder}
     */
    public BlockBuilder<T, P> defaultLoot() {
        return this/*loot(RegistrateBlockLootTables::addDrop)*/;
    }

    /**
     * Configure the loot table for this block. This is different than most data gen callbacks as the callback does not accept a {@link DataGenContext}, but instead a
     * {@link RegistrateBlockLootTables}, for creating specifically block loot tables.
     * <p>
     * If the block does not have a loot table (i.e. {@link FabricBlockSettings#dropsNothing()} is called) this action will be <em>skipped</em>.
     * 
     * @param cons
     *            The callback which will be invoked during block loot table creation.
     * @return this {@link BlockBuilder}
     */
//    public BlockBuilder<T, P> loot(NonNullBiConsumer<RegistrateBlockLootTables, T> cons) {
//        return setData(ProviderType.LOOT, (ctx, prov) -> prov.addLootAction(LootType.BLOCK, tb -> {
//            if (!ctx.getEntry().getLootTableId().equals(LootTables.EMPTY)) {
//                cons.accept(tb, ctx.getEntry());
//            }
//        }));
//    }

    /**
     * Configure the recipe(s) for this block.
     * 
     * @param cons
     *            The callback which will be invoked during data generation.
     * @return this {@link BlockBuilder}
     * @see #setData(ProviderType, NonNullBiConsumer)
     */
//    public BlockBuilder<T, P> recipe(NonNullBiConsumer<DataGenContext<Block, T>, RegistrateRecipeProvider> cons) {
//        return setData(ProviderType.RECIPE, cons);
//    }

    /**
     * Assign {@link Identified}{@code s} to this block. Multiple calls will add additional tags.
     * 
     * @param tags
     *            The tags to assign
     * @return this {@link BlockBuilder}
     */
    @SafeVarargs
    public final BlockBuilder<T, P> tag(Identified<Block>... tags) {
        return this/*tag(ProviderType.BLOCK_TAGS, tags)*/;
    }

    @Override
    protected T createEntry() {
        @NotNull FabricBlockSettings properties = this.initialProperties.get();
        properties = propertiesCallback.apply(properties);
        return factory.apply(properties);
    }
    
    @Override
    protected RegistryEntry<T> createEntryWrapper(RegistryObject<T> delegate) {
        return new BlockEntry<>(getOwner(), delegate);
    }
    
    @Override
    public BlockEntry<T> register() {
        return (BlockEntry<T>) super.register();
    }
}

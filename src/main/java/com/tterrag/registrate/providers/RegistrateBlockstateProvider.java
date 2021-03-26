//package com.tterrag.registrate.providers;
//
//import java.util.Optional;
//
//import com.tterrag.registrate.AbstractRegistrate;
//
//import net.fabricmc.api.EnvType;
//import net.minecraft.block.Block;
//import net.minecraft.data.DataGenerator;
//import net.minecraft.world.gen.stateprovider.BlockStateProvider;
//import net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder;
//import net.minecraftforge.client.model.generators.VariantBlockStateBuilder;
//import net.minecraftforge.common.data.ExistingFileHelper;
//
//public class RegistrateBlockstateProvider extends BlockStateProvider implements RegistrateProvider {
//
//    private final AbstractRegistrate<?> parent;
//
//    public RegistrateBlockstateProvider(AbstractRegistrate<?> parent, DataGenerator gen, ExistingFileHelper exFileHelper) {
//        super(gen, parent.getModid(), exFileHelper);
//        this.parent = parent;
//    }
//
//    @Override
//    public EnvType getSide() {
//        return EnvType.CLIENT;
//    }
//
//    @Override
//    protected void registerStatesAndModels() {
//        parent.genData(ProviderType.BLOCKSTATE, this);
//    }
//    
//    @Override
//    public String getName() {
//        return "Blockstates";
//    }
//    
//    ExistingFileHelper getExistingFileHelper() {
//        return this.models().existingFileHelper;
//    }
//    
//    @SuppressWarnings("null")
//    public Optional<VariantBlockStateBuilder> getExistingVariantBuilder(Block block) {
//        return Optional.ofNullable(registeredBlocks.get(block))
//                .filter(b -> b instanceof VariantBlockStateBuilder)
//                .map(b -> (VariantBlockStateBuilder) b);
//    }
//    
//    @SuppressWarnings("null")
//    public Optional<MultiPartBlockStateBuilder> getExistingMultipartBuilder(Block block) {
//        return Optional.ofNullable(registeredBlocks.get(block))
//                .filter(b -> b instanceof MultiPartBlockStateBuilder)
//                .map(b -> (MultiPartBlockStateBuilder) b);
//    }
//}

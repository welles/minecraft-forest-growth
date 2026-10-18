package dev.welles.forestgrowth;

//? if fabric {
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class ForestGrowthFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ForestGrowth.init(FabricLoader.getInstance().getConfigDir());
    }
}
//?}

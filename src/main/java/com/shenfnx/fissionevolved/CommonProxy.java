package com.shenfnx.fissionevolved;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronize(event.getSuggestedConfigurationFile());
        SuperFuelRods.register();
        FissionStructureBlocks.register();
        FissionMachines.register();
        FissionEvolved.LOG.info("{} (version {})", Config.greeting, Tags.VERSION);
    }

    public void init(FMLInitializationEvent event) {
        SuperFuelRods.registerRecipes();
        FissionStructureBlocks.registerRecipes();
        FissionMachines.registerRecipes();
    }

    public void postInit(FMLPostInitializationEvent event) {
        FissionReactorRecipes.register();
        ColossalFissionReactorRecipes.register();
    }

    public void loadComplete(FMLLoadCompleteEvent event) {
        FissionReactorRecipes.scheduleLateOptionalFuelRegistration();
    }

    public void serverStarting(FMLServerStartingEvent event) {}
}

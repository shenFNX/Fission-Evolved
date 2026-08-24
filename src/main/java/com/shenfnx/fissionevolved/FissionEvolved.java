package com.shenfnx.fissionevolved;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
    modid = FissionEvolved.MOD_ID,
    version = Tags.VERSION,
    name = FissionEvolved.MOD_NAME,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:gregtech")
public final class FissionEvolved {

    public static final String MOD_ID = "fissionevolved";
    public static final String MOD_NAME = "Fission Evolved";
    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    @SidedProxy(
        clientSide = "com.shenfnx.fissionevolved.ClientProxy",
        serverSide = "com.shenfnx.fissionevolved.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        proxy.loadComplete(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }
}

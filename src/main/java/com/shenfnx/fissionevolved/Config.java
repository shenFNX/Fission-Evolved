package com.shenfnx.fissionevolved;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public final class Config {

    public static String greeting = "Fission Evolved initialized";
    public static int fissionReactorControllerId = 32100;
    public static int colossalFissionReactorControllerId = 32101;
    public static boolean enableAnimatedController = true;
    public static boolean enableReactorParticles = true;
    public static boolean enableReactorSounds = true;

    private Config() {}

    public static void synchronize(File configFile) {
        Configuration configuration = new Configuration(configFile);
        greeting = configuration.getString(
            "greeting",
            Configuration.CATEGORY_GENERAL,
            greeting,
            "Message written to the log during pre-initialization.");
        fissionReactorControllerId = configuration.getInt(
            "fissionReactorControllerId",
            "machine_ids",
            fissionReactorControllerId,
            2049,
            32766,
            "GregTech MetaTileEntity ID for the Fission Reactor Controller.");
        colossalFissionReactorControllerId = configuration.getInt(
            "colossalFissionReactorControllerId",
            "machine_ids",
            colossalFissionReactorControllerId,
            2049,
            32766,
            "GregTech MetaTileEntity ID for the Ultimate Universe-Destroying Generator Controller.");
        if (colossalFissionReactorControllerId == fissionReactorControllerId) {
            int conflictingId = colossalFissionReactorControllerId;
            colossalFissionReactorControllerId = fissionReactorControllerId == 32766 ? 32765
                : fissionReactorControllerId + 1;
            configuration.get("machine_ids", "colossalFissionReactorControllerId", colossalFissionReactorControllerId)
                .set(colossalFissionReactorControllerId);
            FissionEvolved.LOG.warn(
                "Colossal reactor MetaTileEntity ID {} conflicts with the existing 7x7x7 reactor; reassigned only "
                    + "the new controller to {}",
                conflictingId,
                colossalFissionReactorControllerId);
        }
        enableAnimatedController = configuration.getBoolean(
            "enableAnimatedController",
            "client",
            enableAnimatedController,
            "Use the animated high-resolution front overlay while the fission reactor is active.");
        enableReactorParticles = configuration.getBoolean(
            "enableReactorParticles",
            "client",
            enableReactorParticles,
            "Show sparse electrical particles around an active fission reactor controller.");
        enableReactorSounds = configuration.getBoolean(
            "enableReactorSounds",
            "client",
            enableReactorSounds,
            "Play the low reactor activity loop while the fission reactor is active.");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}

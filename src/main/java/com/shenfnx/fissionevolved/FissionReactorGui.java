package com.shenfnx.fissionevolved;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/** Player-facing status and wireless controls for the fission reactor. */
public final class FissionReactorGui extends MTEMultiBlockBaseGui<MTEFissionReactor> {

    public FissionReactorGui(MTEFissionReactor reactor) {
        super(reactor);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue(
            "fissionWirelessMode",
            new BooleanSyncValue(multiblock::getWirelessMode, multiblock::setWirelessEnabled).allowC2S());
        syncManager.syncValue("fissionCoolant", new StringSyncValue(multiblock::getActiveCoolantKey));
        syncManager.syncValue("fissionCoolantMultiplier", new IntSyncValue(multiblock::getCoolantMultiplierPercent));
        syncManager.syncValue("fissionEnergyBuffer", new StringSyncValue(multiblock::getEnergyBufferText));
        syncManager.syncValue("fissionMaxParallel", new IntSyncValue(multiblock::getTrueParallel));
        syncManager.syncValue("fissionActiveParallel", new IntSyncValue(multiblock::getActiveFuelParallel));
        syncManager.syncValue("fissionGeneration", new StringSyncValue(multiblock::getCurrentGenerationText));
        syncManager.syncValue("fissionActiveFuel", new StringSyncValue(multiblock::getActiveFuelNameKey));
        syncManager.syncValue("fissionRemainingTicks", new IntSyncValue(multiblock::getRemainingTicks));
        syncManager
            .syncValue("fissionEstimatedRunGeneration", new StringSyncValue(multiblock::getEstimatedRunGenerationText));
        syncManager.syncValue("fissionPauseReason", new StringSyncValue(multiblock::getPauseReasonKey));
        syncManager.syncValue("fissionDynamoCount", new IntSyncValue(multiblock::getDynamoOutputCount));
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        BooleanSyncValue wireless = syncManager.findSyncHandler("fissionWirelessMode", BooleanSyncValue.class);
        StringSyncValue coolant = syncManager.findSyncHandler("fissionCoolant", StringSyncValue.class);
        IntSyncValue coolantMultiplier = syncManager.findSyncHandler("fissionCoolantMultiplier", IntSyncValue.class);
        StringSyncValue energyBuffer = syncManager.findSyncHandler("fissionEnergyBuffer", StringSyncValue.class);
        IntSyncValue maxParallel = syncManager.findSyncHandler("fissionMaxParallel", IntSyncValue.class);
        IntSyncValue activeParallel = syncManager.findSyncHandler("fissionActiveParallel", IntSyncValue.class);
        StringSyncValue generation = syncManager.findSyncHandler("fissionGeneration", StringSyncValue.class);
        StringSyncValue activeFuel = syncManager.findSyncHandler("fissionActiveFuel", StringSyncValue.class);
        IntSyncValue remainingTicks = syncManager.findSyncHandler("fissionRemainingTicks", IntSyncValue.class);
        StringSyncValue estimatedRunGeneration = syncManager
            .findSyncHandler("fissionEstimatedRunGeneration", StringSyncValue.class);
        StringSyncValue pauseReason = syncManager.findSyncHandler("fissionPauseReason", StringSyncValue.class);
        IntSyncValue dynamoCount = syncManager.findSyncHandler("fissionDynamoCount", IntSyncValue.class);

        ListWidget<IWidget, ?> status = super.createTerminalTextWidget(syncManager, parent)
            .child(
                statusText(
                    IKey.dynamic(
                        () -> StatCollector.translateToLocalFormatted(
                            "fissionevolved.reactor.gui.fuel",
                            StatCollector.translateToLocal(activeFuel.getValue()))),
                    0xFFAA55))
            .child(
                statusText(
                    IKey.dynamic(
                        () -> StatCollector.translateToLocalFormatted(
                            "fissionevolved.reactor.gui.remaining",
                            formatDuration(remainingTicks.getIntValue()))),
                    0xFFFF55))
            .child(
                statusText(
                    IKey.dynamic(
                        () -> StatCollector.translateToLocalFormatted(
                            "fissionevolved.reactor.gui.estimated_total",
                            estimatedRunGeneration.getValue())),
                    0xFF55FF));
        if (multiblock.usesCoolantBoost()) {
            status.child(
                statusText(
                    IKey.dynamic(
                        () -> StatCollector.translateToLocalFormatted(
                            "fissionevolved.reactor.gui.coolant",
                            StatCollector.translateToLocal(coolant.getValue()),
                            coolantMultiplier.getIntValue())),
                    0x55FFFF));
        }
        return status
            .child(
                statusText(
                    IKey.dynamic(
                        () -> StatCollector
                            .translateToLocalFormatted("fissionevolved.reactor.gui.buffer", energyBuffer.getValue())),
                    0xFFFF55))
            .child(
                statusText(
                    IKey.dynamic(
                        () -> StatCollector.translateToLocalFormatted(
                            "fissionevolved.reactor.gui.parallel",
                            activeParallel.getIntValue(),
                            maxParallel.getIntValue())),
                    0xFFAA00))
            .child(
                statusText(
                    IKey.dynamic(
                        () -> StatCollector
                            .translateToLocalFormatted("fissionevolved.reactor.gui.generation", generation.getValue())),
                    0x55FF55))
            .child(
                statusText(
                    IKey.dynamic(
                        () -> StatCollector.translateToLocalFormatted(
                            "fissionevolved.reactor.gui.output_target",
                            StatCollector.translateToLocalFormatted(
                                wireless.getBoolValue() ? "fissionevolved.reactor.output.wireless"
                                    : dynamoCount.getIntValue() > 0 ? "fissionevolved.reactor.output.dynamos"
                                        : "fissionevolved.reactor.output.none",
                                dynamoCount.getIntValue()))),
                    0x55FFFF))
            .child(
                statusText(
                    IKey.dynamic(
                        () -> StatCollector.translateToLocalFormatted(
                            "fissionevolved.reactor.gui.pause_reason",
                            StatCollector.translateToLocal(pauseReason.getValue()))),
                    0xFFFFFF));
    }

    private static IWidget statusText(IKey key, int color) {
        return new TextWidget<>(key).color(color);
    }

    private static String formatDuration(int ticks) {
        int totalSeconds = Math.max(0, (ticks + 19) / 20);
        int hours = totalSeconds / 3_600;
        int minutes = totalSeconds / 60 % 60;
        int seconds = totalSeconds % 60;
        return hours > 0 ? String.format("%d:%02d:%02d", hours, minutes, seconds)
            : String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        Flow row = super.createLeftPanelGapRow(parent, syncManager);
        if (multiblock instanceof MTEColossalFissionReactor) {
            return row;
        }
        BooleanSyncValue wireless = syncManager.findSyncHandler("fissionWirelessMode", BooleanSyncValue.class);
        return row.child(
            new ButtonWidget<>().size(18, 18)
                .playClickSound(true)
                .overlay(
                    new DynamicDrawable(
                        () -> wireless.getBoolValue() ? GTGuiTextures.OVERLAY_BUTTON_WIRELESS_ON
                            : GTGuiTextures.OVERLAY_BUTTON_WIRELESS_OFF))
                .onMousePressed(mouseButton -> {
                    wireless.setBoolValue(!wireless.getBoolValue());
                    return true;
                })
                .tooltipBuilder(
                    tooltip -> tooltip.addLine(IKey.lang("fissionevolved.reactor.gui.wireless.tooltip"))
                        .addLine(
                            IKey.dynamic(
                                () -> StatCollector.translateToLocal(
                                    wireless.getBoolValue() ? "fissionevolved.reactor.wireless.enabled"
                                        : "fissionevolved.reactor.wireless.disabled"))))
                .tooltipShowUpTimer(TOOLTIP_DELAY));
    }
}

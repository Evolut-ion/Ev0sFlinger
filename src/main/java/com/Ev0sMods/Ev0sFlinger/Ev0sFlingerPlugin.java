package com.Ev0sMods.Ev0sFlinger;

import com.Ev0sMods.Ev0sFlinger.BlockStates.FlingerBlockState;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class Ev0sFlingerPlugin extends JavaPlugin {
    private static Ev0sFlingerPlugin instance;

    public Ev0sFlingerPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        this.getLogger().atInfo().log("Initialized");
        System.out.println("[Ev0sFlingerPlugin] Plugin loaded!");
    }
    public static Ev0sFlingerPlugin getInstance() {
        return instance;
    }


    public void onDisable() {
        System.out.println("[Ev0sFlingerPlugin] Plugin disabled!");
        
        // TODO: Cleanup your plugin here
        // - Save data
        // - Stop services
        // - Close connections
    }
    protected void start() {
        this.getLogger().at(Level.INFO).log("[TemplatePlugin] Plugin enabled!");
    }

    public void shutdown() {
        this.getLogger().at(Level.INFO).log("[TemplatePlugin] Plugin disabled!");
    }

    @Override
    protected void setup() {
        getBlockStateRegistry().registerBlockState(FlingerBlockState.class, "Ev0sFlinger", FlingerBlockState.CODEC, FlingerBlockState.Data.class, FlingerBlockState.Data.CODEC);
    }
}

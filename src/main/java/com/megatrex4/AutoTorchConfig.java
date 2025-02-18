package com.megatrex4;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.*;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.util.List;

@Config(name="autotorch")
public class AutoTorchConfig implements ConfigData {

    @Comment("Minimum level when the torch is placed.")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 14)
    private int lightLevel = 4;

    @Comment("List of torches (Vanilla and Modded) that can be used.")
    private List<String> torches = List.of("minecraft:torch", "minecraft:soul_torch");

    public int getLightLevel() {
        return lightLevel;
    }

    public List<String> getTorches() {
        return torches;
    }
}

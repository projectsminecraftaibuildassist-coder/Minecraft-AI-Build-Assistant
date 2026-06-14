package com.example.ai.api;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;


public class WorldInfo {
    public String biome;
    public int groundLevel;
    public String[] nearbyMaterials;
    
    public WorldInfo(ServerWorld world, BlockPos origin, int radius) {
    }
}
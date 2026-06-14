package com.example.ai.api;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import com.example.ai.build.BuildTaskQueue;
import com.example.config.ForbiddenBlockRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MinecraftAI {
    private static ServerWorld world;
    private static BlockPos origin;
    private static float playerYaw;
    private static float playerPitch;
    
    public static void init(ServerWorld w, BlockPos o) {
        init(w, o, 0f, 0f);
    }

    public static void init(ServerWorld w, BlockPos o, float yaw, float pitch) {
        world = w;
        origin = o;
        playerYaw = yaw;
        playerPitch = pitch;
        System.out.println("[MinecraftAI] Initialized at " + o + " facing " + TerrainVision.compassFromYaw(yaw));
    }
    
    public static String getWorldInfo(Number radius) {
        return getWorldInfo(radius, 0f, 0f);
    }

    public static String getWorldInfo(Number radius, float yaw, float pitch) {
        int r = toInt(radius);
        if (world == null || origin == null) {
            return "World not initialized";
        }
        
        StringBuilder info = new StringBuilder();
        
        int surfaceRelativeY = getGroundLevel(0, 0);
        info.append("Player feet (relative 0,0,0) at world Y=").append(origin.getY()).append("\n");
        info.append("Ground surface at (0,0): relative Y=").append(surfaceRelativeY).append("\n");
        
        String biome = world.getBiome(origin)
            .getKey()
            .map(key -> key.getValue().toString())
            .orElse("unknown");
        info.append("Biome: ").append(biome).append("\n");
        
        Map<String, Integer> blockCount = new HashMap<>();
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = origin.getY() - 5; y <= origin.getY() + 5; y++) {
                    BlockPos pos = origin.add(x, y, z);
                    Block block = world.getBlockState(pos).getBlock();
                    String name = Registries.BLOCK.getId(block).toString();
                    blockCount.merge(name, 1, Integer::sum);
                }
            }
        }
        
        info.append("Available materials: ");
        String materials = blockCount.entrySet().stream()
            .filter(e -> e.getValue() > 5 && !e.getKey().contains("air"))
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.joining(", "));
        
        if (materials.isEmpty()) {
            info.append("stone, dirt, grass_block");
        } else {
            info.append(materials);
        }

        TerrainVision.appendReport(info, world, origin, yaw, pitch);
        
        return info.toString();
    }
    
    
    public static void placeBlock(Number x, Number y, Number z, String blockName) {
        if (world == null || ForbiddenBlockRegistry.isForbidden(blockName)) {
            return;
        }

        Block block = getBlock(blockName);
        BlockPos pos = origin.add(toInt(x), toInt(y), toInt(z));
        schedulePlace(pos, block.getDefaultState(), blockName);
    }

    public static void placeBlock(String blockName, Number x, Number y, Number z) {
        placeBlock(x, y, z, blockName);
    }
    
    public static void placeBox(Number x, Number y, Number z, Number w, Number h, Number l, String blockName) {
        int ix = toInt(x);
        int iy = toInt(y);
        int iz = toInt(z);
        int iw = Math.max(1, Math.abs(toInt(w)));
        int ih = Math.max(1, Math.abs(toInt(h)));
        int il = Math.max(1, Math.abs(toInt(l)));

        if (world == null || ForbiddenBlockRegistry.isForbidden(blockName)) {
            return;
        }

        Block block = getBlock(blockName);
        int count = 0;

        for (int dx = 0; dx < iw; dx++) {
            for (int dy = 0; dy < ih; dy++) {
                for (int dz = 0; dz < il; dz++) {
                    boolean isEdge = (dx == 0 || dx == iw - 1 ||
                                     dy == 0 || dy == ih - 1 ||
                                     dz == 0 || dz == il - 1);
                    if (isEdge) {
                        BlockPos pos = origin.add(ix + dx, iy + dy, iz + dz);
                        schedulePlace(pos, block.getDefaultState(), blockName);
                        count++;
                    }
                }
            }
        }

        System.out.println("[AI] Queued box: " + count + " blocks of " + blockName);
    }

    public static void placeBox(String blockName, Number x, Number y, Number z, Number w, Number h, Number l) {
        placeBox(x, y, z, w, h, l, blockName);
    }
    
    public static void placePillar(Number x, Number y, Number z, Number h, String blockName) {
        int ix = toInt(x);
        int iy = toInt(y);
        int iz = toInt(z);
        int ih = toInt(h);

        if (world == null || ForbiddenBlockRegistry.isForbidden(blockName)) {
            return;
        }

        Block block = getBlock(blockName);

        for (int dy = 0; dy < ih; dy++) {
            BlockPos pos = origin.add(ix, iy + dy, iz);
            schedulePlace(pos, block.getDefaultState(), blockName);
        }
    }

    public static void placePillar(String blockName, Number x, Number y, Number z, Number h) {
        placePillar(x, y, z, h, blockName);
    }

    public static void placeFloor(Number x, Number y, Number z, Number w, Number l, String blockName) {
        int ix = toInt(x);
        int iy = toInt(y);
        int iz = toInt(z);
        int iw = toInt(w);
        int il = toInt(l);

        if (world == null || ForbiddenBlockRegistry.isForbidden(blockName)) {
            return;
        }

        Block block = getBlock(blockName);

        for (int dx = 0; dx < iw; dx++) {
            for (int dz = 0; dz < il; dz++) {
                BlockPos pos = origin.add(ix + dx, iy, iz + dz);
                schedulePlace(pos, block.getDefaultState(), blockName);
            }
        }
    }

    public static void placeFloor(String blockName, Number x, Number y, Number z, Number w, Number l) {
        placeFloor(x, y, z, w, l, blockName);
    }

    public static void placeFloor(Number x, Number y, Number z, Number w, Number l) {
        placeFloor(x, y, z, w, l, "oak_planks");
    }
    
    public static void placeRoof(Number x, Number y, Number z, Number w, Number l, String blockName) {
        placeFloor(x, y, z, w, l, blockName);
    }

    public static void placeRoof(String blockName, Number x, Number y, Number z, Number w, Number l) {
        placeRoof(x, y, z, w, l, blockName);
    }

    public static void placeRoof(Number x, Number y, Number z, Number w, Number l) {
        placeRoof(x, y, z, w, l, "oak_planks");
    }
    
    public static void placeWall(Number x, Number y, Number z, Number h, Number l, String first, String second) {
        if (isWallDirection(first)) {
            placeWallInternal(x, y, z, h, l, second, first);
        } else {
            placeWallInternal(x, y, z, h, l, first, second);
        }
    }

    public static void placeWall(String blockName, Number x, Number y, Number z, Number h, Number l, String direction) {
        placeWallInternal(x, y, z, h, l, blockName, direction);
    }

    private static void placeWallInternal(Number x, Number y, Number z, Number h, Number l, String blockName, String direction) {
        int ix = toInt(x);
        int iy = toInt(y);
        int iz = toInt(z);
        int ih = toInt(h);
        int il = toInt(l);

        if (world == null || ForbiddenBlockRegistry.isForbidden(blockName)) {
            return;
        }

        Block block = getBlock(blockName);

        String axis = normalizeWallDirection(direction);
        if ("x".equals(axis)) {
            for (int dx = 0; dx < il; dx++) {
                for (int dy = 0; dy < ih; dy++) {
                    BlockPos pos = origin.add(ix + dx, iy + dy, iz);
                    schedulePlace(pos, block.getDefaultState(), blockName);
                }
            }
        } else if ("z".equals(axis)) {
            for (int dz = 0; dz < il; dz++) {
                for (int dy = 0; dy < ih; dy++) {
                    BlockPos pos = origin.add(ix, iy + dy, iz + dz);
                    schedulePlace(pos, block.getDefaultState(), blockName);
                }
            }
        }
    }

    private static boolean isWallDirection(String value) {
        if (value == null) {
            return false;
        }

        return switch (value.toLowerCase()) {
            case "north", "south", "east", "west", "x", "z" -> true;
            default -> false;
        };
    }

    private static String normalizeWallDirection(String direction) {
        if (direction == null) {
            return "";
        }

        return switch (direction.toLowerCase()) {
            case "x", "north", "south" -> "x";
            case "z", "east", "west" -> "z";
            default -> direction.toLowerCase();
        };
    }



    public static String getFacing() {
        return TerrainVision.compassFromYaw(playerYaw);
    }

    public static int getForwardX(Number distance) {
        return TerrainVision.forwardX(playerYaw, toInt(distance));
    }

    public static int getForwardZ(Number distance) {
        return TerrainVision.forwardZ(playerYaw, toInt(distance));
    }

    public static String inspectColumn(Number x, Number z) {
        if (world == null || origin == null) {
            return "uninitialized";
        }

        int ix = toInt(x);
        int iz = toInt(z);
        int groundY = getGroundLevel(ix, iz);
        int headroom = TerrainVision.countHeadroom(world, origin, ix, iz, groundY);
        boolean blocked = TerrainVision.isBlockedAtBodyHeight(world, origin, ix, iz, groundY);
        String blockAtFeet = getBlock(ix, 0, iz);
        String blockAboveGround = getBlock(ix, groundY + 1, iz);

        return "column(" + ix + "," + iz + ")"
            + " groundY=" + groundY
            + " headroom=" + headroom
            + " blocked=" + blocked
            + " feet=" + shortName(blockAtFeet)
            + " aboveGround=" + shortName(blockAboveGround);
    }

    public static String scanForward(Number maxDistance) {
        if (world == null || origin == null) {
            return "uninitialized";
        }

        int max = Math.clamp(toInt(maxDistance), 1, 16);
        StringBuilder report = new StringBuilder();
        report.append("forwardScan max=").append(max).append(" facing=").append(getFacing()).append('\n');

        for (int distance = 1; distance <= max; distance++) {
            int x = getForwardX(distance);
            int z = getForwardZ(distance);
            report.append(" d=").append(distance)
                .append(" -> ").append(inspectColumn(x, z))
                .append('\n');
        }

        return report.toString().trim();
    }

    public static String scanArea(Number x, Number z, Number width, Number length) {
        if (world == null || origin == null) {
            return "uninitialized";
        }

        int ix = toInt(x);
        int iz = toInt(z);
        int iw = Math.clamp(toInt(width), 1, 15);
        int il = Math.clamp(toInt(length), 1, 15);

        StringBuilder report = new StringBuilder();
        report.append("areaScan origin=(").append(ix).append(",").append(iz).append(")")
            .append(" size=").append(iw).append("x").append(il)
            .append(" (values=ground relative Y)\n");

        for (int dz = 0; dz < il; dz++) {
            for (int dx = 0; dx < iw; dx++) {
                report.append(getGroundLevel(ix + dx, iz + dz)).append(' ');
            }
            report.append('\n');
        }

        return report.toString().trim();
    }

    private static String shortName(String blockId) {
        int colon = blockId.indexOf(':');
        return colon >= 0 ? blockId.substring(colon + 1) : blockId;
    }
    
    public static boolean canPlace(Number x, Number y, Number z) {
        if (world == null) {
            return false;
        }

        BlockPos pos = origin.add(toInt(x), toInt(y), toInt(z));
        return world.getBlockState(pos).getBlock() == Blocks.AIR;
    }
    
    public static boolean isSolid(Number x, Number y, Number z) {
        if (world == null) {
            return false;
        }

        BlockPos pos = origin.add(toInt(x), toInt(y), toInt(z));
        return !world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
    }
    
    public static String getBlock(Number x, Number y, Number z) {
        if (world == null) {
            return "air";
        }

        BlockPos pos = origin.add(toInt(x), toInt(y), toInt(z));
        return Registries.BLOCK.getId(world.getBlockState(pos).getBlock()).toString();
    }
    
    public static int getGroundLevel(Number x, Number z) {
        int ix = toInt(x);
        int iz = toInt(z);

        if (world == null || origin == null) {
            return 0;
        }

        BlockPos column = origin.add(ix, 0, iz);
        int bottomY = world.getBottomY();

        for (int y = origin.getY(); y >= bottomY; y--) {
            BlockPos pos = column.withY(y);
            if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
                return y - origin.getY();
            }
        }

        return 0;
    }


    public static String listBlocks() {
        return listBlocks(0, 50);
    }

    public static String listBlocks(Number offset, Number limit) {
        return BlockCatalog.listPage(toInt(offset), toInt(limit));
    }

    public static String searchBlocks(String query) {
        return searchBlocks(query, 40);
    }

    public static String searchBlocks(String query, Number limit) {
        return BlockCatalog.search(query, toInt(limit));
    }

    public static int getBlockListCount() {
        return BlockCatalog.countAvailable();
    }

    public static boolean isBlockAllowed(String blockName) {
        return BlockCatalog.isAllowed(blockName);
    }

    public static void clearArea(Number x, Number y, Number z, Number w, Number l) {
        clearArea(x, y, z, w, 1, l);
    }

    public static void clearArea(Number x, Number y, Number z, Number w, Number h, Number l) {
        int ix = toInt(x);
        int iy = toInt(y);
        int iz = toInt(z);
        int iw = Math.max(1, Math.abs(toInt(w)));
        int ih = Math.max(1, Math.abs(toInt(h)));
        int il = Math.max(1, Math.abs(toInt(l)));

        if (world == null) {
            return;
        }

        for (int dx = 0; dx < iw; dx++) {
            for (int dy = 0; dy < ih; dy++) {
                for (int dz = 0; dz < il; dz++) {
                    BlockPos pos = origin.add(ix + dx, iy + dy, iz + dz);
                    schedulePlace(pos, Blocks.AIR.getDefaultState(), "minecraft:air");
                }
            }
        }
    }

    private static void schedulePlace(BlockPos pos, BlockState state, String blockName) {
        BuildTaskQueue.enqueue(() -> world.setBlockState(pos, state));
    }
    
    
    private static int toInt(Number value) {
        if (value == null) {
            return 0;
        }
        return value.intValue();
    }

    private static Block getBlock(String blockName) {
        if (!blockName.contains(":")) {
            blockName = "minecraft:" + blockName;
        }
        
        try {
            Block block = Registries.BLOCK.get(Identifier.of(blockName));
            if (block == Blocks.AIR) {
                return Blocks.AIR;
            }
            return block;
        } catch (Exception e) {
            System.out.println("[WARNING] Block not found: " + blockName + ", using STONE");
            return Blocks.STONE;
        }
    }
    
    public static void setOrigin(BlockPos newOrigin) {
        origin = newOrigin;
        System.out.println("[MinecraftAI] Origin set to " + origin);
    }
    
    public static BlockPos getOrigin() {
        return origin;
    }
}
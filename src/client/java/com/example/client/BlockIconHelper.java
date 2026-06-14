package com.example.client;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BlockIconHelper {

    private static List<String> pickableBlockIds;

    private BlockIconHelper() {
    }

    public static List<String> getPickableBlockIds() {
        if (pickableBlockIds == null) {
            pickableBlockIds = buildPickableBlockIds();
        }
        return pickableBlockIds;
    }

    public static ItemStack createStack(String blockId) {
        Block block = resolveBlock(blockId);
        if (block == null || block == Blocks.AIR) {
            return ItemStack.EMPTY;
        }

        Item item = block.asItem();
        if (item == Items.AIR) {
            return new ItemStack(block);
        }

        return new ItemStack(item);
    }

    public static String shortName(String blockId) {
        int colon = blockId.indexOf(':');
        return colon >= 0 ? blockId.substring(colon + 1) : blockId;
    }

    public static void drawBlockIcon(DrawContext drawContext, String blockId, int x, int y) {
        ItemStack stack = createStack(blockId);
        if (stack.isEmpty()) {
            return;
        }

        drawContext.drawItem(stack, x, y);
    }

    public static void drawBlockTooltip(
        DrawContext drawContext,
        String blockId,
        int x,
        int y
    ) {
        ItemStack stack = createStack(blockId);
        if (stack.isEmpty()) {
            return;
        }

        drawContext.drawItemTooltip(MinecraftClient.getInstance().textRenderer, stack, x, y);
    }

    private static List<String> buildPickableBlockIds() {
        List<String> ids = new ArrayList<>();

        for (Block block : Registries.BLOCK) {
            if (block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) {
                continue;
            }

            String blockId = Registries.BLOCK.getId(block).toString();
            if (!createStack(blockId).isEmpty()) {
                ids.add(blockId);
            }
        }

        ids.sort(Comparator.naturalOrder());
        return List.copyOf(ids);
    }

    private static Block resolveBlock(String blockId) {
        String normalized = blockId == null ? null : blockId.trim().toLowerCase();
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }

        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }

        try {
            return Registries.BLOCK.get(Identifier.of(normalized));
        } catch (Exception exception) {
            return null;
        }
    }
}

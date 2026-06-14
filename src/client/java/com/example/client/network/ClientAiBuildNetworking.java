package com.example.client.network;

import net.minecraft.client.MinecraftClient;

public final class ClientAiBuildNetworking {

    private ClientAiBuildNetworking() {
    }

    public static void sendAiBuildRequest(String prompt) {
        sendAiBuildRequest(prompt, false);
    }

    public static void sendAiBuildRequest(String prompt, boolean useWebSearch) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            String command = useWebSearch ? "aibuild --web " + prompt : "aibuild " + prompt;
            client.player.networkHandler.sendChatCommand(command);
        }
    }
}

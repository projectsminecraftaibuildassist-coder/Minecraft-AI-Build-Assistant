package com.example;

import com.example.ai.build.BuildTaskQueue;
import com.example.command.AiApprovalCommands;
import com.example.command.AiBuildCommand;
import com.example.config.AiBuilderConfig;
import com.example.event.ChatMessageHandler;
import com.example.network.AiNetworking;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiBuildAssistantMod implements ModInitializer {

    public static final String MOD_ID = "minecraft-ai-build-assistant";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Minecraft AI Build Assistant starting...");

        AiBuilderConfig.load();
        BuildTaskQueue.register();

        AiNetworking.registerPayloads();
        AiNetworking.registerServerReceivers();

        AiBuildCommand.register();
        AiApprovalCommands.register();
        ChatMessageHandler.registerChatFilter();
    }
}

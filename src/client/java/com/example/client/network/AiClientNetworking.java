package com.example.client.network;

import com.example.client.AiClientState;
import com.example.config.AiBuilderConfig;
import com.example.config.ForbiddenBlockRegistry;
import com.example.network.AiNetworkPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class AiClientNetworking {

    private AiClientNetworking() {
    }

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(AiNetworkPayloads.AiLogS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> AiClientState.addLog(payload.message()));
        });

        ClientPlayNetworking.registerGlobalReceiver(AiNetworkPayloads.ApprovalPendingS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                AiClientState.setPendingApproval(payload.description());
                AiClientState.addLog("[AI] Approval required.");
                for (String line : payload.description().split("\n")) {
                    if (!line.isEmpty()) {
                        AiClientState.addLog("  " + line);
                    }
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(AiNetworkPayloads.ApprovalClearS2CPayload.ID, (payload, context) -> {
            context.client().execute(AiClientState::clearPendingApproval);
        });
    }

    public static void sendApprovalAction(boolean approve) {
        if (approve) {
            ClientPlayNetworking.send(new AiNetworkPayloads.ApprovalAcceptC2SPayload());
        } else {
            ClientPlayNetworking.send(new AiNetworkPayloads.ApprovalDenyC2SPayload());
        }
    }

    public static void sendSettingsSync(AiBuilderConfig config) {
        ClientPlayNetworking.send(new AiNetworkPayloads.AiSettingsSyncC2SPayload(
            config.provider.name(),
            config.modelName,
            config.apiUrl,
            config.apiKey,
            config.ticksPerBlock,
            config.forbiddenBlocksPath,
            ForbiddenBlockRegistry.encodeForNetwork(config.forbiddenBlocks),
            config.debugLogEnabled ? 1 : 0,
            config.openAiWebSearchEnabled ? 1 : 0
        ));
    }
}

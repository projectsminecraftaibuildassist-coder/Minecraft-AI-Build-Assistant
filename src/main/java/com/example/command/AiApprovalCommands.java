package com.example.command;

import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

import com.example.ai.approval.BuildApproval;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import static net.minecraft.server.command.CommandManager.literal;

public final class AiApprovalCommands {

    private AiApprovalCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("ai-approve")
                    .executes(context -> {
                        BuildApproval.approveFor(context.getSource().getPlayer());
                        context.getSource().sendFeedback(() -> Text.literal("Build approved."), false);
                        return 1;
                    })
            );

            dispatcher.register(
                literal("ai-deny")
                    .executes(context -> {
                        BuildApproval.denyFor(context.getSource().getPlayer());
                        context.getSource().sendFeedback(() -> Text.literal("Build cancelled."), false);
                        return 1;
                    })
            );
        });
    }
}

package com.example.client;

import com.example.client.network.AiClientNetworking;
import com.example.client.network.ClientAiBuildNetworking;
import com.example.ai.OpenAiWebSearchSupport;
import com.example.config.AiBuilderConfig;
import com.example.config.AiProvider;

import java.util.LinkedList;
import java.util.List;

public final class AiClientState {

    private static final LinkedList<String> LOGS = new LinkedList<>();
    private static final int MAX_LOGS = 30;

    private static boolean pendingApproval = false;
    private static String pendingDescription = "";
    private static boolean sessionWebSearch = false;
    private static AiPromptScreen openScreen;

    private AiClientState() {
    }

    public static void setOpenScreen(AiPromptScreen screen) {
        openScreen = screen;
    }

    public static void clearOpenScreen(AiPromptScreen screen) {
        if (openScreen == screen) {
            openScreen = null;
        }
    }

    public static void addLog(String message) {
        synchronized (LOGS) {
            LOGS.add(message);
            while (LOGS.size() > MAX_LOGS) {
                LOGS.removeFirst();
            }
        }
        refreshOpenScreen(false);
    }

    public static List<String> getLogs() {
        synchronized (LOGS) {
            return List.copyOf(LOGS);
        }
    }

    public static void setPendingApproval(String description) {
        pendingApproval = true;
        pendingDescription = description == null ? "" : description;
        refreshOpenScreen(true);
    }

    public static void clearPendingApproval() {
        pendingApproval = false;
        pendingDescription = "";
        refreshOpenScreen(true);
    }

    public static boolean hasPendingApproval() {
        return pendingApproval;
    }

    public static String getPendingDescription() {
        return pendingDescription;
    }

    public static boolean isSessionWebSearchEnabled() {
        return sessionWebSearch;
    }

    public static void toggleSessionWebSearch() {
        if (AiBuilderConfig.get().provider != AiProvider.OPENAI) {
            return;
        }
        sessionWebSearch = !sessionWebSearch;
        refreshOpenScreen(true);
    }

    public static boolean willUseWebSearch() {
        AiBuilderConfig config = AiBuilderConfig.get();
        return config.provider == AiProvider.OPENAI
            && (config.openAiWebSearchEnabled || sessionWebSearch);
    }

    public static boolean isWebSearchAvailable() {
        return AiBuilderConfig.get().provider == AiProvider.OPENAI;
    }

    public static void sendPromptText(String prompt) {
        if (prompt == null) {
            return;
        }

        String trimmed = prompt.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        if (willUseWebSearch() && !OpenAiWebSearchSupport.isModelSupported(AiBuilderConfig.get().modelName)) {
            addLog("[AI] Error: " + OpenAiWebSearchSupport.unsupportedModelMessage(AiBuilderConfig.get().modelName));
            return;
        }

        ClientAiBuildNetworking.sendAiBuildRequest(trimmed, willUseWebSearch());
    }

    public static void approve() {
        if (!pendingApproval) {
            return;
        }
        AiClientNetworking.sendApprovalAction(true);
    }

    public static void cancel() {
        if (!pendingApproval) {
            return;
        }
        AiClientNetworking.sendApprovalAction(false);
    }

    private static void refreshOpenScreen(boolean rebuildWidgets) {
        AiPromptScreen screen = openScreen;
        if (screen != null) {
            screen.onStateUpdated(rebuildWidgets);
        }
    }
}

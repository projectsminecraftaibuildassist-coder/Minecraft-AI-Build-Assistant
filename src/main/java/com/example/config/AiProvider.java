package com.example.config;

public enum AiProvider {
    OLLAMA("Ollama (Local)"),
    OPENAI("OpenAI API"),
    CUSTOM("Custom EndPoint");

    private final String displayName;

    AiProvider(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean requiresApiKey() {
        return this == OPENAI;
    }

    public String getDefaultApiUrl() {
        return switch (this) {
            case OLLAMA -> "http://localhost:11434";
            case OPENAI -> "https://api.openai.com";
            case CUSTOM -> "http://localhost:11434";
        };
    }
}

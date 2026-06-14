# Changelog

## [1.0.0] - 2026-06-08

### Added

- Minecraft AI Build Assistant panel (O key) and settings screen
- Ollama, OpenAI, and Custom provider support
- OpenAI web search (Responses API) with manual Web:ON/OFF toggle
- Build approval flow (Accept / Cancel)
- Terrain scan (forward, ground height, obstacles) in AI prompts
- Groovy build API (placement, clearing, terrain inspection, block catalog)
- Forbidden block management (icon picker UI + config file)
- Block placement queue (Build Speed slider)

### Fixed

- OpenAI API URL normalization (404 fix)
- Groovy numeric argument types (`Number` support)
- API calls without the `ai.` prefix
- Block name as first or last argument in placement methods
- `clearArea` 5-argument overload (single horizontal layer)

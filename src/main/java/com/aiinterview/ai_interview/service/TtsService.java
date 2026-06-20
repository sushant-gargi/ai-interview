package com.aiinterview.ai_interview.service;

public interface TtsService {
    /**
     * Generates local raw speech audio bytes from the given text.
     * Returns null if generation fails or is disabled.
     *
     * @param text the content to speak
     * @return raw audio bytes (typically MP3 formatted)
     */
    byte[] generateSpeech(String text);
}

package com.aiinterview.ai_interview.service.impl;

import com.aiinterview.ai_interview.service.TtsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@Slf4j
public class TtsServiceImpl implements TtsService {

    private final RestClient restClient;
    private final String ttsUrl;
    private final String ttsModel;
    private final String ttsVoice;
    private final boolean ttsEnabled;

    public TtsServiceImpl(
            RestClient.Builder restClientBuilder,
            @Value("${app.tts.url}") String ttsUrl,
            @Value("${app.tts.model}") String ttsModel,
            @Value("${app.tts.voice}") String ttsVoice,
            @Value("${app.tts.enabled}") boolean ttsEnabled) {
        this.restClient = restClientBuilder.build();
        this.ttsUrl = ttsUrl;
        this.ttsModel = ttsModel;
        this.ttsVoice = ttsVoice;
        this.ttsEnabled = ttsEnabled;
    }

    @Override
    public byte[] generateSpeech(String text) {
        if (!ttsEnabled || text == null || text.isBlank()) {
            log.debug("TTS is disabled or input text is empty");
            return null;
        }

        try {
            log.info("Generating local speech via Kokoro for text: {}", text);

            Map<String, Object> requestBody = Map.of(
                    "model", ttsModel,
                    "input", text,
                    "voice", ttsVoice,
                    "response_format", "mp3"
            );

            return restClient.post()
                    .uri(ttsUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(byte[].class);

        } catch (Exception e) {
            log.warn("Local TTS generation failed (URL: {}): {}. Continuing chat text conversation without audio.", ttsUrl, e.getMessage());
            return null;
        }
    }
}

package com.aiinterview.ai_interview.service;

import com.aiinterview.ai_interview.service.impl.TtsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TtsServiceTest {

    RestClient restClient;
    RestClient.Builder restClientBuilder;
    TtsServiceImpl ttsService;

    @BeforeEach
    void setup() {
        restClient = mock(RestClient.class);
        restClientBuilder = mock(RestClient.Builder.class);
        when(restClientBuilder.build()).thenReturn(restClient);

        ttsService = new TtsServiceImpl(
                restClientBuilder,
                "http://localhost:8801/v1/audio/speech",
                "kokoro",
                "af_bella",
                true
        );
    }

    @Test
    void generateSpeech_success() {
        byte[] expectedAudio = new byte[]{1, 2, 3};

        RestClient.RequestBodyUriSpec bodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any(MediaType.class))).thenReturn(bodySpec);
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(byte[].class))).thenReturn(expectedAudio);

        byte[] result = ttsService.generateSpeech("Hello world");
        assertNotNull(result);
        assertArrayEquals(expectedAudio, result);
    }

    @Test
    void generateSpeech_whenTtsDisabled_returnsNull() {
        TtsServiceImpl disabledService = new TtsServiceImpl(
                restClientBuilder,
                "http://localhost:8801/v1/audio/speech",
                "kokoro",
                "af_bella",
                false
        );

        byte[] result = disabledService.generateSpeech("Hello world");
        assertNull(result);
    }

    @Test
    void generateSpeech_onApiFailure_returnsNullGracefully() {
        when(restClient.post()).thenThrow(new RuntimeException("Connection refused"));

        byte[] result = ttsService.generateSpeech("Hello world");
        assertNull(result);
    }
}

/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.config.ollama;

import com.innowise.logistics.ocr.exception.OllamaStartupException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatProperties;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OllamaStartupVerifierTest {
    private static final String REQUIRED_MODEL = "llama3.1:8b";

    @Test
    void shouldCompleteStartupWhenRequiredModelIsAvailable() {
        // Arrange
        OllamaApi ollamaApi = mock(OllamaApi.class);
        OllamaChatProperties chatProperties = new OllamaChatProperties();
        chatProperties.setModel(REQUIRED_MODEL);
        OllamaApi.ShowModelResponse showModelResponse =
            mock(OllamaApi.ShowModelResponse.class);
        when(ollamaApi.showModel(
            new OllamaApi.ShowModelRequest(REQUIRED_MODEL)
        )).thenReturn(showModelResponse);
        OllamaStartupVerifier verifier =
            new OllamaStartupVerifier(ollamaApi, chatProperties);

        // Act & Assert
        assertDoesNotThrow(
            () -> verifier.run(new DefaultApplicationArguments())
        );
    }

    @Test
    void shouldFailStartupWhenRequiredModelCannotBeVerified() {
        // Arrange
        OllamaApi ollamaApi = mock(OllamaApi.class);
        OllamaChatProperties chatProperties = new OllamaChatProperties();
        chatProperties.setModel(REQUIRED_MODEL);
        RestClientException cause = new RestClientException("Connection refused");
        when(ollamaApi.showModel(
            new OllamaApi.ShowModelRequest(REQUIRED_MODEL)
        )).thenThrow(cause);
        OllamaStartupVerifier verifier =
            new OllamaStartupVerifier(ollamaApi, chatProperties);
        DefaultApplicationArguments arguments =
            new DefaultApplicationArguments();

        // Act & Assert
        assertThatThrownBy(() -> verifier.run(arguments))
            .isInstanceOf(OllamaStartupException.class)
            .hasMessage(
                "Unable to verify required Ollama model '%s'"
                    .formatted(REQUIRED_MODEL)
            )
            .hasCause(cause);
    }

    @Test
    void shouldFailStartupWhenRequiredModelIsNotConfigured() {
        // Arrange
        OllamaApi ollamaApi = mock(OllamaApi.class);
        OllamaChatProperties chatProperties = new OllamaChatProperties();
        OllamaStartupVerifier verifier =
            new OllamaStartupVerifier(ollamaApi, chatProperties);
        DefaultApplicationArguments arguments =
            new DefaultApplicationArguments();

        // Act & Assert
        assertThatThrownBy(() -> verifier.run(arguments))
            .isInstanceOf(OllamaStartupException.class)
            .hasMessage("Required Ollama model name is not configured");
    }

    @Test
    void shouldVerifyConfiguredModel() {
        // Arrange
        OllamaApi ollamaApi = mock(OllamaApi.class);
        OllamaChatProperties chatProperties = new OllamaChatProperties();
        chatProperties.setModel(REQUIRED_MODEL);
        OllamaApi.ShowModelResponse showModelResponse =
            mock(OllamaApi.ShowModelResponse.class);
        when(ollamaApi.showModel(
            new OllamaApi.ShowModelRequest(REQUIRED_MODEL)
        )).thenReturn(showModelResponse);
        OllamaStartupVerifier verifier =
            new OllamaStartupVerifier(ollamaApi, chatProperties);

        // Act
        verifier.run(new DefaultApplicationArguments());

        // Assert
        verify(ollamaApi).showModel(
            new OllamaApi.ShowModelRequest(REQUIRED_MODEL)
        );
    }
}

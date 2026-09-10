/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.config.ollama;

import com.innowise.logistics.ocr.exception.OllamaStartupException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatProperties;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public final class OllamaStartupVerifier implements ApplicationRunner {
    private final OllamaApi ollamaApi;
    private final OllamaChatProperties chatProperties;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        Optional.ofNullable(chatProperties.getModel())
            .ifPresentOrElse(
                this::verifyModel,
                () -> {
                    throw new OllamaStartupException(
                        "Required Ollama model name is not configured"
                    );
                }
            );
    }

    private void verifyModel(String model) {
        try {
            ollamaApi.showModel(
                new OllamaApi.ShowModelRequest(model)
            );
        } catch (RestClientException cause) {
            throw new OllamaStartupException(
                "Unable to verify required Ollama model '%s'"
                    .formatted(model),
                cause
            );
        }
    }
}

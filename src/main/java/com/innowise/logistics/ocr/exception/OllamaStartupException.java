/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.exception;

public final class OllamaStartupException extends RuntimeException {
    public OllamaStartupException(String message) {
        super(message);
    }

    public OllamaStartupException(String message, Throwable cause) {
        super(message, cause);
    }
}

/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.exception;

public final class ObjectStorageStartupException extends RuntimeException {
    public ObjectStorageStartupException(String message) {
        super(message);
    }

    public ObjectStorageStartupException(String message, Throwable cause) {
        super(message, cause);
    }
}

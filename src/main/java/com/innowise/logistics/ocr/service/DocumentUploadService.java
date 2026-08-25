/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface DocumentUploadService {
    UUID upload(UUID orderId, String documentType, MultipartFile file);
}

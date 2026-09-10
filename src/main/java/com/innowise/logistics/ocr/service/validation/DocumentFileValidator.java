/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.service.validation;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentFileValidator {
    void validate(MultipartFile file);
}

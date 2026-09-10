/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.service.staging;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface DocumentFileStaging {
    Path stage(MultipartFile file);

    void delete(Path file);
}

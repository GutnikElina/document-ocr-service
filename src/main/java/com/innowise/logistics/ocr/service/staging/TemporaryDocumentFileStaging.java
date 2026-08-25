/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.service.staging;

import com.innowise.logistics.ocr.exception.InvalidDocumentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class TemporaryDocumentFileStaging implements DocumentFileStaging {
    @Override
    public Path stage(MultipartFile file) {
        try {
            Path tempFile = Files.createTempFile(
                "logistics-document-",
                ".pdf"
            );
            file.transferTo(tempFile);
            return tempFile;
        } catch (IOException e) {
            throw new InvalidDocumentException(
                "Unable to stage uploaded document",
                e
            );
        }
    }

    @Override
    public void delete(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn(
                "Unable to delete temporary document file: {}",
                file,
                e
            );
        }
    }
}

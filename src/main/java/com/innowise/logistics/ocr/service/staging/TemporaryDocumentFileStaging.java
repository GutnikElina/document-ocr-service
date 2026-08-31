/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.service.staging;

import com.innowise.logistics.ocr.config.file.DocumentProperties;
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
    private final Path stagingDirectory;

    public TemporaryDocumentFileStaging(DocumentProperties documentProperties) {
        this.stagingDirectory = Path.of(documentProperties.stagingDirectory());
        try {
            Files.createDirectories(this.stagingDirectory);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Unable to initialize document staging directory",
                e
            );
        }
    }

    @Override
    public Path stage(MultipartFile file) {
        try {
            Path tempFile = Files.createTempFile(
                stagingDirectory,
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

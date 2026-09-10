/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.service;

import com.innowise.logistics.ocr.service.extraction.ExtractedText;
import com.innowise.logistics.ocr.service.extraction.ExtractionMethod;
import com.innowise.logistics.ocr.service.extraction.TextExtractor;
import com.innowise.logistics.ocr.service.staging.DocumentFileStaging;
import com.innowise.logistics.ocr.service.validation.DocumentFileValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentUploadServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldValidateStageExtractAndReturnDocumentId() {
        // Arrange
        var validator = mock(DocumentFileValidator.class);
        var staging = mock(DocumentFileStaging.class);
        var extractor = mock(TextExtractor.class);
        var stagedFile = tempDir.resolve("document.pdf");
        when(staging.stage(any()))
            .thenReturn(stagedFile);
        when(extractor.extract(stagedFile))
            .thenReturn(
                new ExtractedText(
                    "ORDER-123",
                    ExtractionMethod.TIKA
                )
            );
        var service = new DocumentUploadServiceImpl(
            validator,
            staging,
            extractor
        );
        var file = new MockMultipartFile(
            "file",
            "document.pdf",
            "application/pdf",
            "%PDF-1.7".getBytes()
        );

        // Act
        var documentId = service.upload(
            UUID.randomUUID(),
            "INVOICE",
            file
        );

        // Assert
        assertThat(documentId).isNotNull();
        verify(validator).validate(file);
        verify(staging).stage(file);
        verify(extractor).extract(stagedFile);
    }

    @Test
    void shouldAlwaysDeleteStagedFile() {
        // Arrange
        var validator = mock(DocumentFileValidator.class);
        var staging = mock(DocumentFileStaging.class);
        var extractor = mock(TextExtractor.class);
        var stagedFile = tempDir.resolve("document.pdf");
        when(staging.stage(any()))
            .thenReturn(stagedFile);
        when(extractor.extract(stagedFile))
            .thenThrow(new RuntimeException("boom"));
        var service = new DocumentUploadServiceImpl(
            validator,
            staging,
            extractor
        );
        var file = new MockMultipartFile(
            "file",
            "document.pdf",
            "application/pdf",
            "%PDF".getBytes()
        );

        // Act & Assert
        try {
            service.upload(
                UUID.randomUUID(),
                "INVOICE",
                file
            );
        } catch (RuntimeException ignored) {
        }
        verify(staging).delete(stagedFile);
    }
}

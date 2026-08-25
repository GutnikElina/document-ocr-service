/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.service.validation;

import com.innowise.logistics.ocr.exception.InvalidDocumentException;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfDocumentFileValidatorTest {
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    private final PdfDocumentFileValidator validator =
        new PdfDocumentFileValidator(MAX_FILE_SIZE);

    @Test
    void shouldAcceptValidPdf() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "file",
            "document.pdf",
            "application/pdf",
            "%PDF-1.7\n".getBytes()
        );

        // Act & Assert
        assertThatCode(() -> validator.validate(file))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectNullFile() {
        // Act & Assert
        assertThatThrownBy(() -> validator.validate(null))
            .isInstanceOf(InvalidDocumentException.class)
            .hasMessage("File is required");
    }

    @Test
    void shouldRejectEmptyFile() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "file",
            "document.pdf",
            "application/pdf",
            new byte[0]
        );

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(InvalidDocumentException.class)
            .hasMessage("Uploaded file is empty");
    }

    @Test
    void shouldRejectFileExceedingMaximumSize() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "file",
            "document.pdf",
            "application/pdf",
            new byte[]{1}
        ) {
            @Override
            public long getSize() {
                return MAX_FILE_SIZE + 1;
            }
        };

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(InvalidDocumentException.class)
            .hasMessage(
                "Uploaded file exceeds maximum size of "
                    + MAX_FILE_SIZE
                    + " bytes"
            );
    }

    @Test
    void shouldAcceptFileExactlyAtMaximumSize() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "file",
            "document.pdf",
            "application/pdf",
            new byte[]{1}
        ) {
            @Override
            public long getSize() {
                return MAX_FILE_SIZE;
            }

            @NonNull
            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(
                    "%PDF-1.7\n".getBytes()
                );
            }
        };

        // Act & Assert
        assertThatCode(() -> validator.validate(file))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectNonPdfFile() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "file",
            "document.txt",
            "text/plain",
            "hello world".getBytes()
        );

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(InvalidDocumentException.class)
            .hasMessageContaining(
                "Only PDF documents are supported"
            )
            .hasMessageContaining(
                "text/plain"
            );
    }

    @Test
    void shouldRejectPngFileEvenIfExtensionIsPdf() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "file",
            "document.pdf",
            "application/pdf",
            new byte[]{
                (byte) 0x89,
                'P',
                'N',
                'G',
                '\r',
                '\n'
            }
        );

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(InvalidDocumentException.class)
            .hasMessageContaining(
                "Only PDF documents are supported"
            );
    }

    @Test
    void shouldRejectJpegFileEvenIfExtensionIsPdf() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "file",
            "document.pdf",
            "application/pdf",
            new byte[]{
                (byte) 0xFF,
                (byte) 0xD8,
                (byte) 0xFF,
                (byte) 0xE0
            }
        );

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(InvalidDocumentException.class)
            .hasMessageContaining(
                "Only PDF documents are supported"
            );
    }

    @Test
    void shouldWrapIOExceptionFromInputStream() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "file",
            "document.pdf",
            "application/pdf",
            new byte[]{1}
        ) {
            @NonNull
            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("Cannot read uploaded file");
            }
        };

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(InvalidDocumentException.class)
            .hasMessage("Unable to inspect uploaded file")
            .hasCauseInstanceOf(IOException.class);
    }
}

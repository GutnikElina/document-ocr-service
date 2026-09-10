/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.service.validation;

import com.innowise.logistics.ocr.exception.InvalidDocumentException;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@RequiredArgsConstructor
public class PdfDocumentFileValidator implements DocumentFileValidator {

    private static final String PDF_MEDIA_TYPE = "application/pdf";

    private final long maxFileSize;
    private final Tika tika = new Tika();

    @Override
    public void validate(MultipartFile file) {
        Optional.ofNullable(file)
            .orElseThrow(() -> new InvalidDocumentException("File is required"));
        Optional.of(file)
            .filter(upload -> !upload.isEmpty())
            .orElseThrow(() -> new InvalidDocumentException(
                "Uploaded file is empty"
            ));
        Optional.of(file)
            .filter(upload -> upload.getSize() <= maxFileSize)
            .orElseThrow(() -> new InvalidDocumentException(
                "Uploaded file exceeds maximum size of " + maxFileSize + " bytes"
            ));
        validatePdfMediaType(file);
    }

    private void validatePdfMediaType(MultipartFile file) {
        String detectedType = detectMediaType(file);
        Optional.ofNullable(detectedType)
            .filter(PDF_MEDIA_TYPE::equals)
            .orElseThrow(() -> new InvalidDocumentException(
                "Only PDF documents are supported. Detected: " + detectedType
            ));
    }

    private String detectMediaType(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            return tika.detect(is);
        } catch (IOException e) {
            throw new InvalidDocumentException(
                "Unable to inspect uploaded file",
                e
            );
        }
    }
}

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

@RequiredArgsConstructor
public class PdfDocumentFileValidator implements DocumentFileValidator {

    private static final String PDF_MEDIA_TYPE = "application/pdf";

    private final long maxFileSize;
    private final Tika tika = new Tika();

    @Override
    public void validate(MultipartFile file) {
        if (file == null) {
            throw new InvalidDocumentException("File is required");
        }
        if (file.isEmpty()) {
            throw new InvalidDocumentException("Uploaded file is empty");
        }
        if (file.getSize() > maxFileSize) {
            throw new InvalidDocumentException(
                "Uploaded file exceeds maximum size of " + maxFileSize + " bytes"
            );
        }
        try {
            String detectedType = tika.detect(file.getInputStream());
            if (!PDF_MEDIA_TYPE.equals(detectedType)) {
                throw new InvalidDocumentException(
                    "Only PDF documents are supported. Detected: " + detectedType
                );
            }
        } catch (IOException e) {
            throw new InvalidDocumentException(
                "Unable to inspect uploaded file",
                e
            );
        }
    }
}

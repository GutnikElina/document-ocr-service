/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.service;

import com.innowise.logistics.ocr.service.extraction.ExtractedText;
import com.innowise.logistics.ocr.service.extraction.TextExtractor;
import com.innowise.logistics.ocr.service.staging.DocumentFileStaging;
import com.innowise.logistics.ocr.service.validation.DocumentFileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentUploadServiceImpl implements DocumentUploadService {
    private final DocumentFileValidator validator;
    private final DocumentFileStaging staging;
    private final TextExtractor textExtractorChain;

    @Override
    public UUID upload(UUID orderId, String documentType, MultipartFile file) {
        validator.validate(file);
        UUID documentId = UUID.randomUUID();
        Path stagedFile = staging.stage(file);
        try {
            ExtractedText extractedText = textExtractorChain.extract(stagedFile);
            log.info(
                "Document text extracted: documentId={}, orderId={}, documentType={}, method={}, text={}",
                documentId,
                orderId,
                documentType,
                extractedText.method(),
                extractedText.text()
            );
            return documentId;
        } finally {
            staging.delete(stagedFile);
        }
    }
}

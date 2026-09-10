/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.rest.controller;

import com.innowise.logistics.ocr.rest.api.DocumentsApi;
import com.innowise.logistics.ocr.rest.dto.DocumentUploadedResponse;
import com.innowise.logistics.ocr.service.DocumentUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DocumentController implements DocumentsApi {
    private final DocumentUploadService documentUploadService;

    @Override
    public ResponseEntity<DocumentUploadedResponse> upload(
        UUID orderId,
        String documentType,
        MultipartFile file
    ) {
        // TODO
        UUID documentId = documentUploadService.upload(orderId, documentType, file);
        DocumentUploadedResponse response = new DocumentUploadedResponse()
            .id(documentId)
            .status(
                DocumentUploadedResponse.StatusEnum.ACCEPTED
            );
        return ResponseEntity.accepted().body(response);
    }
}

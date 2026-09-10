/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.rest.controller;

import com.innowise.logistics.ocr.exception.InvalidDocumentException;
import com.innowise.logistics.ocr.exception.TextExtractionException;
import com.innowise.logistics.ocr.rest.api.DocumentsApi;
import com.innowise.logistics.ocr.service.DocumentUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentUploadService documentUploadService;

    @Test
    @WithMockUser
    void shouldReturn202ForAcceptedPdf() throws Exception {
        // Arrange
        var documentId = UUID.randomUUID();
        when(documentUploadService.upload(any(), eq("INVOICE"), any()))
            .thenReturn(documentId);
        var file = new MockMultipartFile(
            "file",
            "invoice.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "%PDF-1.7".getBytes()
        );

        // Act
        mockMvc.perform(
                multipart(DocumentsApi.PATH_UPLOAD)
                    .file(file)
                    .param("orderId", UUID.randomUUID().toString())
                    .param("documentType", "INVOICE")
                    .with(jwt())
            )
            // Assert
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.id").value(documentId.toString()))
            .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    @WithMockUser
    void shouldReturn400WhenDocumentIsInvalid() throws Exception {
        // Arrange
        var orderId = UUID.randomUUID();
        given(documentUploadService.upload(eq(orderId), eq("INVOICE"), any()))
            .willThrow(
                new InvalidDocumentException(
                    "Only PDF documents are supported"
                )
            );

        // Act
        mockMvc.perform(
                multipart(DocumentsApi.PATH_UPLOAD)
                    .file(pdfFile())
                    .param("orderId", orderId.toString())
                    .param("documentType", "INVOICE")
                    .with(jwt())
            )
            // Assert
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title")
                .value("Invalid document"))
            .andExpect(jsonPath("$.detail")
                .value("Only PDF documents are supported"));
        verify(documentUploadService).upload(
            eq(orderId),
            eq("INVOICE"),
            any()
        );
    }

    @Test
    @WithMockUser
    void shouldReturn422WhenTextExtractionFails() throws Exception {
        // Arrange
        var orderId = UUID.randomUUID();
        given(documentUploadService.upload(eq(orderId), eq("INVOICE"), any()))
            .willThrow(
                new TextExtractionException(
                    "Unable to extract text from document"
                )
            );

        // Act
        mockMvc.perform(
                multipart(DocumentsApi.PATH_UPLOAD)
                    .file(pdfFile())
                    .param("orderId", orderId.toString())
                    .param("documentType", "INVOICE")
                    .with(jwt())
            )
            // Assert
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.title")
                .value("Document text extraction failed"))
            .andExpect(jsonPath("$.detail")
                .value("Unable to extract text from document"));
        verify(documentUploadService).upload(
            eq(orderId),
            eq("INVOICE"),
            any()
        );
    }

    @Test
    @WithMockUser
    void shouldReturn400WhenOrderIdIsInvalid() throws Exception {
        // Act
        mockMvc.perform(
                multipart(DocumentsApi.PATH_UPLOAD)
                    .file(pdfFile())
                    .param("orderId", "not-a-uuid")
                    .param("documentType", "INVOICE")
                    .with(jwt())
            )
            // Assert
            .andExpect(status().isBadRequest());
        verify(documentUploadService, never()).upload(any(), any(), any());
    }

    @Test
    @WithMockUser
    void shouldReturn400WhenOrderIdIsMissing() throws Exception {
        // Act
        mockMvc.perform(
                multipart(DocumentsApi.PATH_UPLOAD)
                    .file(pdfFile())
                    .param("documentType", "INVOICE")
                    .with(jwt())
            )
            // Assert
            .andExpect(status().isBadRequest());
        verify(documentUploadService, never()).upload(any(), any(), any());
    }

    @Test
    @WithMockUser
    void shouldReturn400WhenDocumentTypeIsMissing() throws Exception {
        // Arrange
        var orderId = UUID.randomUUID();

        // Act
        mockMvc.perform(
                multipart(DocumentsApi.PATH_UPLOAD)
                    .file(pdfFile())
                    .param("orderId", orderId.toString())
                    .with(jwt())
            )
            // Assert
            .andExpect(status().isBadRequest());
        verify(documentUploadService, never()).upload(any(), any(), any());
    }

    @Test
    @WithMockUser
    void shouldReturn400WhenFileIsMissing() throws Exception {
        // Arrange
        var orderId = UUID.randomUUID();

        // Act
        mockMvc.perform(
                multipart(DocumentsApi.PATH_UPLOAD)
                    .param("orderId", orderId.toString())
                    .param("documentType", "INVOICE")
                    .with(jwt())
            )
            // Assert
            .andExpect(status().isBadRequest());
        verify(documentUploadService, never()).upload(any(), any(), any());
    }

    @Test
    @WithMockUser
    void shouldReturn400WhenFileIsEmpty() throws Exception {
        // Arrange
        var orderId = UUID.randomUUID();
        var emptyFile = new MockMultipartFile(
            "file",
            "document.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            new byte[0]
        );
        given(documentUploadService.upload(eq(orderId), eq("INVOICE"), any()))
            .willThrow(
                new InvalidDocumentException(
                    "Uploaded file is empty"
                )
            );

        // Act
        mockMvc.perform(
                multipart(DocumentsApi.PATH_UPLOAD)
                    .file(emptyFile)
                    .param("orderId", orderId.toString())
                    .param("documentType", "INVOICE")
                    .with(jwt())
            )
            // Assert
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title")
                .value("Invalid document"))
            .andExpect(jsonPath("$.detail")
                .value("Uploaded file is empty"));
    }

    private MockMultipartFile pdfFile() {
        return new MockMultipartFile(
            "file",
            "document.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "%PDF-1.7\n".getBytes()
        );
    }
}

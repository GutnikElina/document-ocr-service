/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.rest.advice;

import com.innowise.logistics.ocr.exception.InvalidDocumentException;
import com.innowise.logistics.ocr.exception.TextExtractionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(InvalidDocumentException.class)
    public ProblemDetail handleInvalidDocument(InvalidDocumentException exception) {
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            exception.getMessage()
        );
        problem.setTitle("Invalid document");
        return problem;
    }

    @ExceptionHandler(TextExtractionException.class)
    public ProblemDetail handleTextExtraction(TextExtractionException exception) {
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "Unable to extract text from document"
        );
        problem.setTitle("Document text extraction failed");
        return problem;
    }

}

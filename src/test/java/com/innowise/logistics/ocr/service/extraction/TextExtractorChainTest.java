/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.service.extraction;

import com.innowise.logistics.ocr.exception.TextExtractionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TextExtractorChainTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReturnResultFromFirstExtractorWithText() {
        // Arrange
        var firstExtractor = mock(TextExtractor.class);
        var secondExtractor = mock(TextExtractor.class);
        var document = tempDir.resolve("document.pdf");
        var expected = new ExtractedText(
            "extracted text",
            ExtractionMethod.TIKA
        );
        when(firstExtractor.extract(document))
            .thenReturn(expected);
        var chain = new TextExtractorChain(
            List.of(firstExtractor, secondExtractor)
        );

        // Act
        var result = chain.extract(document);

        // Assert
        assertThat(result)
            .isSameAs(expected);
        verify(firstExtractor).extract(document);
        verifyNoInteractions(secondExtractor);
    }

    @Test
    void shouldFallbackToNextExtractorWhenFirstReturnsEmptyText() {
        // Arrange
        var firstExtractor = mock(TextExtractor.class);
        var secondExtractor = mock(TextExtractor.class);
        var document = tempDir.resolve("document.pdf");
        when(firstExtractor.extract(document))
            .thenReturn(
                new ExtractedText(
                    "",
                    ExtractionMethod.TIKA
                )
            );
        var expected = new ExtractedText(
            "OCR text",
            ExtractionMethod.TESSERACT
        );
        when(secondExtractor.extract(document))
            .thenReturn(expected);
        var chain = new TextExtractorChain(
            List.of(firstExtractor, secondExtractor)
        );

        // Act
        var result = chain.extract(document);

        // Assert
        assertThat(result)
            .isSameAs(expected);
        verify(firstExtractor).extract(document);
        verify(secondExtractor).extract(document);
    }

    @Test
    void shouldFallbackToNextExtractorWhenFirstReturnsBlankText() {
        // Arrange
        var firstExtractor = mock(TextExtractor.class);
        var secondExtractor = mock(TextExtractor.class);
        var document = tempDir.resolve("document.pdf");
        when(firstExtractor.extract(document))
            .thenReturn(
                new ExtractedText(
                    "   ",
                    ExtractionMethod.TIKA
                )
            );
        var expected = new ExtractedText(
            "OCR text",
            ExtractionMethod.TESSERACT
        );
        when(secondExtractor.extract(document))
            .thenReturn(expected);
        var chain = new TextExtractorChain(
            List.of(firstExtractor, secondExtractor)
        );

        // Act
        var result = chain.extract(document);

        // Assert
        assertThat(result)
            .isSameAs(expected);
        verify(firstExtractor).extract(document);
        verify(secondExtractor).extract(document);
    }

    @Test
    void shouldFallbackToNextExtractorWhenFirstReturnsNullText() {
        // Arrange
        var firstExtractor = mock(TextExtractor.class);
        var secondExtractor = mock(TextExtractor.class);
        var document = tempDir.resolve("document.pdf");
        when(firstExtractor.extract(document))
            .thenReturn(
                new ExtractedText(
                    null,
                    ExtractionMethod.TIKA
                )
            );
        var expected = new ExtractedText(
            "OCR text",
            ExtractionMethod.TESSERACT
        );
        when(secondExtractor.extract(document))
            .thenReturn(expected);
        var chain = new TextExtractorChain(
            List.of(firstExtractor, secondExtractor)
        );

        // Act
        var result = chain.extract(document);

        // Assert
        assertThat(result)
            .isSameAs(expected);
        verify(firstExtractor).extract(document);
        verify(secondExtractor).extract(document);
    }

    @Test
    void shouldFallbackToNextExtractorWhenFirstThrowsTextExtractionException() {
        // Arrange
        var firstExtractor = mock(TextExtractor.class);
        var secondExtractor = mock(TextExtractor.class);
        var document = tempDir.resolve("document.pdf");
        var originalException =
            new TextExtractionException("Tika failed");
        when(firstExtractor.extract(document))
            .thenThrow(originalException);
        var expected = new ExtractedText(
            "OCR text",
            ExtractionMethod.TESSERACT
        );
        when(secondExtractor.extract(document))
            .thenReturn(expected);
        var chain = new TextExtractorChain(
            List.of(firstExtractor, secondExtractor)
        );

        // Act
        var result = chain.extract(document);

        // Assert
        assertThat(result)
            .isSameAs(expected);
        verify(firstExtractor).extract(document);
        verify(secondExtractor).extract(document);
    }

    @Test
    void shouldContinueTryingExtractorsAfterFailure() {
        // Arrange
        var firstExtractor = mock(TextExtractor.class);
        var secondExtractor = mock(TextExtractor.class);
        var thirdExtractor = mock(TextExtractor.class);
        var document = tempDir.resolve("document.pdf");
        when(firstExtractor.extract(document))
            .thenThrow(
                new TextExtractionException("Tika failed")
            );
        when(secondExtractor.extract(document))
            .thenReturn(
                new ExtractedText(
                    "",
                    ExtractionMethod.TIKA
                )
            );
        var expected = new ExtractedText(
            "OCR text",
            ExtractionMethod.TESSERACT
        );
        when(thirdExtractor.extract(document))
            .thenReturn(expected);
        var chain = new TextExtractorChain(
            List.of(
                firstExtractor,
                secondExtractor,
                thirdExtractor
            )
        );

        // Act
        var result = chain.extract(document);

        // Assert
        assertThat(result)
            .isSameAs(expected);
        verify(firstExtractor).extract(document);
        verify(secondExtractor).extract(document);
        verify(thirdExtractor).extract(document);
    }

    @Test
    void shouldThrowAllExtractorsFailedWhenAllExtractorsThrowException() {
        // Arrange
        var firstExtractor = mock(TextExtractor.class);
        var secondExtractor = mock(TextExtractor.class);
        var document = tempDir.resolve("document.pdf");
        var firstException =
            new TextExtractionException("Tika failed");
        var secondException =
            new TextExtractionException("Tesseract failed");
        when(firstExtractor.extract(document))
            .thenThrow(firstException);
        when(secondExtractor.extract(document))
            .thenThrow(secondException);
        var chain = new TextExtractorChain(
            List.of(firstExtractor, secondExtractor)
        );

        // Act & Assert
        assertThatThrownBy(
            () -> chain.extract(document)
        )
            .isInstanceOf(TextExtractionException.class)
            .hasMessage("All configured text extractors failed")
            .hasCause(secondException);
        verify(firstExtractor).extract(document);
        verify(secondExtractor).extract(document);
    }

    @Test
    void shouldThrowAllExtractorsReturnedEmptyWhenNoExtractorReturnsText() {
        // Arrange
        var firstExtractor = mock(TextExtractor.class);
        var secondExtractor = mock(TextExtractor.class);
        var document = tempDir.resolve("document.pdf");
        when(firstExtractor.extract(document))
            .thenReturn(
                new ExtractedText(
                    "",
                    ExtractionMethod.TIKA
                )
            );
        when(secondExtractor.extract(document))
            .thenReturn(
                new ExtractedText(
                    "   ",
                    ExtractionMethod.TESSERACT
                )
            );
        var chain = new TextExtractorChain(
            List.of(firstExtractor, secondExtractor)
        );

        // Act & Assert
        assertThatThrownBy(
            () -> chain.extract(document)
        )
            .isInstanceOf(TextExtractionException.class)
            .hasMessage(
                "All configured text extractors returned empty text"
            )
            .hasNoCause();
        verify(firstExtractor).extract(document);
        verify(secondExtractor).extract(document);
    }

    @Test
    void shouldThrowAllExtractorsReturnedEmptyWhenExtractorReturnsNullText() {
        // Arrange
        var extractor = mock(TextExtractor.class);
        var document = tempDir.resolve("document.pdf");
        when(extractor.extract(document))
            .thenReturn(
                new ExtractedText(
                    null,
                    ExtractionMethod.TIKA
                )
            );
        var chain = new TextExtractorChain(
            List.of(extractor)
        );

        // Act & Assert
        assertThatThrownBy(
            () -> chain.extract(document)
        )
            .isInstanceOf(TextExtractionException.class)
            .hasMessage(
                "All configured text extractors returned empty text"
            )
            .hasNoCause();
        verify(extractor).extract(document);
    }
}

package com.aiinterview.ai_interview.service.impl;

import com.aiinterview.ai_interview.dto.report.QuestionScore;
import com.aiinterview.ai_interview.entity.Report;
import com.aiinterview.ai_interview.error.ResourceNotFoundException;
import com.aiinterview.ai_interview.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfReportService {

    private final ReportRepository reportRepository;

    public byte[] generatePdfReport(Long sessionId) {
        Report report = reportRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", sessionId.toString()));

        try (PDDocument document = new PDDocument()) {
            PdfRenderer renderer = new PdfRenderer(document);
            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            // Title
            renderer.checkPageBreak(50);
            renderer.stream.beginText();
            renderer.stream.setFont(titleFont, 18);
            renderer.stream.newLineAtOffset(renderer.margin, renderer.currentY);
            renderer.stream.showText("AI Interview Evaluation Report");
            renderer.stream.endText();
            renderer.currentY -= 30;

            // Basic Info
            renderer.drawText("Candidate: " + report.getSession().getCandidateEmail(), boldFont, 12);
            renderer.drawText("Role: " + report.getSession().getJobRole(), regularFont, 12);
            renderer.drawText("Overall Score: " + (report.getOverallScore() != null ? report.getOverallScore() : "N/A") + "/10", boldFont, 12);
            renderer.drawText("Recommendation: " + (report.getRecommendation() != null ? report.getRecommendation().name() : "N/A"), boldFont, 12);
            renderer.currentY -= 20;

            // Summary
            renderer.checkPageBreak(30);
            renderer.stream.beginText();
            renderer.stream.setFont(boldFont, 14);
            renderer.stream.newLineAtOffset(renderer.margin, renderer.currentY);
            renderer.stream.showText("Summary");
            renderer.stream.endText();
            renderer.currentY -= 20;

            if (report.getSummary() != null) {
                renderer.drawWrappedText(report.getSummary(), regularFont, 11);
            }
            renderer.currentY -= 20;

            // Question Scores
            if (report.getQuestionScores() != null && !report.getQuestionScores().isEmpty()) {
                renderer.checkPageBreak(30);
                renderer.stream.beginText();
                renderer.stream.setFont(boldFont, 14);
                renderer.stream.newLineAtOffset(renderer.margin, renderer.currentY);
                renderer.stream.showText("Q&A Breakdown");
                renderer.stream.endText();
                renderer.currentY -= 20;

                for (int i = 0; i < report.getQuestionScores().size(); i++) {
                    QuestionScore qs = report.getQuestionScores().get(i);
                    renderer.drawText("Q" + (i + 1) + ": " + qs.question(), boldFont, 11);
                    renderer.drawText("Score: " + qs.score() + "/10", boldFont, 11);
                    renderer.drawWrappedText("Feedback: " + qs.feedback(), regularFont, 11);
                    renderer.currentY -= 15;
                }
            }

            renderer.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Error generating PDF for session {}", sessionId, e);
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private class PdfRenderer {
        PDDocument document;
        PDPageContentStream stream;
        float margin = 50;
        float currentY;
        float width;

        public PdfRenderer(PDDocument document) throws IOException {
            this.document = document;
            addNewPage();
        }

        void addNewPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            PDPage page = new PDPage();
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            currentY = page.getMediaBox().getHeight() - margin;
            width = page.getMediaBox().getWidth() - 2 * margin;
        }

        void checkPageBreak(float requiredSpace) throws IOException {
            if (currentY - requiredSpace < margin) {
                addNewPage();
            }
        }

        void close() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }

        void drawText(String text, PDType1Font font, int fontSize) throws IOException {
            checkPageBreak(fontSize + 4);
            stream.beginText();
            stream.setFont(font, fontSize);
            stream.newLineAtOffset(margin, currentY);
            String cleanText = text.replace("\n", " ").replace("\r", " ").replaceAll("[^\\x00-\\x7F]", "");
            
            if (font.getStringWidth(cleanText) / 1000 * fontSize > width) {
               cleanText = cleanText.substring(0, Math.min(cleanText.length(), 80)) + "...";
            }
            
            stream.showText(cleanText);
            stream.endText();
            currentY -= (fontSize + 4);
        }

        void drawWrappedText(String text, PDType1Font font, int fontSize) throws IOException {
            String cleanText = text.replaceAll("[^\\x00-\\x7F\n]", "");
            String[] words = cleanText.split(" ");
            List<String> lines = new ArrayList<>();
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                if (word.contains("\n")) {
                    String[] splitNewline = word.split("\n");
                    for (int i=0; i<splitNewline.length; i++) {
                        if (font.getStringWidth(currentLine.toString() + " " + splitNewline[i]) / 1000 * fontSize > width) {
                            lines.add(currentLine.toString().trim());
                            currentLine = new StringBuilder(splitNewline[i] + " ");
                        } else {
                            currentLine.append(splitNewline[i]).append(" ");
                        }
                        if (i < splitNewline.length - 1) {
                             lines.add(currentLine.toString().trim());
                             currentLine = new StringBuilder();
                        }
                    }
                } else {
                    if (font.getStringWidth(currentLine.toString() + " " + word) / 1000 * fontSize > width) {
                        lines.add(currentLine.toString().trim());
                        currentLine = new StringBuilder(word + " ");
                    } else {
                        currentLine.append(word).append(" ");
                    }
                }
            }
            lines.add(currentLine.toString().trim());

            for (String line : lines) {
                if (line.isBlank()) continue;
                checkPageBreak(fontSize + 4);
                stream.beginText();
                stream.setFont(font, fontSize);
                stream.newLineAtOffset(margin, currentY);
                stream.showText(line);
                stream.endText();
                currentY -= (fontSize + 4);
            }
        }
    }
}

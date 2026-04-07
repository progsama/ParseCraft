package com.portfolio.docanalyzer.service;

import com.portfolio.docanalyzer.exception.DocumentParsingException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentParsingService {

    public String extractText(MultipartFile file, String extension) {
        try (InputStream inputStream = file.getInputStream()) {
            return switch (extension) {
                case "pdf" -> parsePdf(inputStream);
                case "docx" -> parseDocx(inputStream);
                case "txt" -> new String(file.getBytes(), StandardCharsets.UTF_8);
                default -> throw new DocumentParsingException("Unsupported file extension for parsing.", null);
            };
        } catch (IOException ex) {
            throw new DocumentParsingException("Failed to parse uploaded document.", ex);
        }
    }

    private String parsePdf(InputStream inputStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String parseDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(inputStream)) {
            StringBuilder builder = new StringBuilder();
            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                builder.append(paragraph.getText()).append(System.lineSeparator());
            }
            return builder.toString();
        }
    }
}

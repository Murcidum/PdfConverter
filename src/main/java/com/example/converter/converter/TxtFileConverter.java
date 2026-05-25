package com.example.converter.converter;

import com.example.converter.exception.ConversionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TxtFileConverter implements FileConverter {

    private static final float MARGIN = 50f;
    private static final float FONT_SIZE = 12f;
    private static final float LEADING = 16f;

    @Override
    public String getSupportedExtension() {
        return "txt";
    }

    @Override
    public byte[] convert(byte[] fileContent) {
        String text = new String(fileContent, StandardCharsets.UTF_8);
        String[] rawLines = text.split("\\r?\\n", -1);

        try (PDDocument doc = new PDDocument()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float usableWidth = PDRectangle.A4.getWidth() - 2 * MARGIN;
            List<String> lines = wrapLines(rawLines, font, usableWidth);

            float topY = PDRectangle.A4.getHeight() - MARGIN;
            float bottomY = MARGIN;
            float currentY = topY;
            List<String> pageLines = new ArrayList<>();

            for (String line : lines) {
                if (currentY - LEADING < bottomY) {
                    writePage(doc, font, pageLines, topY);
                    pageLines = new ArrayList<>();
                    currentY = topY;
                }
                pageLines.add(line);
                currentY -= LEADING;
            }
            writePage(doc, font, pageLines, topY);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ConversionException("Failed to convert TXT to PDF", e);
        }
    }

    private void writePage(PDDocument doc, PDType1Font font, List<String> lines, float topY) throws IOException {
        PDPage page = newPage(doc);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            cs.beginText();
            cs.setFont(font, FONT_SIZE);
            cs.setLeading(LEADING);
            cs.newLineAtOffset(MARGIN, topY);
            for (String line : lines) {
                cs.showText(sanitize(line));
                cs.newLine();
            }
            cs.endText();
        }
    }

    private List<String> wrapLines(String[] rawLines, PDType1Font font, float maxWidth) throws IOException {
        List<String> result = new ArrayList<>();
        for (String raw : rawLines) {
            String[] words = raw.split(" ", -1);
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                float width = font.getStringWidth(sanitize(candidate)) / 1000 * FONT_SIZE;
                if (width > maxWidth && !current.isEmpty()) {
                    result.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            result.add(current.toString());
        }
        return result;
    }

    private String sanitize(String text) {
        return text.chars()
                .filter(c -> c >= 0x20 && c <= 0xFF)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private PDPage newPage(PDDocument doc) {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        return page;
    }
}

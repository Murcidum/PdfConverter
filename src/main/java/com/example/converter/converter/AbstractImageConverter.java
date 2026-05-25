package com.example.converter.converter;

import com.example.converter.exception.ConversionException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class AbstractImageConverter implements FileConverter {

    @Override
    public byte[] convert(byte[] fileContent) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDImageXObject image = PDImageXObject.createFromByteArray(doc, fileContent, "image");

            float pageWidth = PDRectangle.A4.getWidth();
            float pageHeight = PDRectangle.A4.getHeight();
            float scale = Math.min(pageWidth / image.getWidth(), pageHeight / image.getHeight());
            float scaledWidth = image.getWidth() * scale;
            float scaledHeight = image.getHeight() * scale;
            float x = (pageWidth - scaledWidth) / 2;
            float y = (pageHeight - scaledHeight) / 2;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.drawImage(image, x, y, scaledWidth, scaledHeight);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ConversionException("Failed to convert image to PDF: " + getSupportedExtension(), e);
        }
    }
}

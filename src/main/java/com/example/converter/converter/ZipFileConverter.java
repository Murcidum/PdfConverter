package com.example.converter.converter;

import com.example.converter.exception.ConversionException;
import com.example.converter.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Component
public class ZipFileConverter implements FileConverter {

    private Map<String, FileConverter> converters;

    @Autowired
    public void setConverters(List<FileConverter> converterList) {
        this.converters = converterList.stream()
                .filter(c -> !(c instanceof ZipFileConverter))
                .collect(Collectors.toMap(FileConverter::getSupportedExtension, Function.identity()));
    }

    @Override
    public String getSupportedExtension() {
        return "zip";
    }

    @Override
    public byte[] convert(byte[] fileContent) {
        List<byte[]> convertedPdfs = new ArrayList<>();

        try (ZipInputStream zis = openZipInputStream(fileContent)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }
                String ext = FileUtils.extractExtension(entry.getName());
                FileConverter converter = converters.get(ext);
                if (converter == null) {
                    log.warn("No converter for '{}' inside ZIP, skipping", entry.getName());
                    zis.closeEntry();
                    continue;
                }
                byte[] entryBytes = zis.readAllBytes();
                log.info("Converting ZIP entry: {}", entry.getName());
                convertedPdfs.add(converter.convert(entryBytes));
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new ConversionException("Failed to read ZIP archive", e);
        }

        if (convertedPdfs.isEmpty()) {
            return emptyPdf();
        }

        return mergePdfs(convertedPdfs);
    }

    private ZipInputStream openZipInputStream(byte[] content) {
        try {
            ZipInputStream probe = new ZipInputStream(new ByteArrayInputStream(content), StandardCharsets.UTF_8);
            probe.getNextEntry();
            probe.close();
            return new ZipInputStream(new ByteArrayInputStream(content), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.debug("ZIP entry names are not valid UTF-8, retrying with CP866");
            return new ZipInputStream(new ByteArrayInputStream(content), Charset.forName("CP866"));
        }
    }

    private byte[] mergePdfs(List<byte[]> pdfs) {
        try {
            PDFMergerUtility merger = new PDFMergerUtility();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            merger.setDestinationStream(baos);
            for (byte[] pdfBytes : pdfs) {
                merger.addSource(new RandomAccessReadBuffer(pdfBytes));
            }
            merger.mergeDocuments(null);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ConversionException("Failed to merge PDFs from ZIP", e);
        }
    }

    private byte[] emptyPdf() {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("ZIP archive contained no convertible files.");
                cs.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ConversionException("Failed to create empty PDF", e);
        }
    }

}

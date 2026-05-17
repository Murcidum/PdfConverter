package com.example.converter.converter;

import org.springframework.stereotype.Component;

@Component
public class PngFileConverter extends AbstractImageConverter {

    @Override
    public String getSupportedExtension() {
        return "png";
    }
}

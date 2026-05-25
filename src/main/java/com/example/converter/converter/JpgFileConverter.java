package com.example.converter.converter;

import org.springframework.stereotype.Component;

@Component
public class JpgFileConverter extends AbstractImageConverter {

    @Override
    public String getSupportedExtension() {
        return "jpg";
    }
}

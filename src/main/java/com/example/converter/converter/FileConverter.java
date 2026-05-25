package com.example.converter.converter;

public interface FileConverter {

    String getSupportedExtension();

    byte[] convert(byte[] fileContent);
}

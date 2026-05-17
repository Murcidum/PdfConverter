package com.example.converter.config;

import com.example.converter.converter.FileConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class ConverterConfig {

    @Bean
    public Map<String, FileConverter> fileConverters(List<FileConverter> converters) {
        return converters.stream()
                .collect(Collectors.toMap(FileConverter::getSupportedExtension, Function.identity()));
    }
}

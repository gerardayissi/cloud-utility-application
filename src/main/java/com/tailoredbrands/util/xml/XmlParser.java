package com.tailoredbrands.util.xml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

import java.io.IOException;

public class XmlParser {
    private static final ObjectMapper MAPPER = new XmlMapper().registerModule(new JaxbAnnotationModule());

    public static <T> T deserialize(String xml, Class<T> clazz) {
        try {
            return MAPPER.readValue(xml, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

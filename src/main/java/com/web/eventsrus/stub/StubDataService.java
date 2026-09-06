package com.web.eventsrus.stub;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads JSON files from src/main/resources/stubs/ and deserializes them into
 * the same model classes that will eventually hold real eventsrus-backend
 * API responses. This is the one class that goes away (replaced by a real
 * HTTP client) once the backend integration starts — every controller using
 * it today should need to change only its data source, not its models or
 * templates.
 */
@Service
public class StubDataService {

    private final ObjectMapper objectMapper;

    public StubDataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T load(String stubFileName, Class<T> type) {
        ClassPathResource resource = new ClassPathResource("stubs/" + stubFileName);
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load stub data: " + stubFileName, e);
        }
    }

    public <T> List<T> loadList(String stubFileName, Class<T> elementType) {
        ClassPathResource resource = new ClassPathResource("stubs/" + stubFileName);
        try (InputStream in = resource.getInputStream()) {
            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
            return objectMapper.readValue(in, listType);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load stub data: " + stubFileName, e);
        }
    }

    /** Loads a JSON object of {@code "key": [ ... ]} into a {@code Map<String, List<V>>}. */
    public <V> Map<String, List<V>> loadMapOfLists(String stubFileName, Class<V> elementType) {
        ClassPathResource resource = new ClassPathResource("stubs/" + stubFileName);
        try (InputStream in = resource.getInputStream()) {
            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
            JavaType mapType = objectMapper.getTypeFactory().constructMapType(Map.class,
                    objectMapper.getTypeFactory().constructType(String.class), listType);
            return objectMapper.readValue(in, mapType);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load stub data: " + stubFileName, e);
        }
    }
}

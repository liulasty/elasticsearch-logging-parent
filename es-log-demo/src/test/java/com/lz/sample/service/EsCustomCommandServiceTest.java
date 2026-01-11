package com.lz.sample.service;

import com.lz.sample.es.SimpleEsWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EsCustomCommandServiceTest {

    @Mock
    private SimpleEsWriter simpleEsWriter;

    @InjectMocks
    private EsCustomCommandService esCustomCommandService;

    @Test
    public void testListIndices() throws IOException {
        String options = "v&s=index";
        when(simpleEsWriter.executeRequest(eq("GET"), eq("/_cat/indices?" + options), eq(null)))
                .thenReturn("mock response");

        esCustomCommandService.listIndices(options);

        verify(simpleEsWriter).executeRequest(eq("GET"), eq("/_cat/indices?" + options), eq(null));
    }

    @Test
    public void testCreateIndex() throws IOException {
        String indexName = "my-custom-index";
        when(simpleEsWriter.executeRequest(eq("PUT"), eq("/" + indexName), eq(null)))
                .thenReturn("mock response");

        esCustomCommandService.createIndex(indexName);

        verify(simpleEsWriter).executeRequest(eq("PUT"), eq("/" + indexName), eq(null));
    }

    @Test
    public void testWriteData() throws IOException {
        String indexName = "my-custom-index";
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        
        when(simpleEsWriter.executeRequest(eq("POST"), eq("/" + indexName + "/_doc"), anyString()))
                .thenReturn("mock response");
        
        esCustomCommandService.writeData(indexName, data);

        verify(simpleEsWriter).executeRequest(eq("POST"), eq("/" + indexName + "/_doc"), anyString());
    }

    @Test
    public void testSearchData() throws IOException {
        String indexName = "my-custom-index";
        String query = "{\"query\": {\"match_all\": {}}}";
        
        when(simpleEsWriter.executeRequest(eq("GET"), eq("/" + indexName + "/_search"), eq(query)))
                .thenReturn("mock response");
        
        esCustomCommandService.searchData(indexName, query);

        verify(simpleEsWriter).executeRequest(eq("GET"), eq("/" + indexName + "/_search"), eq(query));
    }

    @Test
    public void testDeleteIndex() throws IOException {
        String indexName = "my-custom-index";
        when(simpleEsWriter.executeRequest(eq("DELETE"), eq("/" + indexName), eq(null)))
                .thenReturn("mock response");

        esCustomCommandService.deleteIndex(indexName);

        verify(simpleEsWriter).executeRequest(eq("DELETE"), eq("/" + indexName), eq(null));
    }
}

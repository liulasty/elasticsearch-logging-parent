package com.lz.sample.service;

import com.lz.sample.entry.LogEntry;
import com.lz.sample.es.SimpleEsWriter;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class LogStorageServiceTest {

    @Test
    public void flushQueueToElasticsearch_emptyQueue_doesNotWrite() {
        SimpleEsWriter esWriter = Mockito.mock(SimpleEsWriter.class);
        LogStorageService service = new LogStorageService(esWriter, "log", 10, 100);

        service.flushQueueToElasticsearch();

        verify(esWriter, never()).bulkWrite(anyString(), anyList());
    }

    @Test
    public void addLogToQueue_null_doesNotWriteOnFlush() {
        SimpleEsWriter esWriter = Mockito.mock(SimpleEsWriter.class);
        LogStorageService service = new LogStorageService(esWriter, "log", 10, 100);

        service.addLogToQueue(null);
        service.flushQueueToElasticsearch();

        verify(esWriter, never()).bulkWrite(anyString(), anyList());
    }

    @Test
    public void addLogToQueue_thenFlush_callsBulkWriteWithExpectedDocs() {
        SimpleEsWriter esWriter = Mockito.mock(SimpleEsWriter.class);
        when(esWriter.bulkWrite(anyString(), anyList())).thenReturn(true);

        LogStorageService service = new LogStorageService(esWriter, "my-index", 10, 100);
        service.addLogToQueue(new LogEntry("INFO", "hello"));
        service.addLogToQueue(new LogEntry("WARN", "world"));

        service.flushQueueToElasticsearch();

        ArgumentCaptor<List> docsCaptor = ArgumentCaptor.forClass(List.class);
        verify(esWriter, times(1)).bulkWrite(eq("my-index"), docsCaptor.capture());

        List docs = docsCaptor.getValue();
        assertThat(docs).hasSize(2);

        for (Object docObj : docs) {
            assertThat(docObj).isInstanceOf(Map.class);
            Map<String, Object> doc = (Map<String, Object>) docObj;

            assertThat(doc).containsKeys("@timestamp", "level", "message", "app_name");
            assertThat(doc.get("app_name")).isEqualTo("es-log-demo");

            Object timestamp = doc.get("@timestamp");
            assertThat(timestamp).isInstanceOf(String.class);
            Instant.parse((String) timestamp);
        }
    }

    @Test
    public void flushQueueToElasticsearch_batchSizeZero_usesDefaultBatchSize() {
        SimpleEsWriter esWriter = Mockito.mock(SimpleEsWriter.class);
        when(esWriter.bulkWrite(anyString(), anyList())).thenReturn(true);

        LogStorageService service = new LogStorageService(esWriter, "log", 0, 1000);

        for (int i = 0; i < 250; i++) {
            service.addLogToQueue(new LogEntry("INFO", "m" + i));
        }

        service.flushQueueToElasticsearch();

        ArgumentCaptor<List> docsCaptor = ArgumentCaptor.forClass(List.class);
        verify(esWriter).bulkWrite(eq("log"), docsCaptor.capture());
        assertThat(docsCaptor.getValue()).hasSize(200);
    }

    @Test
    public void searchWithPagination_delegatesToWriter() {
        SimpleEsWriter esWriter = Mockito.mock(SimpleEsWriter.class);
        LogStorageService service = new LogStorageService(esWriter, "log", 10, 100);

        QueryBuilder query = QueryBuilders.matchAllQuery();
        SearchResponse response = Mockito.mock(SearchResponse.class);
        List<Map<String, Object>> expectedDocs = new ArrayList<>();
        expectedDocs.add(new HashMap<>());

        when(esWriter.searchWithPagination(eq("idx"), eq(query), eq(2), eq(10))).thenReturn(response);
        when(esWriter.getDocumentsFromResponse(eq(response))).thenReturn(expectedDocs);

        List<Map<String, Object>> docs = service.searchWithPagination("idx", query, 2, 10);
        assertThat(docs).isSameAs(expectedDocs);
    }

    @Test
    public void getTotalCount_returnsTotalHitsFromResponse() {
        SimpleEsWriter esWriter = Mockito.mock(SimpleEsWriter.class);
        LogStorageService service = new LogStorageService(esWriter, "log", 10, 100);

        SearchResponse response = Mockito.mock(SearchResponse.class);
        SearchHits hits = Mockito.mock(SearchHits.class);
        when(response.getHits()).thenReturn(hits);
        when(hits.getTotalHits()).thenReturn(new TotalHits(123L, TotalHits.Relation.EQUAL_TO));

        when(esWriter.searchWithPagination(eq("idx"), isNull(), eq(1), eq(1))).thenReturn(response);

        long total = service.getTotalCount("idx", null);
        assertThat(total).isEqualTo(123L);
    }

    @Test
    public void getAllIndices_delegates() throws IOException {
        SimpleEsWriter esWriter = Mockito.mock(SimpleEsWriter.class);
        LogStorageService service = new LogStorageService(esWriter, "log", 10, 100);

        List<String> indices = new ArrayList<>();
        indices.add("a");
        indices.add("b");
        when(esWriter.getAllIndices()).thenReturn(indices);

        assertThat(service.getAllIndices()).isSameAs(indices);
    }

    @Test
    public void getAllIndicesWithDetails_delegates() throws IOException {
        SimpleEsWriter esWriter = Mockito.mock(SimpleEsWriter.class);
        LogStorageService service = new LogStorageService(esWriter, "log", 10, 100);

        Map<String, Map<String, Object>> details = new HashMap<>();
        details.put("a", new HashMap<>());
        when(esWriter.getAllIndicesWithDetails()).thenReturn(details);

        assertThat(service.getAllIndicesWithDetails()).isSameAs(details);
    }
}


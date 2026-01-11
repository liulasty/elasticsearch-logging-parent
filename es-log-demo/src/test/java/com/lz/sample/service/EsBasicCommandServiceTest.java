package com.lz.sample.service;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EsBasicCommandServiceTest {

    @Mock
    private RestHighLevelClient restHighLevelClient;

    @Mock
    private RestClient restClient;

    @Mock
    private Response response;

    @Mock
    private StatusLine statusLine;

    @InjectMocks
    private EsBasicCommandService esBasicCommandService;

    @Before
    public void setUp() throws IOException {
        when(restHighLevelClient.getLowLevelClient()).thenReturn(restClient);
        when(restClient.performRequest(any(Request.class))).thenReturn(response);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        
        HttpEntity entity = new StringEntity("{\"status\":\"ok\"}");
        when(response.getEntity()).thenReturn(entity);
    }

    @Test
    public void testListIndices() throws IOException {
        esBasicCommandService.listIndices();

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(restClient).performRequest(requestCaptor.capture());
        Request request = requestCaptor.getValue();
        
        assertEquals("GET", request.getMethod());
        assertEquals("/_cat/indices?v", request.getEndpoint());
    }

    @Test
    public void testCreateTestIndex() throws IOException {
        esBasicCommandService.createTestIndex();

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(restClient).performRequest(requestCaptor.capture());
        Request request = requestCaptor.getValue();

        assertEquals("PUT", request.getMethod());
        assertEquals("/test_practice_001", request.getEndpoint());
    }

    @Test
    public void testWriteTestData() throws IOException {
        esBasicCommandService.writeTestData();

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(restClient).performRequest(requestCaptor.capture());
        Request request = requestCaptor.getValue();

        assertEquals("POST", request.getMethod());
        assertEquals("/test_practice_001/_doc", request.getEndpoint());
        // Could also verify the body content if needed
    }

    @Test
    public void testSearchTestData() throws IOException {
        esBasicCommandService.searchTestData();

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(restClient).performRequest(requestCaptor.capture());
        Request request = requestCaptor.getValue();

        assertEquals("GET", request.getMethod());
        assertEquals("/test_practice_001/_search", request.getEndpoint());
    }

    @Test
    public void testGetIndexDetails() throws IOException {
        esBasicCommandService.getIndexDetails();

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(restClient).performRequest(requestCaptor.capture());
        Request request = requestCaptor.getValue();

        assertEquals("GET", request.getMethod());
        assertEquals("/test_practice_001", request.getEndpoint());
    }

    @Test
    public void testDeleteTestIndex() throws IOException {
        esBasicCommandService.deleteTestIndex();

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(restClient).performRequest(requestCaptor.capture());
        Request request = requestCaptor.getValue();

        assertEquals("DELETE", request.getMethod());
        assertEquals("/test_practice_001", request.getEndpoint());
    }
}

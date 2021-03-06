/**
 * Copyright 2016 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy of 
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 * Some files may be comprised of various open source software components, each of which
 * has its own license that is located in the source code of the respective component.
 */
package com.vmware.loginsightapi;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.loginsightapi.core.AggregateResponse;
import com.vmware.loginsightapi.core.FieldConstraint;
import com.vmware.loginsightapi.core.IngestionRequest;
import com.vmware.loginsightapi.core.IngestionResponse;
import com.vmware.loginsightapi.core.LogInsightApiException;
import com.vmware.loginsightapi.core.LogInsightConnectionStrategy;
import com.vmware.loginsightapi.core.Message;
import com.vmware.loginsightapi.core.MessageQueryResponse;
import com.vmware.loginsightapi.util.AsyncLogInsightConnectionStrategy;

@RunWith(MockitoJUnitRunner.class)
public class LogInsightClientMockTest {

	private LogInsightClient client;
	private final static Logger logger = LoggerFactory.getLogger(LogInsightClientMockTest.class);

	private final static String SERVER_RESPONSE_EXPECTED = "{\"userId\":\"7506ecf5-cd7a-4ae3-88b7-f72fc1955c73\","
			+ "\"sessionId\":\"qyOLWEe7f/GjdM1WnczrCeQure97B/NpTbWTeqqYPBd1AYMf9cMNfQYqltITI4ffPMx822Sz9i/X47t8VwsDb0oGckclJUdn83cyIPk6WlsOpI4Yjw6WpurAnv9RhDsYSzKhAMzskzhTOJKfDHZjWR5v576WwtJA71wqI7igFrG91LG5c/3GfzMb68sUHF6hV+meYtGS4A1y/lUItvfkqTTAxBtTCZNoKrvCJZ4R+b6vuAAYoBNSWL7ycIy2LsALrVFxftAkA8n9DBAZYA9T5A==\",\"ttl\":1800}";
	private final static String SERVER_EXPECTED_QUERY_RESPONSE = "{\"complete\":true,\"duration\":57,\"events\":"
			+ "[{\"text\":\"log line 1\","
			+ "\"timestamp\":1432135888000,\"fields\":[{\"name\":\"field1\",\"content\":\"value1\"},"
			+ "{\"name\":\"field2\",\"content\":\"value2\"},{\"name\":\"field3\",\"content\":\"value3\"}]}]}";

	private final static String SERVER_EXPECTED_AGGREGATE_QUERY_RESPONSE = "{\"complete\":true,\"duration\":52,"
			+ "\"bins\":[{\"minTimestamp\":1432135885000,\"maxTimestamp\":1432135889999,\"value\":208515}]}";

	private final static String SERVER_EXPECTED_RESPONSE_FOR_INGESTION = "{\"status\":\"ok\",\"message\":\"messages ingested\",\"ingested\":1}";
	@Mock
	private LogInsightConnectionStrategy<CloseableHttpAsyncClient> connectionStrategy;
	@Mock
	private CloseableHttpAsyncClient asyncHttpClient;
	String host;
	String user;
	String password;

	// Properties connectionConfig = null;
	Configuration config;

	@Before
	public void setUp() {
		config = Configuration.buildFromConfig("config-mock.properties");
		when(connectionStrategy.getHttpClient()).thenReturn(asyncHttpClient);
		HttpResponse response = mock(HttpResponse.class);
		Future<HttpResponse> future = ConcurrentUtils.constantFuture(response);
		when(asyncHttpClient.execute(any(HttpUriRequest.class),any(FutureCallback.class))).thenReturn(future, null);
		HttpEntity httpEntity = mock(HttpEntity.class);
		when(response.getEntity()).thenReturn(httpEntity);
		StatusLine statusLine = mock(StatusLine.class);
		when(response.getStatusLine()).thenReturn(statusLine);
		when(statusLine.getStatusCode()).thenReturn(200);
		try {
			InputStream inputStream = IOUtils.toInputStream(SERVER_RESPONSE_EXPECTED, "UTF-8");
			when(httpEntity.getContent()).thenReturn(inputStream);
			client = new LogInsightClient(config, connectionStrategy);
			// client.connect(user, password);
			assertEquals("Invalid session id!!",
					"qyOLWEe7f/GjdM1WnczrCeQure97B/NpTbWTeqqYPBd1AYMf9cMNfQYqltITI4ffPMx822Sz9i/X47t8VwsDb0oGckclJUdn83cyIPk6WlsOpI4Yjw6WpurAnv9RhDsYSzKhAMzskzhTOJKfDHZjWR5v576WwtJA71wqI7igFrG91LG5c/3GfzMb68sUHF6hV+meYtGS4A1y/lUItvfkqTTAxBtTCZNoKrvCJZ4R+b6vuAAYoBNSWL7ycIy2LsALrVFxftAkA8n9DBAZYA9T5A==",
					client.getSessionId());
		} catch (Exception e) {
			logger.error("Exception raised " + ExceptionUtils.getStackTrace(e));
		}
	}
	
	@Test
	public void testLogInsightConstructor() {
		HttpResponse response = mock(HttpResponse.class);
		Future<HttpResponse> future = ConcurrentUtils.constantFuture(response);
		when(asyncHttpClient.execute(any(HttpUriRequest.class),any(FutureCallback.class))).thenReturn(future, null);
		HttpEntity httpEntity = mock(HttpEntity.class);
		when(response.getEntity()).thenReturn(httpEntity);
		StatusLine statusLine = mock(StatusLine.class);
		when(response.getStatusLine()).thenReturn(statusLine);
		when(statusLine.getStatusCode()).thenReturn(200);
		AsyncLogInsightConnectionStrategy asyncConnectionStrategy = mock(AsyncLogInsightConnectionStrategy.class);
		when(asyncConnectionStrategy.getHttpClient()).thenReturn(asyncHttpClient);
		LogInsightClient client1 = null;
		try {
			InputStream inputStream = IOUtils.toInputStream(SERVER_RESPONSE_EXPECTED, "UTF-8");
			when(httpEntity.getContent()).thenReturn(inputStream);
			client1 = new LogInsightClient(config.getHost(), config.getUser(), "dummy-password");
			assertEquals("Invalid session id!!",
					"qyOLWEe7f/GjdM1WnczrCeQure97B/NpTbWTeqqYPBd1AYMf9cMNfQYqltITI4ffPMx822Sz9i/X47t8VwsDb0oGckclJUdn83cyIPk6WlsOpI4Yjw6WpurAnv9RhDsYSzKhAMzskzhTOJKfDHZjWR5v576WwtJA71wqI7igFrG91LG5c/3GfzMb68sUHF6hV+meYtGS4A1y/lUItvfkqTTAxBtTCZNoKrvCJZ4R+b6vuAAYoBNSWL7ycIy2LsALrVFxftAkA8n9DBAZYA9T5A==",
					client1.getSessionId());
		} catch (Exception e) {
			logger.error("Exception raised " + ExceptionUtils.getStackTrace(e));
		} finally {
			try {
				client1.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	public void testMessageQuery() {
		MessageQuery mqb = getMessageQueryForTest();
		testMessageQueryUrlAndHeaders(mqb);
		HttpResponse response = mock(HttpResponse.class);
		HttpEntity httpEntity = mock(HttpEntity.class);
		when(response.getEntity()).thenReturn(httpEntity);
		
		doAnswer(new Answer<Future<HttpResponse>>() {
			  @Override
		      public Future<HttpResponse> answer(InvocationOnMock invocation) {
		          @SuppressWarnings("unchecked")
				FutureCallback<HttpResponse> responseCallback = invocation.getArgumentAt(1, FutureCallback.class);
		          responseCallback.completed(response);
		          return null;
		      }})
		  .when(asyncHttpClient).execute(any(HttpUriRequest.class), any(FutureCallback.class));
		

		try {
			InputStream inputStream = IOUtils.toInputStream(SERVER_EXPECTED_QUERY_RESPONSE, "UTF-8");
			when(httpEntity.getContent()).thenReturn(inputStream);
			CompletableFuture<MessageQueryResponse> responseFuture = client.messageQuery(mqb.toUrlString());
				
			MessageQueryResponse messages = responseFuture.get(0, TimeUnit.MILLISECONDS);
			Assert.assertTrue("Invalid number of messages", messages.getEvents().size() <= 100);
		} catch (Exception e) {
			logger.error("Exception raised " + ExceptionUtils.getStackTrace(e));
			Assert.assertTrue(false);
		}
	}
	
	@Test
	public void testMessageQueryFailure() {
		MessageQuery mqb = getMessageQueryForTest();
		testMessageQueryUrlAndHeaders(mqb);
		HttpResponse response = mock(HttpResponse.class);
		HttpEntity httpEntity = mock(HttpEntity.class);
		when(response.getEntity()).thenReturn(httpEntity);
		
		doAnswer(new Answer<Future<HttpResponse>>() {
			  @Override
		      public Future<HttpResponse> answer(InvocationOnMock invocation) {
		          @SuppressWarnings("unchecked")
				FutureCallback<HttpResponse> responseCallback = invocation.getArgumentAt(1, FutureCallback.class);
		          responseCallback.completed(response);
		          return null;
		      }})
		  .when(asyncHttpClient).execute(any(HttpUriRequest.class), any(FutureCallback.class));
		

		try {
			when(httpEntity.getContent()).thenThrow(IOException.class);
			CompletableFuture<MessageQueryResponse> responseFuture = client.messageQuery(mqb.toUrlString());
			MessageQueryResponse messages = responseFuture.get(0, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			logger.error("Exception raised " + ExceptionUtils.getStackTrace(e));
			Assert.assertTrue(e.getCause() instanceof IOException);
		}
	}
	
	@Test
	public void testMessageQueryRuntimeFailure() {
		MessageQuery mqb = getMessageQueryForTest();
		testMessageQueryUrlAndHeaders(mqb);
		HttpResponse response = mock(HttpResponse.class);
		HttpEntity httpEntity = mock(HttpEntity.class);
		when(response.getEntity()).thenReturn(httpEntity);
		
		doAnswer(new Answer<Future<HttpResponse>>() {
			  @Override
		      public Future<HttpResponse> answer(InvocationOnMock invocation) {
		          @SuppressWarnings("unchecked")
				FutureCallback<HttpResponse> responseCallback = invocation.getArgumentAt(1, FutureCallback.class);
		          responseCallback.completed(response);
		          return null;
		      }})
		  .when(asyncHttpClient).execute(any(HttpUriRequest.class), any(FutureCallback.class));
		

		try {
			when(httpEntity.getContent()).thenThrow(Exception.class);
			CompletableFuture<MessageQueryResponse> responseFuture = client.messageQuery(mqb.toUrlString());
			MessageQueryResponse messages = responseFuture.get(0, TimeUnit.MILLISECONDS);
		} catch (ExecutionException e) {
			logger.error("Exception raised " + ExceptionUtils.getStackTrace(e));
			Assert.assertTrue(e.getCause() instanceof LogInsightApiException);
			Assert.assertEquals(e.getCause().getMessage(), "Message query failed");
		} catch (Exception e1) {
			Assert.assertTrue(false);
		}
	}
	
	@Test
	public void testMessageQueryCancelled() {
		MessageQuery mqb = getMessageQueryForTest();
		testMessageQueryUrlAndHeaders(mqb);
		
		doAnswer(new Answer<Future<HttpResponse>>() {
			  @Override
		      public Future<HttpResponse> answer(InvocationOnMock invocation) {
		          @SuppressWarnings("unchecked")
				FutureCallback<HttpResponse> responseCallback = invocation.getArgumentAt(1, FutureCallback.class);
		          responseCallback.cancelled();
		          return null;
		      }})
		  .when(asyncHttpClient).execute(any(HttpUriRequest.class), any(FutureCallback.class));
		

		try {
			CompletableFuture<MessageQueryResponse> responseFuture = client.messageQuery(mqb.toUrlString());
			responseFuture.get(0, TimeUnit.MILLISECONDS);
		} catch (ExecutionException e) {
			logger.error("Exception raised " + ExceptionUtils.getStackTrace(e));
			Assert.assertTrue(e.getCause() instanceof LogInsightApiException);
			Assert.assertEquals(e.getCause().getMessage(), "Cancelled message Query");
		} catch (Exception e) {
			Assert.assertTrue(false);
		}
	}
	
	@Test
	public void testMessageQueryFailedInCallback() {
		MessageQuery mqb = getMessageQueryForTest();
		testMessageQueryUrlAndHeaders(mqb);
		
		doAnswer(new Answer<Future<HttpResponse>>() {
			  @Override
		      public Future<HttpResponse> answer(InvocationOnMock invocation) {
		          @SuppressWarnings("unchecked")
				FutureCallback<HttpResponse> responseCallback = invocation.getArgumentAt(1, FutureCallback.class);
		          responseCallback.failed(new Exception());
		          return null;
		      }})
		  .when(asyncHttpClient).execute(any(HttpUriRequest.class), any(FutureCallback.class));
		

		try {
			CompletableFuture<MessageQueryResponse> responseFuture = client.messageQuery(mqb.toUrlString());
			responseFuture.get(0, TimeUnit.MILLISECONDS);
		} catch (ExecutionException e) {
			logger.error("Exception raised " + ExceptionUtils.getStackTrace(e));
			Assert.assertTrue(e.getCause() instanceof LogInsightApiException);
			Assert.assertEquals(e.getCause().getMessage(), "Failed message Query");
		} catch (Exception e) {
			Assert.assertTrue(false);
		}
	}
	
	private MessageQuery getMessageQueryForTest() {
		List<FieldConstraint> constraints = new ConstraintBuilder().eq("vclap_caseid", "1423244")
				.gt("timestamp", "0").build();
		return (MessageQuery) new MessageQuery().limit(100)
				.setConstraints(constraints);
	}
	
	private void testMessageQueryUrlAndHeaders(MessageQuery mqb) {
		HttpGet getRequest = client.getHttpRequest(mqb.toUrlString(), false);
		try {
			Assert.assertEquals("Request URI is malformed", new URI(
					"https://" + config.getHost() + ":443/api/v1/events/vclap_caseid/EQ+1423244/timestamp/GT+0"),
					getRequest.getURI());
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			Assert.assertTrue(false);
		}
		Header[] headers = getRequest.getAllHeaders();
		testRequestHeaders(headers);
	}
	
	private void testAggregateQueryUrlAndHeaders(AggregateQuery aqb) {
		HttpGet getRequest = client.getHttpRequest(aqb.toUrlString(), true);
		try {
			Assert.assertTrue("Request URI is malformed", getRequest.getURI().equals(new URI("https://"
					+ config.getHost() + ":443/api/v1/aggregated-events/vclap_caseid/EQ+1423244/timestamp/GT+0")));
		} catch (Exception e) {
			Assert.assertTrue(false);
		}
		Header[] headers = getRequest.getAllHeaders();
		testRequestHeaders(headers);
	}
	
	private void testIngestionQueryUrlAndHeaders(IngestionRequest ingestionRequest) {
		HttpPost postRequest = client.getIngestionHttpRequest(ingestionRequest);
		try {
			URI expectedURI = new URI("https://" + config.getHost() + ":" + config.getIngestionPort()
					+ "/api/v1/messages/ingest/" + LogInsightClient.DEFAULT_INGESTION_AGENT_ID);
			Assert.assertTrue("Request URI is malformed", postRequest.getURI().equals(expectedURI));
		} catch (Exception e) {
			Assert.assertTrue(false);
		}
		Header[] headers = postRequest.getAllHeaders();
		testPostRequestHeaders(headers);
	}
	
	private void testRequestHeaders(Header[] headers) {
		for (int i = 0; i < headers.length; i++) {
			String headerName = headers[i].getName();
			String headerValue = headers[i].getValue();
			if (headerName.equals("Content-Type")) {
				Assert.assertEquals("Wrong request header value", "application/json", headerValue);
			}

			if (headerName.equals("Accept")) {
				Assert.assertEquals("Wrong request header value", "application/json", headerValue);
			}

			if (headerName.equals("x-li-timestamp")) {
				Assert.assertNotNull("x-li-timestamp header is not set", headerValue);
			}

			if (headerName.equals("X-li-session-id")) {
				Assert.assertNotNull("X-li-session-id header is not set", headerValue);
			}
		}
	}
	
	private void testPostRequestHeaders(Header[] headers) {
		for (int i = 0; i < headers.length; i++) {
			String headerName = headers[i].getName();
			String headerValue = headers[i].getValue();
			if (headerName.equals("Content-Type")) {
				Assert.assertEquals("Wrong request header value", "application/json", headerValue);
			}

			if (headerName.equals("Accept")) {
				Assert.assertEquals("Wrong request header value", "application/json", headerValue);
			}
		}
	}


	@Test
	public void testAggregateQuery() {
		List<FieldConstraint> constraints =  new ConstraintBuilder().eq("vclap_caseid", "1423244")
				.gt("timestamp", "0").build();
		AggregateQuery aqb = (AggregateQuery) new AggregateQuery().limit(100)
				.setConstraints(constraints);
		testAggregateQueryUrlAndHeaders(aqb);
		HttpResponse response = mock(HttpResponse.class);
		HttpEntity httpEntity = mock(HttpEntity.class);
		when(response.getEntity()).thenReturn(httpEntity);

		doAnswer(new Answer<Future<HttpResponse>>() {
			  @Override
		      public Future<HttpResponse> answer(InvocationOnMock invocation) {
		          @SuppressWarnings("unchecked")
				FutureCallback<HttpResponse> responseCallback = invocation.getArgumentAt(1, FutureCallback.class);
		          responseCallback.completed(response);
		          return null;
		      }})
		  .when(asyncHttpClient).execute(any(HttpUriRequest.class), any(FutureCallback.class));

		try {
			InputStream inputStream = IOUtils.toInputStream(SERVER_EXPECTED_AGGREGATE_QUERY_RESPONSE, "UTF-8");
			when(httpEntity.getContent()).thenReturn(inputStream);
			CompletableFuture<AggregateResponse> responseFuture = client.aggregateQuery(aqb.toUrlString());
			
			AggregateResponse message = responseFuture.get(0, TimeUnit.MILLISECONDS);
			
			Assert.assertTrue("Invalid number of bins", message.getBins().size() <= 100);
			Assert.assertTrue("Invalid duration in the response", message.getDuration() > 0);
		} catch (Exception e) {
			logger.error("Exception raised " + ExceptionUtils.getStackTrace(e));
			Assert.assertTrue(false);
		}
	}
	
	@Test
	public void testAggregateQueryFailure() {
		List<FieldConstraint> constraints =  new ConstraintBuilder().eq("vclap_caseid", "1423244")
				.gt("timestamp", "0").build();
		AggregateQuery aqb = (AggregateQuery) new AggregateQuery().limit(100)
				.setConstraints(constraints);
		testAggregateQueryUrlAndHeaders(aqb);
		HttpResponse response = mock(HttpResponse.class);
		HttpEntity httpEntity = mock(HttpEntity.class);
		when(response.getEntity()).thenReturn(httpEntity);

		doAnswer(new Answer<Future<HttpResponse>>() {
			  @Override
		      public Future<HttpResponse> answer(InvocationOnMock invocation) {
		          @SuppressWarnings("unchecked")
				FutureCallback<HttpResponse> responseCallback = invocation.getArgumentAt(1, FutureCallback.class);
		          responseCallback.completed(response);
		          return null;
		      }})
		  .when(asyncHttpClient).execute(any(HttpUriRequest.class), any(FutureCallback.class));

		try {
			InputStream inputStream = IOUtils.toInputStream(SERVER_EXPECTED_AGGREGATE_QUERY_RESPONSE, "UTF-8");
			when(httpEntity.getContent()).thenThrow(IOException.class);
			CompletableFuture<AggregateResponse> responseFuture = client.aggregateQuery(aqb.toUrlString());
			responseFuture.get(0, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			logger.error("Exception raised " + ExceptionUtils.getStackTrace(e));
			Assert.assertTrue(e.getCause() instanceof LogInsightApiException);
			Assert.assertEquals(e.getCause().getMessage(), "Unable to process the query response");
		}
	}
	
	@Test
	public void testAggregateQueryRuntimeFailure() {
		List<FieldConstraint> constraints =  new ConstraintBuilder().eq("vclap_caseid", "1423244")
				.gt("timestamp", "0").build();
		AggregateQuery aqb = (AggregateQuery) new AggregateQuery().limit(100)
				.setConstraints(constraints);
		testAggregateQueryUrlAndHeaders(aqb);
		HttpResponse response = mock(HttpResponse.class);
		HttpEntity httpEntity = mock(HttpEntity.class);
		when(response.getEntity()).thenReturn(httpEntity);

		doAnswer(new Answer<Future<HttpResponse>>() {
			  @Override
		      public Future<HttpResponse> answer(InvocationOnMock invocation) {
		          @SuppressWarnings("unchecked")
				FutureCallback<HttpResponse> responseCallback = invocation.getArgumentAt(1, FutureCallback.class);
		          responseCallback.completed(response);
		          return null;
		      }})
		  .when(asyncHttpClient).execute(any(HttpUriRequest.class), any(FutureCallback.class));

		try {
			InputStream inputStream = IOUtils.toInputStream(SERVER_EXPECTED_AGGREGATE_QUERY_RESPONSE, "UTF-8");
			when(httpEntity.getContent()).thenThrow(Exception.class);
			CompletableFuture<AggregateResponse> responseFuture = client.aggregateQuery(aqb.toUrlString());
			responseFuture.get(0, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			logger.error("Exception raised " + ExceptionUtils.getStackTrace(e));
			Assert.assertTrue(e.getCause() instanceof LogInsightApiException);
			Assert.assertEquals(e.getCause().getMessage(), "Message query failed");
		}
	}

	
	@Test
	public void testAggregateQueryCancelled() {
		List<FieldConstraint> constraints =  new ConstraintBuilder().eq("vclap_caseid", "1423244")
				.gt("timestamp", "0").build();
		AggregateQuery aqb = (AggregateQuery) new AggregateQuery().limit(100)
				.setConstraints(constraints);
		testAggregateQueryUrlAndHeaders(aqb);

		doAnswer(new Answer<Future<HttpResponse>>() {
			  @Override
		      public Future<HttpResponse> answer(InvocationOnMock invocation) {
		          @SuppressWarnings("unchecked")
				FutureCallback<HttpResponse> responseCallback = invocation.getArgumentAt(1, FutureCallback.class);
		          responseCallback.cancelled();
		          return null;
		      }})
		  .when(asyncHttpClient).execute(any(HttpUriRequest.class), any(FutureCallback.class));

		try {
			CompletableFuture<AggregateResponse> responseFuture = client.aggregateQuery(aqb.toUrlString());
			responseFuture.get(0, TimeUnit.MILLISECONDS);
		} catch (ExecutionException e) {
			logger.error("Exception raised " + ExceptionUtils.getStackTrace(e));
			Assert.assertTrue(e.getCause() instanceof LogInsightApiException);
			Assert.assertEquals(e.getCause().getMessage(), "Cancelled message Query");
		} catch (Exception e) {
			Assert.assertTrue(false);
		}
	}
	
	@Test
	public void testAggregateQueryFailedInCallback() {
		List<FieldConstraint> constraints =  new ConstraintBuilder().eq("vclap_caseid", "1423244")
				.gt("timestamp", "0").build();
		AggregateQuery aqb = (AggregateQuery) new AggregateQuery().limit(100)
				.setConstraints(constraints);
		testAggregateQueryUrlAndHeaders(aqb);

		doAnswer(new Answer<Future<HttpResponse>>() {
			  @Override
		      public Future<HttpResponse> answer(InvocationOnMock invocation) {
		          @SuppressWarnings("unchecked")
				FutureCallback<HttpResponse> responseCallback = invocation.getArgumentAt(1, FutureCallback.class);
		          responseCallback.failed(new Exception());
		          return null;
		      }})
		  .when(asyncHttpClient).execute(any(HttpUriRequest.class), any(FutureCallback.class));

		try {
			CompletableFuture<AggregateResponse> responseFuture = client.aggregateQuery(aqb.toUrlString());
			responseFuture.get(0, TimeUnit.MILLISECONDS);
		} catch (ExecutionException e) {
			logger.error("Exception raised " + ExceptionUtils.getStackTrace(e));
			Assert.assertTrue(e.getCause() instanceof LogInsightApiException);
			Assert.assertEquals(e.getCause().getMessage(), "Failed message Query");
		} catch (Exception e) {
			Assert.assertTrue(false);
		}
	}


	@Test
	public void testIngestion() {
		Message msg1 = new Message("Testing the ingestion");
		msg1.addField("vclap_test_id", "11111");
		IngestionRequest request = new IngestionRequest();
		request.addMessage(msg1);
		testIngestionQueryUrlAndHeaders(request);
		
		HttpResponse response = mock(HttpResponse.class);
		HttpEntity httpEntity = mock(HttpEntity.class);
		when(response.getEntity()).thenReturn(httpEntity);
		StatusLine statusLine = mock(StatusLine.class);
		when(response.getStatusLine()).thenReturn(statusLine);
		when(statusLine.getStatusCode()).thenReturn(200);
		
		doAnswer(new Answer<Future<HttpResponse>>() {
			  @Override
		      public Future<HttpResponse> answer(InvocationOnMock invocation) {
		          FutureCallback<HttpResponse> responseCallback = invocation.getArgumentAt(1, FutureCallback.class);
		          responseCallback.completed(response);
		          return null;
		      }})
		  .when(asyncHttpClient).execute(any(HttpUriRequest.class), any(FutureCallback.class));

		try {
			InputStream inputStream = IOUtils.toInputStream(SERVER_EXPECTED_RESPONSE_FOR_INGESTION, "UTF-8");
			when(httpEntity.getContent()).thenReturn(inputStream);
			CompletableFuture<IngestionResponse> responseFuture = client.ingest(request);
			Assert.assertTrue("Invalid status in ingestion response", "ok".equals(responseFuture.get().getStatus()));
		} catch (Exception e) {
			logger.error("Exception raised " + ExceptionUtils.getStackTrace(e));
			Assert.assertTrue(false);
		}
	}
	
	@Test
	public void testIngestionFailure() {
		Message msg1 = new Message("Testing the ingestion");
		msg1.addField("vclap_test_id", "11111");
		IngestionRequest request = new IngestionRequest();
		request.addMessage(msg1);
		testIngestionQueryUrlAndHeaders(request);
		
		HttpResponse response = mock(HttpResponse.class);
		HttpEntity httpEntity = mock(HttpEntity.class);
		when(response.getEntity()).thenReturn(httpEntity);
		
		doAnswer(new Answer<Future<HttpResponse>>() {
			  @Override
		      public Future<HttpResponse> answer(InvocationOnMock invocation) {
		          FutureCallback<HttpResponse> responseCallback = invocation.getArgumentAt(1, FutureCallback.class);
		          responseCallback.completed(response);
		          return null;
		      }})
		  .when(asyncHttpClient).execute(any(HttpUriRequest.class), any(FutureCallback.class));

		try {
			when(httpEntity.getContent()).thenThrow(IOException.class);
			CompletableFuture<IngestionResponse> responseFuture = client.ingest(request);
		} catch (Exception e) {
			logger.error("Exception raised " + ExceptionUtils.getStackTrace(e));
			Assert.assertTrue(e.getCause() instanceof LogInsightApiException);
			Assert.assertEquals(e.getCause().getMessage(), "Unable to process the query response");
		}
	}
	
	@Test
	public void testIngestionRuntimeFailure() {
		Message msg1 = new Message("Testing the ingestion");
		msg1.addField("vclap_test_id", "11111");
		IngestionRequest request = new IngestionRequest();
		request.addMessage(msg1);
		testIngestionQueryUrlAndHeaders(request);
		
		HttpResponse response = mock(HttpResponse.class);
		HttpEntity httpEntity = mock(HttpEntity.class);
		when(response.getEntity()).thenReturn(httpEntity);
		
		doAnswer(new Answer<Future<HttpResponse>>() {
			  @Override
		      public Future<HttpResponse> answer(InvocationOnMock invocation) {
		          FutureCallback<HttpResponse> responseCallback = invocation.getArgumentAt(1, FutureCallback.class);
		          responseCallback.completed(response);
		          return null;
		      }})
		  .when(asyncHttpClient).execute(any(HttpUriRequest.class), any(FutureCallback.class));

		try {
			when(httpEntity.getContent()).thenThrow(Exception.class);
			CompletableFuture<IngestionResponse> responseFuture = client.ingest(request);
			responseFuture.get(0, TimeUnit.MILLISECONDS);
		} catch (ExecutionException e) {
			logger.error("Exception raised " + ExceptionUtils.getStackTrace(e));
			Assert.assertTrue(e.getCause() instanceof LogInsightApiException);
			Assert.assertEquals(e.getCause().getMessage(), "Ingestion failed");
		} catch (Exception e1) {
			Assert.assertTrue(false);
		}
	}

	
	@Test
	public void testIngestionCancelled() {
		Message msg1 = new Message("Testing the ingestion");
		msg1.addField("vclap_test_id", "11111");
		IngestionRequest request = new IngestionRequest();
		request.addMessage(msg1);
		testIngestionQueryUrlAndHeaders(request);
		
		doAnswer(new Answer<Future<HttpResponse>>() {
			  @Override
		      public Future<HttpResponse> answer(InvocationOnMock invocation) {
		          FutureCallback<HttpResponse> responseCallback = invocation.getArgumentAt(1, FutureCallback.class);
		          responseCallback.cancelled();
		          return null;
		      }})
		  .when(asyncHttpClient).execute(any(HttpUriRequest.class), any(FutureCallback.class));

		try {
			CompletableFuture<IngestionResponse> responseFuture = client.ingest(request);
			responseFuture.get(0, TimeUnit.MILLISECONDS);
		} catch (ExecutionException e) {
			logger.error("Exception raised " + ExceptionUtils.getStackTrace(e));
			Assert.assertTrue(e.getCause() instanceof LogInsightApiException);
			Assert.assertEquals(e.getCause().getMessage(), "Ingestion cancelled");
		} catch (Exception e) {
			Assert.assertTrue(false);
		}
	}
	
	@Test
	public void testIngestionFailedInCallback() {
		Message msg1 = new Message("Testing the ingestion");
		msg1.addField("vclap_test_id", "11111");
		IngestionRequest request = new IngestionRequest();
		request.addMessage(msg1);
		testIngestionQueryUrlAndHeaders(request);
		
		doAnswer(new Answer<Future<HttpResponse>>() {
			  @Override
		      public Future<HttpResponse> answer(InvocationOnMock invocation) {
		          FutureCallback<HttpResponse> responseCallback = invocation.getArgumentAt(1, FutureCallback.class);
		          responseCallback.failed(new Exception());
		          return null;
		      }})
		  .when(asyncHttpClient).execute(any(HttpUriRequest.class), any(FutureCallback.class));

		try {
			CompletableFuture<IngestionResponse> responseFuture = client.ingest(request);
			responseFuture.get(0, TimeUnit.MILLISECONDS);
		} catch (ExecutionException e) {
			logger.error("Exception raised " + ExceptionUtils.getStackTrace(e));
			Assert.assertTrue(e.getCause() instanceof LogInsightApiException);
			Assert.assertEquals(e.getCause().getMessage(), "Ingestion failed");
		} catch (Exception e) {
			Assert.assertTrue(false);
		}
	}
}

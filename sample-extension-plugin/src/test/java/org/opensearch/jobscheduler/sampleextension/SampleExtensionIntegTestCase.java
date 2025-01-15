/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.jobscheduler.sampleextension;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.Assert;
import org.opensearch.client.Request;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.Response;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.WarningsHandler;
import org.opensearch.common.io.PathUtils;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.common.xcontent.LoggingDeprecationHandler;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.jobscheduler.spi.schedule.IntervalSchedule;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.test.rest.OpenSearchRestTestCase;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class SampleExtensionIntegTestCase extends OpenSearchRestTestCase {

    protected boolean isHttps() {
        boolean isHttps = Optional.ofNullable(System.getProperty("https")).map("true"::equalsIgnoreCase).orElse(false);
        if (isHttps) {
            // currently only external cluster is supported for security enabled testing
            if (!Optional.ofNullable(System.getProperty("tests.rest.cluster")).isPresent()) {
                throw new RuntimeException("cluster url should be provided for security enabled testing");
            }
        }

        return isHttps;
    }

    @Override
    protected String getProtocol() {
        return isHttps() ? "https" : "http";
    }

    @Override
    protected Settings restAdminSettings() {
        return Settings.builder()
            .put("http.port", 9200)
            .put("plugins.security.ssl.http.enabled", isHttps())
            .put("plugins.security.ssl.http.pemcert_filepath", "sample.pem")
            .put("plugins.security.ssl.http.keystore_filepath", "test-kirk.jks")
            .put("plugins.security.ssl.http.keystore_password", "changeit")
            .build();
        // return Settings.builder().put("strictDeprecationMode", false).put("http.port", 9200).build();
    }

    @Override
    protected RestClient buildClient(Settings settings, HttpHost[] hosts) throws IOException {
        boolean strictDeprecationMode = settings.getAsBoolean("strictDeprecationMode", true);
        RestClientBuilder builder = RestClient.builder(hosts);
        if (isHttps()) {
            String keystore = settings.get("plugins.security.ssl.http.keystore_filepath");
            if (Objects.nonNull(keystore)) {
                URI uri = null;
                try {
                    uri = this.getClass().getClassLoader().getResource("security/sample.pem").toURI();
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                Path configPath = PathUtils.get(uri).getParent().toAbsolutePath();
                return new SecureRestClientBuilder(settings, configPath).build();
            } else {
                configureHttpsClient(builder, settings);
                builder.setStrictDeprecationMode(strictDeprecationMode);
                return builder.build();
            }

        } else {
            configureClient(builder, settings);
            builder.setStrictDeprecationMode(strictDeprecationMode);
            return builder.build();
        }
    }

    protected static void configureHttpsClient(RestClientBuilder builder, Settings settings) throws IOException {
        Map<String, String> headers = ThreadContext.buildDefaultHeaders(settings);
        Header[] defaultHeaders = new Header[headers.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            defaultHeaders[i++] = new BasicHeader(entry.getKey(), entry.getValue());
        }
        builder.setDefaultHeaders(defaultHeaders);
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            String userName = Optional.ofNullable(System.getProperty("user"))
                .orElseThrow(() -> new RuntimeException("user name is missing"));
            String password = Optional.ofNullable(System.getProperty("password"))
                .orElseThrow(() -> new RuntimeException("password is missing"));
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, password));
            try {
                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                    // disable the certificate since our testing cluster just uses the default security configuration
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .setSSLContext(SSLContextBuilder.create().loadTrustMaterial(null, (chains, authType) -> true).build());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        final String socketTimeoutString = settings.get(CLIENT_SOCKET_TIMEOUT);
        final TimeValue socketTimeout = TimeValue.parseTimeValue(
            socketTimeoutString == null ? "60s" : socketTimeoutString,
            CLIENT_SOCKET_TIMEOUT
        );
        builder.setRequestConfigCallback(conf -> conf.setSocketTimeout(Math.toIntExact(socketTimeout.getMillis())));
        if (settings.hasValue(CLIENT_PATH_PREFIX)) {
            builder.setPathPrefix(settings.get(CLIENT_PATH_PREFIX));
        }
    }

    protected SampleJobParameter createWatcherJob(String jobId, SampleJobParameter jobParameter) throws IOException {
        return createWatcherJobWithClient(client(), jobId, jobParameter);
    }

    protected String createWatcherJobJson(String jobId, String jobParameter) throws IOException {
        return createWatcherJobJsonWithClient(client(), jobId, jobParameter);
    }

    protected SampleJobParameter createWatcherJobWithClient(RestClient client, String jobId, SampleJobParameter jobParameter)
        throws IOException {
        Map<String, String> params = getJobParameterAsMap(jobId, jobParameter);
        Response response = makeRequest(client, "POST", SampleExtensionRestHandler.WATCH_INDEX_URI, params, null);
        Assert.assertEquals("Unable to create a watcher job", RestStatus.OK, RestStatus.fromCode(response.getStatusLine().getStatusCode()));

        Map<String, Object> responseJson = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            response.getEntity().getContent()
        ).map();
        return getJobParameter(client, responseJson.get("_id").toString());
    }

    protected String createWatcherJobJsonWithClient(RestClient client, String jobId, String jobParameter) throws IOException {
        Response response = makeRequest(
            client,
            "PUT",
            "/" + SampleExtensionPlugin.JOB_INDEX_NAME + "/_doc/" + jobId + "?refresh",
            Collections.emptyMap(),
            new StringEntity(jobParameter, ContentType.APPLICATION_JSON)
        );
        Assert.assertEquals(
            "Unable to create a watcher job",
            RestStatus.CREATED,
            RestStatus.fromCode(response.getStatusLine().getStatusCode())
        );

        Map<String, Object> responseJson = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            response.getEntity().getContent()
        ).map();
        return responseJson.get("_id").toString();
    }

    protected void deleteWatcherJob(String jobId) throws IOException {
        deleteWatcherJobWithClient(client(), jobId);
    }

    protected void deleteWatcherJobWithClient(RestClient client, String jobId) throws IOException {
        Response response = makeRequest(
            client,
            "DELETE",
            SampleExtensionRestHandler.WATCH_INDEX_URI,
            Collections.singletonMap("id", jobId),
            null
        );

        Assert.assertEquals("Unable to delete a watcher job", RestStatus.OK, RestStatus.fromCode(response.getStatusLine().getStatusCode()));
    }

    protected Response makeRequest(
        RestClient client,
        String method,
        String endpoint,
        Map<String, String> params,
        HttpEntity entity,
        Header... headers
    ) throws IOException {
        Request request = new Request(method, endpoint);
        RequestOptions.Builder options = RequestOptions.DEFAULT.toBuilder();
        options.setWarningsHandler(WarningsHandler.PERMISSIVE);

        for (Header header : headers) {
            options.addHeader(header.getName(), header.getValue());
        }
        request.setOptions(options.build());
        request.addParameters(params);
        if (entity != null) {
            request.setEntity(entity);
        }
        return client.performRequest(request);
    }

    protected Map<String, String> getJobParameterAsMap(String jobId, SampleJobParameter jobParameter) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("id", jobId);
        params.put("job_name", jobParameter.getName());
        params.put("index", jobParameter.getIndexToWatch());
        params.put("interval", String.valueOf(((IntervalSchedule) jobParameter.getSchedule()).getInterval()));
        params.put("lock_duration_seconds", String.valueOf(jobParameter.getLockDurationSeconds()));
        return params;
    }

    @SuppressWarnings("unchecked")
    protected SampleJobParameter getJobParameter(RestClient client, String jobId) throws IOException {
        Request request = new Request("POST", "/" + SampleExtensionPlugin.JOB_INDEX_NAME + "/_search");
        String entity = "{\n"
            + "    \"query\": {\n"
            + "        \"match\": {\n"
            + "            \"_id\": {\n"
            + "                \"query\": \""
            + jobId
            + "\"\n"
            + "            }\n"
            + "        }\n"
            + "    }\n"
            + "}";
        request.setJsonEntity(entity);
        Response response = client.performRequest(request);
        Map<String, Object> responseJson = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            response.getEntity().getContent()
        ).map();
        Map<String, Object> hit = (Map<String, Object>) ((List<Object>) ((Map<String, Object>) responseJson.get("hits")).get("hits")).get(
            0
        );
        Map<String, Object> jobSource = (Map<String, Object>) hit.get("_source");

        SampleJobParameter jobParameter = new SampleJobParameter();
        jobParameter.setJobName(jobSource.get("name").toString());
        jobParameter.setIndexToWatch(jobSource.get("index_name_to_watch").toString());

        Map<String, Object> jobSchedule = (Map<String, Object>) jobSource.get("schedule");
        jobParameter.setSchedule(
            new IntervalSchedule(
                Instant.ofEpochMilli(Long.parseLong(((Map<String, Object>) jobSchedule.get("interval")).get("start_time").toString())),
                Integer.parseInt(((Map<String, Object>) jobSchedule.get("interval")).get("period").toString()),
                ChronoUnit.MINUTES
            )
        );
        jobParameter.setLockDurationSeconds(Long.parseLong(jobSource.get("lock_duration_seconds").toString()));
        return jobParameter;
    }

    protected String createTestIndex() throws IOException {
        String index = randomAlphaOfLength(10).toLowerCase(Locale.ROOT);
        createTestIndex(index);
        return index;
    }

    protected void createTestIndex(String index) throws IOException {
        createIndex(index, Settings.builder().put("index.number_of_shards", 2).put("index.number_of_replicas", 0).build());
    }

    protected void deleteTestIndex(String index) throws IOException {
        deleteIndex(index);
    }

    protected long countRecordsInTestIndex(String index) throws IOException {
        String entity = "{\n" + "    \"query\": {\n" + "        \"match_all\": {\n" + "        }\n" + "    }\n" + "}";
        Response response = makeRequest(
            client(),
            "POST",
            "/" + index + "/_count",
            Collections.emptyMap(),
            new StringEntity(entity, ContentType.APPLICATION_JSON)
        );
        Map<String, Object> responseJson = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            response.getEntity().getContent()
        ).map();
        return Integer.parseInt(responseJson.get("count").toString());
    }

    protected void waitAndCreateWatcherJob(String prevIndex, String jobId, SampleJobParameter jobParameter) {
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            private int timeoutCounter = 0;

            @Override
            public void run() {
                try {
                    long count = countRecordsInTestIndex(prevIndex);
                    ++timeoutCounter;
                    if (count == 1) {
                        createWatcherJob(jobId, jobParameter);
                        timer.cancel();
                        timer.purge();
                    }
                    if (timeoutCounter >= 24) {
                        timer.cancel();
                        timer.purge();
                    }
                } catch (IOException ex) {
                    // do nothing
                    // suppress exception
                }
            }
        };
        timer.scheduleAtFixedRate(timerTask, 2000, 5000);
    }

    protected void waitAndDeleteWatcherJob(String prevIndex, String jobId) {
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            private int timeoutCounter = 0;

            @Override
            public void run() {
                try {
                    long count = countRecordsInTestIndex(prevIndex);
                    ++timeoutCounter;
                    if (count == 1) {
                        deleteWatcherJob(jobId);
                        timer.cancel();
                        timer.purge();
                    }
                    if (timeoutCounter >= 24) {
                        timer.cancel();
                        timer.purge();
                    }
                } catch (IOException ex) {
                    // do nothing
                    // suppress exception
                }
            }
        };
        timer.scheduleAtFixedRate(timerTask, 2000, 5000);
    }

    protected long waitAndCountRecords(String index, long waitForInMs) throws Exception {
        Thread.sleep(waitForInMs);
        return countRecordsInTestIndex(index);
    }

    @SuppressWarnings("unchecked")
    protected long getLockTimeByJobId(String jobId) throws IOException {
        String entity = "{\n"
            + "    \"query\": {\n"
            + "        \"match\": {\n"
            + "            \"job_id\": {\n"
            + "                \"query\": \""
            + jobId
            + "\"\n"
            + "            }\n"
            + "        }\n"
            + "    }\n"
            + "}";
        Response response = makeRequest(
            client(),
            "POST",
            "/" + ".opendistro-job-scheduler-lock" + "/_search",
            Collections.emptyMap(),
            new StringEntity(entity, ContentType.APPLICATION_JSON)
        );
        Map<String, Object> responseJson = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            response.getEntity().getContent()
        ).map();
        List<Map<String, Object>> hits = (List<Map<String, Object>>) ((Map<String, Object>) responseJson.get("hits")).get("hits");
        if (hits.size() == 0) {
            return 0L;
        }
        Map<String, Object> lockSource = (Map<String, Object>) hits.get(0).get("_source");
        return Long.parseLong(lockSource.get("lock_time").toString());
    }

    @SuppressWarnings("unchecked")
    protected boolean doesLockExistByLockTime(long lockTime) throws IOException {
        String entity = "{\n"
            + "    \"query\": {\n"
            + "        \"match\": {\n"
            + "            \"lock_time\": {\n"
            + "                \"query\": "
            + lockTime
            + "\n"
            + "            }\n"
            + "        }\n"
            + "    }\n"
            + "}";
        Response response = makeRequest(
            client(),
            "POST",
            "/" + ".opendistro-job-scheduler-lock" + "/_search",
            Collections.emptyMap(),
            new StringEntity(entity, ContentType.APPLICATION_JSON)
        );
        Map<String, Object> responseJson = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            response.getEntity().getContent()
        ).map();
        List<Map<String, Object>> hits = (List<Map<String, Object>>) ((Map<String, Object>) responseJson.get("hits")).get("hits");
        return hits.size() == 1;
    }
}

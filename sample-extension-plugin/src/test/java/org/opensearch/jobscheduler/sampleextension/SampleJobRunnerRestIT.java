/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.jobscheduler.sampleextension;

import org.awaitility.core.ConditionTimeoutException;
import org.junit.Assert;
import org.opensearch.jobscheduler.spi.LockModel;
import org.opensearch.client.Response;
import org.opensearch.common.xcontent.LoggingDeprecationHandler;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.jobscheduler.spi.schedule.IntervalSchedule;
import org.opensearch.test.rest.OpenSearchRestTestCase;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import java.util.List;
import java.util.Map;

public class SampleJobRunnerRestIT extends SampleExtensionIntegTestCase {

    public void testJobCreateWithCorrectParams() throws IOException {
        SampleJobParameter jobParameter = new SampleJobParameter();
        jobParameter.setJobName("sample-job-it");
        jobParameter.setIndexToWatch("http-logs");
        jobParameter.setSchedule(new IntervalSchedule(Instant.now(), 5, ChronoUnit.SECONDS));
        jobParameter.setLockDurationSeconds(5L);

        // Creates a new watcher job.
        String jobId = OpenSearchRestTestCase.randomAlphaOfLength(10);
        SampleJobParameter schedJobParameter = createWatcherJob(jobId, jobParameter);

        // Asserts that job is created with correct parameters.
        Assert.assertEquals(jobParameter.getName(), schedJobParameter.getName());
        Assert.assertEquals(jobParameter.getIndexToWatch(), schedJobParameter.getIndexToWatch());
        Assert.assertEquals(jobParameter.getLockDurationSeconds(), schedJobParameter.getLockDurationSeconds());
    }

    public void testJobDeleteWithDescheduleJob() throws Exception {
        String index = createTestIndex();
        SampleJobParameter jobParameter = new SampleJobParameter();
        jobParameter.setJobName("sample-job-it");
        jobParameter.setIndexToWatch(index);
        jobParameter.setSchedule(new IntervalSchedule(Instant.now(), 5, ChronoUnit.SECONDS));
        jobParameter.setLockDurationSeconds(5L);

        // Creates a new watcher job.
        String jobId = OpenSearchRestTestCase.randomAlphaOfLength(10);
        SampleJobParameter schedJobParameter = createWatcherJob(jobId, jobParameter);

        waitUntilLockIsAcquiredAndReleased(jobId);

        // wait till the job runner runs for the first time after 5s & inserts a record into the watched index & then delete the job.
        deleteWatcherJob(jobId);

        // ensure log remains released as job is now descheduled
        assertThrows(
            ConditionTimeoutException.class,
            () -> await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).ignoreExceptions().until(() -> {
                LockModel lock = getLockByJobId(jobId);
                if (lock != null && !lock.isReleased()) {
                    Assert.fail("Lock should not be acquired after job deletion");
                }
                return false;
            })
        );

        long actualCount = countRecordsInTestIndex(index);

        // Asserts that in the last 10s, no new job ran to insert a record into the watched index & all locks are deleted for the job.
        Assert.assertEquals(1, actualCount);
    }

    public void testJobRunThenDisable() throws Exception {
        String index = createTestIndex();
        SampleJobParameter jobParameter = new SampleJobParameter();
        jobParameter.setJobName("sample-job-it");
        jobParameter.setIndexToWatch(index);
        jobParameter.setSchedule(new IntervalSchedule(Instant.now(), 5, ChronoUnit.SECONDS));
        jobParameter.setLockDurationSeconds(5L);

        // Creates a new watcher job.
        String jobId = OpenSearchRestTestCase.randomAlphaOfLength(10);
        SampleJobParameter schedJobParameter = createWatcherJob(jobId, jobParameter);

        waitUntilLockIsAcquiredAndReleased(jobId);

        // wait till the job runner runs for the first time after 5s & inserts a record into the watched index & then delete the job.
        disableWatcherJob(jobId, jobParameter);

        // ensure log remains released as job is now descheduled
        assertThrows(
            ConditionTimeoutException.class,
            () -> await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).ignoreExceptions().until(() -> {
                LockModel lock = getLockByJobId(jobId);
                if (lock != null && !lock.isReleased()) {
                    Assert.fail("Lock should not be acquired after job deletion");
                }
                return false;
            })
        );

        long actualCount = countRecordsInTestIndex(index);

        // Asserts that in the last 10s, no new job ran to insert a record into the watched index & all locks are deleted for the job.
        Assert.assertEquals(1, actualCount);
    }

    public void testJobUpdateWithRescheduleJob() throws Exception {
        String index = createTestIndex();
        SampleJobParameter jobParameter = new SampleJobParameter();
        jobParameter.setJobName("sample-job-it");
        jobParameter.setIndexToWatch(index);
        jobParameter.setSchedule(new IntervalSchedule(Instant.now(), 5, ChronoUnit.SECONDS));
        jobParameter.setLockDurationSeconds(5L);

        // Creates a new watcher job.
        String jobId = OpenSearchRestTestCase.randomAlphaOfLength(10);
        createWatcherJob(jobId, jobParameter);

        waitUntilLockIsAcquiredAndReleased(jobId);
        Assert.assertEquals(1, countRecordsInTestIndex(index));
        // update the job params to now watch a new index.
        String newIndex = createTestIndex();
        jobParameter.setIndexToWatch(newIndex);

        // wait till the job runner runs for the first time after 1 min & inserts a record into the watched index & then update the job with
        // new params.
        createWatcherJob(jobId, jobParameter);
        waitUntilLockIsAcquiredAndReleased(jobId);

        // Asserts that the job runner has the updated params & it inserted the record in the new watched index.
        Assert.assertEquals(1, countRecordsInTestIndex(newIndex));

        // Asserts that the job runner no longer updates the old index as the job params have been updated.
        Assert.assertEquals(1, countRecordsInTestIndex(index));
    }

    public void testRunThenListJobs() throws Exception {

        String SCHEDULER_INFO_URI = "/_plugins/_job_scheduler/api/jobs?by_node";

        String index = createTestIndex();
        SampleJobParameter jobParameter = new SampleJobParameter();
        jobParameter.setJobName("sample-job-it");
        jobParameter.setIndexToWatch(index);
        jobParameter.setSchedule(new IntervalSchedule(Instant.now(), 5, ChronoUnit.SECONDS));
        jobParameter.setLockDurationSeconds(5L);

        for (int i = 0; i < 10; i++) {
            // Creates a new watcher job.
            String indexN = createTestIndex();
            SampleJobParameter jobParameterN = new SampleJobParameter();
            jobParameterN.setJobName("sample-job-it" + i);
            jobParameterN.setIndexToWatch(indexN);
            jobParameterN.setSchedule(new IntervalSchedule(Instant.now(), 1, ChronoUnit.MINUTES));
            jobParameterN.setLockDurationSeconds(120L);

            String jobIdN = OpenSearchRestTestCase.randomAlphaOfLength(10);
            createWatcherJob(jobIdN, jobParameterN);
        }

        // Creates a new watcher job.
        String jobId = OpenSearchRestTestCase.randomAlphaOfLength(10);
        createWatcherJob(jobId, jobParameter);

        waitUntilLockIsAcquiredAndReleased(jobId);
        Assert.assertEquals(1, countRecordsInTestIndex(index));

        Response response = makeRequest(client(), "GET", SCHEDULER_INFO_URI, Map.of(), null);
        Map<String, Object> responseJson = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            LoggingDeprecationHandler.INSTANCE,
            response.getEntity().getContent()
        ).map();

        List<Map<String, Object>> nodes = (List<Map<String, Object>>) responseJson.get("nodes");
        assertNotNull("Nodes list should not be null", nodes);
        assertEquals(11, responseJson.get("total_jobs"));
        assertEquals(0, ((List<?>) responseJson.get("failures")).size());
        assertFalse("Should have at least one node", nodes.isEmpty());
    }

    public void testAcquiredLockPreventExecOfTasks() throws Exception {
        String index = createTestIndex();
        SampleJobParameter jobParameter = new SampleJobParameter();
        jobParameter.setJobName("sample-job-lock-test-it");
        jobParameter.setIndexToWatch(index);
        // ensures that the next job tries to run even before the previous job finished & released its lock. Also look at
        // SampleJobRunner.runTaskForLockIntegrationTests
        jobParameter.setSchedule(new IntervalSchedule(Instant.now(), 5, ChronoUnit.SECONDS));
        jobParameter.setLockDurationSeconds(10L);

        // Creates a new watcher job.
        String jobId = OpenSearchRestTestCase.randomAlphaOfLength(10);
        createWatcherJob(jobId, jobParameter);

        long startTime = System.currentTimeMillis();

        waitUntilLockIsAcquiredAndReleased(jobId);

        long endTime = System.currentTimeMillis();

        long durationMs = endTime - startTime;
        Assert.assertTrue("Lock duration should be more than 10 seconds", durationMs > 10000);

        // Asserts that the job runner is running for the first time & it has inserted a new record into the watched index.
        Assert.assertEquals(1, countRecordsInTestIndex(index));

        waitUntilLockIsAcquiredAndReleased(jobId);
        Assert.assertEquals(2, countRecordsInTestIndex(index));
    }
}

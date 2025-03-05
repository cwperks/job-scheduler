/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.jobscheduler.sampleextension;

import org.junit.Assert;
import org.opensearch.jobscheduler.spi.schedule.IntervalSchedule;
import org.opensearch.test.rest.OpenSearchRestTestCase;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class SampleExtensionPluginRestIT extends SampleExtensionIntegTestCase {

    @SuppressWarnings("unchecked")
    public void testGetScheduledJobInfoByJobType() throws IOException {
        SampleJobParameter jobParameter = new SampleJobParameter();
        jobParameter.setJobName("sample-job-it");
        jobParameter.setIndexToWatch("http-logs");
        jobParameter.setSchedule(new IntervalSchedule(Instant.now(), 1, ChronoUnit.MINUTES));
        jobParameter.setLockDurationSeconds(120L);

        // Creates a new watcher job.
        String jobId = OpenSearchRestTestCase.randomAlphaOfLength(10);
        createWatcherJob(jobId, jobParameter);

        Map<String, Object> scheduledJobInfo = getScheduledJobInfoByJobType("scheduler_sample_extension");

        Assert.assertTrue(scheduledJobInfo.containsKey(jobId));

        Map<String, Object> jobInfo = (Map<String, Object>) scheduledJobInfo.get(jobId);

        Assert.assertTrue(jobInfo.containsKey("job_parameter"));
        Assert.assertEquals(jobInfo.get("index_name"), ".scheduler_sample_extension");
    }
}

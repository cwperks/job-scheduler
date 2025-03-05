/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.jobscheduler.scheduler;

import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.core.xcontent.ToXContentFragment;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.jobscheduler.spi.ScheduledJobParameter;
import org.opensearch.threadpool.Scheduler;

import java.io.IOException;
import java.time.Instant;

public class JobSchedulingInfo implements Writeable, ToXContentFragment {

    private String indexName;
    private String jobId;
    private ScheduledJobParameter jobParameter;
    private boolean descheduled = false;
    private Instant actualPreviousExecutionTime;
    private Instant expectedPreviousExecutionTime;
    private Instant expectedExecutionTime;
    private Scheduler.ScheduledCancellable scheduledCancellable;

    JobSchedulingInfo(String indexName, String jobId, ScheduledJobParameter jobParameter) {
        this.indexName = indexName;
        this.jobId = jobId;
        this.jobParameter = jobParameter;
    }

    public JobSchedulingInfo(StreamInput in) throws IOException {
        this.indexName = in.readString();
        this.jobId = in.readString();
        this.descheduled = in.readBoolean();
    }

    public String getIndexName() {
        return indexName;
    }

    public String getJobId() {
        return jobId;
    }

    public ScheduledJobParameter getJobParameter() {
        return jobParameter;
    }

    public boolean isDescheduled() {
        return descheduled;
    }

    public Instant getActualPreviousExecutionTime() {
        return actualPreviousExecutionTime;
    }

    public Instant getExpectedPreviousExecutionTime() {
        return expectedPreviousExecutionTime;
    }

    public Instant getExpectedExecutionTime() {
        return this.expectedExecutionTime;
    }

    public Scheduler.ScheduledCancellable getScheduledCancellable() {
        return scheduledCancellable;
    }

    public void setDescheduled(boolean descheduled) {
        this.descheduled = descheduled;
    }

    public void setActualPreviousExecutionTime(Instant actualPreviousExecutionTime) {
        this.actualPreviousExecutionTime = actualPreviousExecutionTime;
    }

    public void setExpectedPreviousExecutionTime(Instant expectedPreviousExecutionTime) {
        this.expectedPreviousExecutionTime = expectedPreviousExecutionTime;
    }

    public void setExpectedExecutionTime(Instant expectedExecutionTime) {
        this.expectedExecutionTime = expectedExecutionTime;
    }

    public void setScheduledCancellable(Scheduler.ScheduledCancellable scheduledCancellable) {
        this.scheduledCancellable = scheduledCancellable;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(this.indexName);
        out.writeString(this.jobId);
        out.writeOptionalWriteable(this.jobParameter);
        out.writeBoolean(this.descheduled);
        out.writeInstant(this.actualPreviousExecutionTime);
        out.writeInstant(this.expectedPreviousExecutionTime);
        out.writeInstant(this.expectedExecutionTime);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("index_name", this.indexName);
        builder.field("job_id", this.jobId);
        builder.field("job_parameter", this.jobParameter);
        builder.field("descheduled", this.descheduled);
        builder.field("actual_previous_execution_time", this.actualPreviousExecutionTime);
        builder.field("expected_previous_execution_time", this.expectedPreviousExecutionTime);
        builder.field("expected_execution_time", this.expectedExecutionTime);
        return builder.endObject();
    }
}

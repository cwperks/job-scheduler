/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.jobscheduler.spi;

import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.jobscheduler.spi.schedule.Schedule;
import org.opensearch.core.xcontent.ToXContentObject;

import java.io.IOException;
import java.time.Instant;

/**
 * Job parameters that being used by the JobScheduler.
 */
public interface ScheduledJobParameter extends ToXContentObject, Writeable {
    /**
     * @return job name.
     */
    String getName();

    /**
     * @return job last update time.
     */
    Instant getLastUpdateTime();

    /**
     * @return get job enabled time.
     */
    Instant getEnabledTime();

    /**
     * @return job schedule.
     */
    Schedule getSchedule();

    /**
     * @return true if job is enabled, false otherwise.
     */
    boolean isEnabled();

    /**
     * @return Null if scheduled job doesn't need lock. Seconds of lock duration if the scheduled job needs to be a singleton runner.
     */
    default Long getLockDurationSeconds() {
        return null;
    }

    /**
     * Job will be delayed randomly with range of (0, jitter)*interval for the
     * next execution time. For example, if next run is 10 minutes later, jitter
     * is 0.6, then next job run will be randomly delayed by 0 to 6 minutes.
     *
     * Jitter is percentage, so it should be positive and less than 1.
     * <p>
     * <b>Note:</b> default logic for these cases:
     * 1).If jitter is not set, will regard it as 0.0.
     * 2).If jitter is negative, will reset it as 0.0.
     * 3).If jitter exceeds jitter limit, will cap it as jitter limit. Default
     * jitter limit is 0.95. So if you set jitter as 0.96, will cap it as 0.95.
     *
     * @return job execution jitter
     */
    default Double getJitter() {
        return null;
    }

    @Override
    default void writeTo(StreamOutput out) throws IOException {
        out.writeString(getName());
        out.writeInstant(getLastUpdateTime());
        out.writeInstant(getEnabledTime());
        out.writeOptionalWriteable(getSchedule());
        out.writeBoolean(isEnabled());
        out.writeLong(getLockDurationSeconds());
        out.writeDouble(getJitter());
    }
}

/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.jobscheduler.transport.schedule;

import org.opensearch.core.action.ActionResponse;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.jobscheduler.scheduler.JobSchedulingInfo;

import java.io.IOException;
import java.util.Map;

/**
 * Response class used to facilitate serialization/deserialization of the GetLock response
 */
public class GetScheduleResponse extends ActionResponse implements ToXContentObject {

    private Map<String, JobSchedulingInfo> jobIdToInfoMap;

    public GetScheduleResponse(Map<String, JobSchedulingInfo> jobIdToInfoMap) {
        this.jobIdToInfoMap = jobIdToInfoMap;
    }

    public GetScheduleResponse(StreamInput in) throws IOException {
        super(in);
        jobIdToInfoMap = in.readMap(StreamInput::readString, JobSchedulingInfo::new);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.map(jobIdToInfoMap);
        return builder;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeMap(jobIdToInfoMap, StreamOutput::writeString, (o, s) -> s.writeTo(o));
    }
}

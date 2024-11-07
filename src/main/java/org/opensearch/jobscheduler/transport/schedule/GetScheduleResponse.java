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

import java.io.IOException;
import java.util.Map;

/**
 * Response class used to facilitate serialization/deserialization of the GetLock response
 */
public class GetScheduleResponse extends ActionResponse implements ToXContentObject {

    private Map<String, String> jobTypeToIndexMap;

    public GetScheduleResponse(Map<String, String> jobTypeToIndexMap) {
        this.jobTypeToIndexMap = jobTypeToIndexMap;
    }

    public GetScheduleResponse(StreamInput in) throws IOException {
        super(in);
        jobTypeToIndexMap = in.readMap(StreamInput::readString, StreamInput::readString);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("job_type_to_index_map", jobTypeToIndexMap);
        builder.endObject();
        return builder;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeMap(jobTypeToIndexMap, StreamOutput::writeString, StreamOutput::writeString);
    }
}

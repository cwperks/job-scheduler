/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.jobscheduler.transport.schedule;

import org.opensearch.action.support.master.AcknowledgedResponse;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ToXContentObject;

import java.io.IOException;

/**
 * Response class used to facilitate serialization/deserialization of the GetLock response
 */
public class GetScheduleResponse extends AcknowledgedResponse implements ToXContentObject {

    public GetScheduleResponse(boolean status) {
        super(status);
    }

    public GetScheduleResponse(StreamInput in) throws IOException {
        super(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
    }
}

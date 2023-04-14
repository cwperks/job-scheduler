/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.jobscheduler.rest.request;

import org.opensearch.action.ActionResponse;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;

import java.io.IOException;

public class GetJobDetailsResponse extends ActionResponse {

    private final String status;

    public GetJobDetailsResponse(String status) {
        this.status = status;
    }

    public GetJobDetailsResponse(StreamInput in) throws IOException {
        this.status = in.readString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(status);
    }

    public String getStatus() {
        return status;
    }
}

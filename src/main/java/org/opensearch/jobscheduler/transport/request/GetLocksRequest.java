/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.jobscheduler.transport.request;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;

import java.io.IOException;

public class GetLocksRequest extends ActionRequest {

    private String lockId;

    public GetLocksRequest() {
        super();
    }

    public GetLocksRequest(String lockId) {
        super();
        this.lockId = lockId;
    }

    public GetLocksRequest(StreamInput in) throws IOException {
        super(in);
        this.lockId = in.readOptionalString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeOptionalString(lockId);
    }

    public String getLockId() {
        return lockId;
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }
}

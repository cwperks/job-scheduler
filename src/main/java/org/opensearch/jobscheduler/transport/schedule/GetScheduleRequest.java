/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.jobscheduler.transport.schedule;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.core.xcontent.XContentParserUtils;

import java.io.IOException;
import java.util.Objects;

/**
 * Request to get schedule information for the given job type and job id
 */
public class GetScheduleRequest extends ActionRequest implements ToXContentObject {

    /**
     * the id of the job
     */
    private final String jobId;

    /**
     * the name of the job index
     */
    private final String jobType;

    public static final String JOB_ID = "job_id";
    public static final String JOB_TYPE = "job_type";

    /**
     * Instantiates a new GetScheduleRequest
     *
     * @param jobId (optional) the id of the job in which the lock will be given to
     * @param jobIndexName the name of the job index
     */
    public GetScheduleRequest(String jobId, String jobIndexName) {
        super();
        this.jobId = jobId;
        this.jobType = Objects.requireNonNull(jobIndexName);
    }

    /**
     * Instantiates a new GetScheduleRequest from {@link StreamInput}
     *
     * @param in is the byte stream input used to de-serialize the message.
     * @throws IOException IOException when message de-serialization fails.
     */
    public GetScheduleRequest(StreamInput in) throws IOException {
        super(in);
        this.jobType = in.readString();
        this.jobId = in.readOptionalString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(this.jobType);
        out.writeOptionalString(this.jobId);
    }

    public String getJobId() {
        return this.jobId;
    }

    public String getJobType() {
        return this.jobType;
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    public static GetScheduleRequest parse(XContentParser parser) throws IOException {

        String jobId = null;
        String jobType = null;

        XContentParserUtils.ensureExpectedToken(XContentParser.Token.START_OBJECT, parser.currentToken(), parser);
        while (parser.nextToken() != XContentParser.Token.END_OBJECT) {
            String fieldName = parser.currentName();
            parser.nextToken();

            switch (fieldName) {
                case JOB_ID:
                    jobId = parser.text();
                    break;
                case JOB_TYPE:
                    jobType = parser.text();
                    break;
                default:
                    parser.skipChildren();
                    break;
            }
        }
        return new GetScheduleRequest(jobId, jobType);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(JOB_TYPE, jobType);
        builder.field(JOB_ID, jobId);
        builder.endObject();
        return builder;
    }

}

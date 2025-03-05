/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.jobscheduler.rest.action;

import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.client.node.NodeClient;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.jobscheduler.JobSchedulerPlugin;
import org.opensearch.jobscheduler.transport.schedule.GetScheduleAction;
import org.opensearch.jobscheduler.transport.schedule.GetScheduleRequest;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static org.opensearch.core.xcontent.XContentParserUtils.ensureExpectedToken;
import static org.opensearch.rest.RestRequest.Method.POST;

/**
 * This class consists of the REST handler to GET schedule information given a job type and optionally a doc id
 */
public class RestGetScheduleAction extends BaseRestHandler {
    private final Logger logger = LogManager.getLogger(RestGetScheduleAction.class);

    public RestGetScheduleAction() {}

    @Override
    public String getName() {
        return "get_schedule_action";
    }

    @Override
    public List<Route> routes() {
        return ImmutableList.of(new Route(POST, String.format(Locale.ROOT, "%s/%s", JobSchedulerPlugin.JS_BASE_URI, "_schedule")));
    }

    @VisibleForTesting
    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        XContentParser parser = restRequest.contentParser();
        ensureExpectedToken(XContentParser.Token.START_OBJECT, parser.nextToken(), parser);

        // Deserialize get scheduler request
        GetScheduleRequest getScheduleRequest = GetScheduleRequest.parse(parser);
        // String jobId = getScheduleRequest.getJobId();
        // String jobType = getScheduleRequest.getJobType();

        return channel -> client.execute(GetScheduleAction.INSTANCE, getScheduleRequest, new RestToXContentListener<>(channel));
    }
}

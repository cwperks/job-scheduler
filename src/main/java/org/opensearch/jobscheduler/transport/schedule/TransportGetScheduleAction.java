/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.jobscheduler.transport.schedule;

import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.client.Client;
import org.opensearch.common.inject.Inject;
import org.opensearch.core.action.ActionListener;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

public class TransportGetScheduleAction extends HandledTransportAction<GetScheduleRequest, GetScheduleResponse> {

    private final Client client;
    private final ThreadPool threadPool;

    @Inject
    public TransportGetScheduleAction(
        final TransportService transportService,
        final ActionFilters actionFilters,
        final Client client,
        final ThreadPool threadPool
    ) {
        super(GetScheduleAction.NAME, transportService, actionFilters, GetScheduleRequest::new);
        this.client = client;
        this.threadPool = threadPool;
    }

    @Override
    protected void doExecute(Task task, GetScheduleRequest request, ActionListener<GetScheduleResponse> actionListener) {
        actionListener.onResponse(new GetScheduleResponse(true));
    }
}

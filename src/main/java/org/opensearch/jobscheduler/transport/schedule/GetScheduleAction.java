/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.jobscheduler.transport.schedule;

import org.opensearch.action.ActionType;

public class GetScheduleAction extends ActionType<GetScheduleResponse> {
    public static final GetScheduleAction INSTANCE = new GetScheduleAction();
    public static final String NAME = "cluster:admin/opensearch/js/job/get";

    private GetScheduleAction() {
        super(NAME, GetScheduleResponse::new);
    }
}

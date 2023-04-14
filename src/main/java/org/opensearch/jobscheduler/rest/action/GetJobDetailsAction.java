/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.jobscheduler.rest.action;

import org.opensearch.action.ActionType;
import org.opensearch.jobscheduler.rest.request.GetJobDetailsResponse;

public class GetJobDetailsAction extends ActionType<GetJobDetailsResponse> {

    public static final GetJobDetailsAction INSTANCE = new GetJobDetailsAction();
    public static final String NAME = "cluster:js/register_job_details";

    private GetJobDetailsAction() {
        super(NAME, GetJobDetailsResponse::new);
    }
}

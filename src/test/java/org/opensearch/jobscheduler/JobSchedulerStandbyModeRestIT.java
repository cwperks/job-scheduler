/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.jobscheduler;

import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.ResponseException;

import java.util.Map;

public class JobSchedulerStandbyModeRestIT extends ODFERestTestCase {

    public void testStandbyModeBlocksLockMutationRestApi() throws Exception {
        try {
            setStandbyMode(true);

            ResponseException standbyException = expectThrows(
                ResponseException.class,
                () -> TestHelpers.makeRequest(
                    client(),
                    "GET",
                    TestHelpers.GET_LOCK_BASE_URI,
                    Map.of(),
                    TestHelpers.toHttpEntity(TestHelpers.generateAcquireLockRequestBody("standby-job-index", "standby-job-id")),
                    null
                )
            );
            assertEquals(403, standbyException.getResponse().getStatusLine().getStatusCode());

            setStandbyMode(false);

            Response response = TestHelpers.makeRequest(
                client(),
                "GET",
                TestHelpers.GET_LOCK_BASE_URI,
                Map.of(),
                TestHelpers.toHttpEntity(TestHelpers.generateAcquireLockRequestBody("active-job-index", "active-job-id")),
                null
            );
            assertEquals(200, response.getStatusLine().getStatusCode());
        } finally {
            clearStandbyMode();
        }
    }

    private void setStandbyMode(boolean standbyMode) throws Exception {
        Request request = new Request("PUT", "/_cluster/settings");
        request.setJsonEntity("{\"transient\":{\"" + JobSchedulerSettings.STANDBY_MODE.getKey() + "\":" + standbyMode + "}}");
        adminClient().performRequest(request);
    }

    private void clearStandbyMode() throws Exception {
        Request request = new Request("PUT", "/_cluster/settings");
        request.setJsonEntity("{\"transient\":{\"" + JobSchedulerSettings.STANDBY_MODE.getKey() + "\":null}}");
        adminClient().performRequest(request);
    }
}

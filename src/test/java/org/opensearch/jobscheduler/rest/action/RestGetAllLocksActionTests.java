/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.jobscheduler.rest.action;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.mockito.Mockito;
import org.opensearch.core.action.ActionListener;
import org.opensearch.jobscheduler.JobSchedulerPlugin;
import org.opensearch.jobscheduler.transport.action.GetAllLocksAction;
import org.opensearch.jobscheduler.transport.request.GetLocksRequest;
import org.opensearch.jobscheduler.transport.response.GetLocksResponse;
import org.opensearch.rest.RestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.test.rest.FakeRestChannel;
import org.opensearch.test.rest.FakeRestRequest;
import org.opensearch.transport.client.node.NodeClient;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class RestGetAllLocksActionTests extends OpenSearchTestCase {

    private RestGetLocksAction action;
    private String getAllLocksPath;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.action = new RestGetLocksAction();
        this.getAllLocksPath = JobSchedulerPlugin.JS_BASE_URI + "/api/locks";
    }

    public void testGetName() {
        String name = action.getName();
        assertEquals("get_all_locks_action", name);
    }

    public void testRoutes() {
        List<RestHandler.Route> routes = action.routes();
        assertEquals(2, routes.size());
        assertEquals(getAllLocksPath, routes.get(0).getPath());
        assertEquals(RestRequest.Method.GET, routes.get(0).getMethod());
        assertEquals(getAllLocksPath + "/{lock_id}", routes.get(1).getPath());
        assertEquals(RestRequest.Method.GET, routes.get(1).getMethod());
    }

    public void testPrepareRequest() throws IOException {
        FakeRestRequest request = new FakeRestRequest.Builder(xContentRegistry()).withMethod(RestRequest.Method.GET)
            .withPath(getAllLocksPath)
            .withParams(new HashMap<>())
            .build();

        final FakeRestChannel channel = new FakeRestChannel(request, true, 0);
        NodeClient mockClient = Mockito.mock(NodeClient.class);

        doAnswer(invocation -> {
            ActionListener<GetLocksResponse> listener = invocation.getArgument(2);
            GetLocksResponse response = new GetLocksResponse(emptyMap());
            listener.onResponse(response);
            return null;
        }).when(mockClient).execute(eq(GetAllLocksAction.INSTANCE), any(GetLocksRequest.class), any(ActionListener.class));

        action.prepareRequest(request, mockClient);

        assertEquals(0, channel.responses().get());
        assertEquals(0, channel.errors().get());
    }
}

/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.jobscheduler.rest.action;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.ActionListener;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.common.inject.Inject;
import org.opensearch.jobscheduler.rest.request.GetJobDetailsRequest;
import org.opensearch.jobscheduler.rest.request.GetJobDetailsResponse;
import org.opensearch.jobscheduler.utils.JobDetailsService;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class GetJobDetailsTransportAction extends HandledTransportAction<GetJobDetailsRequest, GetJobDetailsResponse> {

    private static final Logger LOG = LogManager.getLogger(GetJobDetailsTransportAction.class);

    public JobDetailsService jobDetailsService;

    @Inject
    public GetJobDetailsTransportAction(
        ActionFilters actionFilters,
        TransportService transportService,
        JobDetailsService jobDetailsService
    ) {
        super(GetJobDetailsAction.NAME, transportService, actionFilters, GetJobDetailsRequest::new);
        this.jobDetailsService = jobDetailsService;
    }

    @Override
    protected void doExecute(Task task, GetJobDetailsRequest request, ActionListener<GetJobDetailsResponse> listener) {
        String documentId = request.getDocumentId();
        String jobIndex = request.getJobIndex();
        String jobType = request.getJobType();
        String jobParameterAction = request.getJobParameterAction();
        String jobRunnerAction = request.getJobRunnerAction();
        String extensionUniqueId = request.getExtensionUniqueId();

        CompletableFuture<String> inProgressFuture = new CompletableFuture<>();

        jobDetailsService.processJobDetails(
            documentId,
            jobIndex,
            jobType,
            jobParameterAction,
            jobRunnerAction,
            extensionUniqueId,
            new ActionListener<>() {
                @Override
                public void onResponse(String indexedDocumentId) {
                    // Set document Id
                    inProgressFuture.complete(indexedDocumentId);
                }

                @Override
                public void onFailure(Exception e) {
                    logger.info("could not process job index", e);
                    inProgressFuture.completeExceptionally(e);
                }
            }
        );

        try {
            inProgressFuture.orTimeout(JobDetailsService.TIME_OUT_FOR_REQUEST, TimeUnit.SECONDS);
        } catch (CompletionException e) {
            if (e.getCause() instanceof TimeoutException) {
                logger.error("Get Job Details timed out ", e);
            }
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else if (e.getCause() instanceof Error) {
                throw (Error) e.getCause();
            } else {
                throw new RuntimeException(e.getCause());
            }
        }

        String jobDetailsResponseHolder = null;
        try {
            jobDetailsResponseHolder = inProgressFuture.get();
        } catch (Exception e) {
            logger.error("Exception occured in get job details ", e);
        }

        String status = jobDetailsResponseHolder != null ? "success" : "failed";
        GetJobDetailsResponse resp = new GetJobDetailsResponse(status);
        listener.onResponse(resp);
    }
}

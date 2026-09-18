/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.rest.util;

/**
 * Documentation strings shared by more than one endpoint's OpenAPI annotations.
 *
 * <h2>Why constants</h2>
 *
 * An annotation element takes a compile-time constant, so a description repeated across resource
 * classes can only be shared this way. Keeping the repeated ones here means a correction lands
 * everywhere at once rather than in whichever copy someone happened to find, which is how the
 * {@code Retry-After} wording drifted out of the spec in the first place: two resource classes each
 * told callers to read the header, and no response anywhere declared it.
 *
 * @author phase3
 */
public final class ApiDocs {

    private ApiDocs() {
    }

    /**
     * Name of the header every retryable 503 carries.
     * <p>
     * Spelled out rather than taken from {@code jakarta.ws.rs.core.HttpHeaders.RETRY_AFTER} so the
     * resource classes that only need the documentation do not pull in the JAX-RS core types.
     */
    public static final String RETRY_AFTER = "Retry-After";

    /**
     * How the {@code Retry-After} header behaves, for the responses that send it.
     * <p>
     * Both RFC 9110 forms genuinely occur, which is the part a client has to be told: the JAX-RS
     * {@code ServiceUnavailableException(String, long, Throwable)} constructor emits a delay in
     * seconds, and {@code ServiceUnavailableException(Date, ...)} emits an HTTP-date. Gemma uses
     * both — a client that parses only the integer form silently loses the schedule on every search
     * and ontology timeout.
     */
    public static final String RETRY_AFTER_DESCRIPTION = "How long to wait before retrying this"
            + " request. Sent in either form RFC 9110 permits — a delay in seconds (`5`) or an"
            + " HTTP-date (`Wed, 18 Sep 2026 21:04:11 GMT`) — so parse both.";

    /**
     * The 503 a {@link ubic.gemma.rest.annotations.Costly} route answers when its budget is full.
     * <p>
     * Distinct from a timeout 503: nothing went wrong and nothing was attempted. The request was
     * refused at the door because the pool for this kind of work was already full.
     */
    public static final String CAPACITY_503_DESCRIPTION = "Gemma is at capacity for this kind of"
            + " request and refused it rather than queueing it. Only unauthenticated requests are"
            + " bounded this way — a request carrying a credential takes no permit and is never"
            + " refused here. Retry after the delay in the `Retry-After` header.";
}

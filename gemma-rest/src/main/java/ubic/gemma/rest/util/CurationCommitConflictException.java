/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ubic.gemma.rest.util;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

/**
 * A curation commit refused as a 409, carrying <em>which</em> conflict it was.
 * <p>
 * The endpoint has several ways to answer 409 and a client has to do something different for each: re-read and
 * re-diff, ask the curator to consent, or fix the body. Told apart only by the prose in the message, that choice
 * becomes string-matching against sentences nobody promised to keep — so the reason travels as a stable code in
 * {@code errors[0].reason}, mapped by {@code CurationCommitConflictExceptionMapper}.
 * <p>
 * 🛑 A stale baseline deliberately does <em>not</em> hand back the current token. The client cannot simply retry
 * with a fresher one: its draft was built against a state that no longer holds, so committing over the change it
 * has not seen is exactly the overwrite the token exists to prevent. Re-read, re-diff, then commit.
 * <p>
 * Extends {@link ClientErrorException} so {@code RequestExceptionLogger} keeps logging it as the client-side
 * outcome it is — one WARN line, no server-fault stack trace.
 *
 * @author gemma
 */
public class CurationCommitConflictException extends ClientErrorException {

    /**
     * What the client should do about it. The names are the wire contract: they appear verbatim as
     * {@code errors[0].reason}.
     */
    public enum Reason {
        /** The dataset moved since the draft's baseline. Re-read it, rebuild the diff, commit again. */
        STALE_BASELINE,
        /**
         * The change would delete differential-expression analyses or strand a subset. Route it through
         * {@code POST /datasets/{id}/curation/sign}, which is where a change with consequences belongs, or —
         * for a caller that is not signing — consent with {@code ?force=true} (admin).
         */
        REQUIRES_FORCE,
        /** Sign-off was attempted without holding the curation lock, or while someone else holds it. */
        LOCK_REQUIRED,
        /** A paper the commit attaches stands rejected for this dataset by a source the commit does not outrank. */
        PUBLICATION_REJECTED,
        /** The write refused this as a conflict without saying which kind — e.g. a short name already in use. */
        UNSPECIFIED
    }

    private final Reason reason;

    public CurationCommitConflictException( Reason reason, String message ) {
        super( message, Response.Status.CONFLICT );
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}

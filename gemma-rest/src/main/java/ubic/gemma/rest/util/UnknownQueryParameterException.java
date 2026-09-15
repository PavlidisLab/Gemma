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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Thrown when a request carries a query parameter the matched resource method cannot bind.
 * <p>
 * Extends {@link MalformedArgException} so the request stays a 400 through any handler that already treats a
 * malformed argument as such; {@code UnknownQueryParameterExceptionMapper} is more specific and builds the
 * structured body, naming each rejected parameter in {@code location} with {@link LocationType#QUERY}.
 *
 * @author gemma
 */
public class UnknownQueryParameterException extends MalformedArgException {

    private final List<String> unknownParameters;
    private final Set<String> acceptedParameters;

    public UnknownQueryParameterException( Collection<String> unknownParameters, Collection<String> acceptedParameters ) {
        super( buildMessage( unknownParameters, acceptedParameters ) );
        this.unknownParameters = Collections.unmodifiableList( new java.util.ArrayList<>( unknownParameters ) );
        this.acceptedParameters = Collections.unmodifiableSet( new TreeSet<>( acceptedParameters ) );
    }

    /**
     * Name the offending parameters and then the accepted ones, so the response alone is enough to correct the
     * request. Mirrors the shape of the limit-cap message ("The provided limit cannot exceed 100."), which callers
     * have found diagnosable because it names what it is complaining about.
     */
    private static String buildMessage( Collection<String> unknownParameters, Collection<String> acceptedParameters ) {
        StringBuilder sb = new StringBuilder();
        sb.append( unknownParameters.size() == 1 ? "Unknown query parameter " : "Unknown query parameters " );
        sb.append( quoteAndJoin( unknownParameters ) );
        sb.append( "." );
        if ( acceptedParameters.isEmpty() ) {
            sb.append( " This endpoint does not accept any query parameter." );
        } else {
            sb.append( " This endpoint accepts: " )
                    .append( String.join( ", ", new TreeSet<>( acceptedParameters ) ) )
                    .append( "." );
        }
        return sb.toString();
    }

    private static String quoteAndJoin( Collection<String> names ) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for ( String name : names ) {
            if ( i++ > 0 ) {
                sb.append( ", " );
            }
            sb.append( '\'' ).append( name ).append( '\'' );
        }
        return sb.toString();
    }

    /**
     * The rejected parameter names, in the order they appeared in the query string.
     */
    public List<String> getUnknownParameters() {
        return unknownParameters;
    }

    /**
     * Every query parameter the matched resource method declares, sorted.
     */
    public Set<String> getAcceptedParameters() {
        return acceptedParameters;
    }
}

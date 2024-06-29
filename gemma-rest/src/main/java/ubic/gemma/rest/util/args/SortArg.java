/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
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
package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.FilteringService;
import ubic.gemma.rest.util.MalformedArgException;

/**
 * Class representing an API argument that should be an integer.
 *
 * @author tesarst
 */
@Schema(type = "string", pattern = "^(\\+|-?)(\\w+)$", description = "Order results by the given property and direction. The '+' sign indicate ascending order whereas the '-' indicate descending.")
public class SortArg<O extends Identifiable> extends AbstractArg<SortArg.Sort> {
    private static final String ERROR_MSG =
            "Value '%s' can not be interpreted as a sort argument. Correct syntax is: [+,-][field]. E.g: '-id' means 'order by ID descending. "
                    + "Make sure you URL encode the arguments, for example '+' has to be encoded to '%%2B'.";

    private SortArg( String field, Sort.Direction direction ) {
        super( new Sort( field, direction ) );
    }

    /**
     * Obtain the {@link Sort} underlying this argument.
     *
     * @param service a {@link FilteringService} that knows how to build a sort object
     * @return the sorting object in question
     * @throws MalformedArgException in case the orderBy property cannot be applied for the given class, or if the
     *                               argument was malformed in the first place
     */
    ubic.gemma.persistence.util.Sort getSort( FilteringService<O> service ) throws MalformedArgException {
        ubic.gemma.persistence.util.Sort.Direction direction;
        if ( getValue().direction == Sort.Direction.ASC ) {
            direction = ubic.gemma.persistence.util.Sort.Direction.ASC;
        } else if ( getValue().direction == Sort.Direction.DESC ) {
            direction = ubic.gemma.persistence.util.Sort.Direction.DESC;
        } else {
            direction = null;
        }
        try {
            return service.getSort( this.getValue().orderBy, direction );
        } catch ( IllegalArgumentException e ) {
            throw new MalformedArgException( e );
        }
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request taxon argument
     * @return a new SortArg object representing the sort options in the given string, or a malformed SortArg that will
     * throw a {@link javax.ws.rs.BadRequestException}, if the given string was not well-formed.
     */
    @SuppressWarnings("unused")
    public static <T extends Identifiable> SortArg<T> valueOf( final String s ) throws MalformedArgException {
        try {
            Sort.Direction direction = parseDirection( s.charAt( 0 ) );
            String orderBy = direction == null ? s : s.substring( 1 );
            return new SortArg<>( orderBy, direction );
        } catch ( NullPointerException | IndexOutOfBoundsException | IllegalArgumentException e ) {
            throw new MalformedArgException( String.format( ERROR_MSG, s ), e );
        }
    }

    /**
     * Decides whether the given char represents a true or false.
     *
     * @param c '+' or '-' character.
     * @return {@link Sort.Direction#ASC} if character was '+', {@link Sort.Direction#DESC} if it was '-'. Null in any other case.
     */
    private static Sort.Direction parseDirection( char c ) {
        if ( c == ' ' ) {
            throw new MalformedArgException( "The sorting direction cannot be an empty character. It seems that you used a raw '+' in your query, instead use the URL-encoded '%2B' value." );
        } else if ( c == '+' ) {
            return Sort.Direction.ASC;
        } else if ( c == '-' ) {
            return Sort.Direction.DESC;
        } else {
            return null; /* the character will be parsed as part of the order by property with the default direction */
        }
    }

    public static class Sort {

        private final String orderBy;
        @Nullable
        private final Direction direction;

        private Sort( String orderBy, @Nullable Direction direction ) {
            if ( StringUtils.isBlank( orderBy ) ) {
                throw new IllegalArgumentException( "The 'orderBy' attribute cannot be blank or empty." );
            }
            this.orderBy = orderBy;
            this.direction = direction;
        }

        public String getOrderBy() {
            return orderBy;
        }

        @Nullable
        public Direction getDirection() {
            return direction;
        }

        public enum Direction {
            ASC, DESC
        }
    }
}

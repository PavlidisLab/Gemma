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
package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.web.services.rest.util.ArgUtils;
import ubic.gemma.web.services.rest.util.MalformedArgException;

import javax.ws.rs.BadRequestException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class representing an API argument that should be an integer.
 *
 * @author tesarst
 */
@Schema(implementation = String.class, type = "string")
public class SortArg extends AbstractArg<Sort> {
    private static final String ERROR_MSG =
            "Value '%s' can not be interpreted as a sort argument. Correct syntax is: [+,-][field]. E.g: '-id' means 'order by ID descending. "
                    + "Make sure you URL encode the arguments, for example '+' has to be encoded to '%%2B'.";

    private SortArg( String field, Sort.Direction direction ) {
        super( new Sort( field, direction ) );
    }

    /**
     * Constructor used to create an instance that instead of returning the sort values, informs that the received
     * string was not well-formed.
     *
     * @param errorMessage the malformed original string argument.
     */
    private SortArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Obtain the field this is sorting by given an entity class that is expected to contain it.
     * @param cls the class of the entity
     * @return the field to sort by.
     * @throws MalformedArgException if the original argument was not well-composed or the class does not contain the
     * expected field
     */
    public String getFieldForClass( Class<?> cls ) throws MalformedArgException {
        try {
            checkFieldExists( cls, getValue().getOrderBy() );
            return getValue().getOrderBy();
        } catch ( NoSuchFieldException e ) {
            throw new MalformedArgException( e.getMessage(), e );
        }
    }

    /**
     * @return the direction of sort.
     * @throws BadRequestException if the original argument was not well-composed
     */
    public boolean isAsc() throws BadRequestException {
        return getValue().getDirection() == Sort.Direction.ASC;
    }

    public Sort getValueForClass( Class<?> cls ) {
        try {
            checkFieldExists( cls, getValue().getOrderBy() );
            return getValue();
        } catch ( NoSuchFieldException e ) {
            throw new MalformedArgException( e.getMessage(), e );
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
    public static SortArg valueOf( final String s ) {
        try {
            //noinspection ConstantConditions // Handled by the try catch
            return new SortArg( parseDirection( s.charAt( 0 ) ) == null ? s : s.substring( 1 ), parseDirection( s.charAt( 0 ) ) );
        } catch ( NullPointerException | StringIndexOutOfBoundsException e ) {
            return new SortArg( String.format( ERROR_MSG, s ), e );
        }
    }

    /**
     * Decides whether the given char represents a true or false.
     *
     * @param c '+' or '-' character.
     * @return true if character was '+', false if it was '-'. Null in any other case.
     */
    private static Sort.Direction parseDirection( char c ) {
        if ( c == '+' ) {
            return Sort.Direction.ASC;
        } else if ( c == '-' ) {
            return Sort.Direction.DESC;
        } else {
            return null;
        }
    }

    private static Field checkFieldExists( Class<?> cls, String field ) throws NoSuchFieldException {
        List<Field> fields = new ArrayList<>();
        for ( Class<?> c = cls; c != null; c = c.getSuperclass() ) {
            fields.addAll( Arrays.asList( c.getDeclaredFields() ) );
        }

        for ( Field f : fields ) {
            if ( f.getName().equals( field ) )
                return f;
        }
        throw new NoSuchFieldException( "Class " + cls + " does not contain field '" + field + "'." );
    }
}

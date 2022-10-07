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
package ubic.gemma.web.services.rest.util;

import lombok.NonNull;
import ubic.gemma.web.services.rest.util.args.Arg;

import javax.ws.rs.BadRequestException;

/**
 * Utilities for working with {@link Arg}.
 * @author poirigui
 */
public class ArgUtils {

    /**
     * Check if the argument exists and raise a {@link BadRequestException} if it is null or has an empty representation.
     * @param arg an argument
     * @param name a name to use, since the passed argument can be null
     * @return the argument if it exists
     * @throws BadRequestException
     */
    public static <T extends Arg> T requiredArg( T arg, @NonNull String name ) throws BadRequestException {
        if ( arg == null || arg.toString().isEmpty() ) {
            throw new BadRequestException( String.format( "Value for required parameter '%s' not found.", name ) );
        } else {
            return arg;
        }
    }
}

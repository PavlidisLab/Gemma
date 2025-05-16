/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
 *
 */
package ubic.gemma.core.loader.util.parser;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * Interface for classes that allow parsing of files and streams.
 *
 * @author pavlidis
 */
@SuppressWarnings("unused") // Possible external use
public interface Parser<T> {

    int PARSE_ALERT_FREQUENCY = 10000;

    /**
     * @return the results of the parse.
     */
    Collection<T> getResults();

    /**
     * Obtain a unique result from the parser.
     * @throws IllegalStateException if there is more than one parse result
     */
    @Nullable
    default T getUniqueResult() throws IllegalStateException {
        Collection<T> results = getResults();
        if ( results.isEmpty() ) {
            return null;
        } else if ( results.size() > 1 ) {
            throw new IllegalStateException( "There is more than one parse result." );
        } else {
            return results.iterator().next();
        }
    }

    /**
     * Parse a {@link File}
     *
     * @param f file
     * @throws IOException if there is a problem while manipulating the file
     */
    void parse( File f ) throws IOException;

    /**
     * Parse a {@link InputStream}.
     *
     * @param is input stream
     * @throws IOException if there is a problem while manipulating the file
     */
    void parse( InputStream is ) throws IOException;

    /**
     * Parse a file identified by its path.
     *
     * @param filename Absolute path to the file
     * @throws IOException if there is a problem while manipulating the file
     */
    void parse( String filename ) throws IOException;

}
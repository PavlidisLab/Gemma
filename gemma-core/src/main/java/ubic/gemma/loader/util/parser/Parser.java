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
package ubic.gemma.loader.util.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * Interface for classes that allow parsing of files and streams.
 * 
 * @author pavlidis
 * @version $Id$
 */
public interface Parser<T> {

    public static final int PARSE_ALERT_FREQUENCY = 10000;

    /**
     * @return the results of the parse.
     */
    public Collection<T> getResults();

    /**
     * Parse a {@link File}
     * 
     * @param f
     * @throws IOException
     */
    public void parse( File f ) throws IOException;

    /**
     * Parse a {@link InputStream}.
     * 
     * @throws IOException
     * @param stream
     */
    public void parse( InputStream is ) throws IOException;

    /**
     * Parse a file identified by its path.
     * 
     * @param filename Absolute path to the file
     * @throws IOException
     */
    public void parse( String filename ) throws IOException;

}
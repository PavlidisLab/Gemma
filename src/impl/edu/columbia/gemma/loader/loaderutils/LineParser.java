/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.loader.loaderutils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * A class that processes its input line-by-line. One of the parse methods must be called before data becomes available.
 * <p>
 * TODO: allow different delimiters to be defined (like $/ in perl)
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public interface LineParser {

    /**
     * Parse a {@link InputStream}.
     * 
     * @throws IOException
     * @param stream
     */
    public void parse( InputStream is ) throws IOException;

    /**
     * Parse a {@link File}
     * 
     * @param f
     * @throws IOException
     */
    public void parse( File f ) throws IOException;

    /**
     * Parse a file identified by its path.
     * 
     * @param filename
     * @throws IOException
     */
    public void parse( String filename ) throws IOException;

    /**
     * @return an Iterator that can be used to look at the results, after they have parsed from the input.
     */
    public Iterator iterator();

    /**
     * Handle the parsing of a single line from the input.
     * 
     * @param line
     */
    abstract Object parseOneLine( String line );

}
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
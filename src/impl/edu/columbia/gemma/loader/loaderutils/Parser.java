package edu.columbia.gemma.loader.loaderutils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Interface for classes that allow parsing of files and streams.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public interface Parser {

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
     * @param filename Absolute path to the file
     * @throws IOException
     */
    public abstract void parse( String filename ) throws IOException;

    /**
     * @return an Iterator that can be used to look at the results, after they have parsed from the input.
     */
    public Iterator iterator();

}
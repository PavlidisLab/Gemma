package edu.columbia.gemma.loader.loaderutils;

import java.io.IOException;
import java.io.InputStream;

/**
 * 
 *
 * <hr>
 * <p>Copyright (c) 2004 - 2005 Columbia University
 * @author keshav
 * @version $Id$
 */
public interface BulkCreator {
    /**
     * @param is
     * @param hasHeader Indicate if the stream is from a file that has a one-line header
     * @throws IOException
     */
    public abstract int bulkCreate( InputStream is, boolean hasHeader ) throws IOException;

    /**
     * @param filename
     * @param hasHeader Indicate if the stream is from a file that has a one-line header
     * @throws IOException
     */
    public abstract void bulkCreate( String filename, boolean hasHeader ) throws IOException;
}
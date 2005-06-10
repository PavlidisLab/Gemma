package edu.columbia.gemma.loader.genome.gene;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public interface Parser {

    public abstract Map parse( InputStream is, Method m ) throws IOException;

    public abstract Map parseFile( String filename ) throws IOException;

}
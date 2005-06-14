package edu.columbia.gemma.loader.genome.gene;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public interface Parser {

    public Map parse( InputStream is, Method m ) throws IOException;

    public Map parseFile( String filename ) throws IOException;

    public Collection parseFromHttp( String url ) throws IOException, ConfigurationException;

    public Collection createOrGetDependencies( Object[] dependencies, Map objectMap );

}
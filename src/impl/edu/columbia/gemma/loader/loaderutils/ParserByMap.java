package edu.columbia.gemma.loader.loaderutils;

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
public interface ParserByMap {

    public Map parse( InputStream is, Method lineParseMethod ) throws IOException;

    public Map parseToMap( String filename ) throws IOException;

    public Map parseFromHttp( String url ) throws IOException, ConfigurationException;

    public Collection createOrGetDependencies( Object[] dependencies, Map objectMap );

}
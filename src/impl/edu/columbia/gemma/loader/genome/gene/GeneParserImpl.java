package edu.columbia.gemma.loader.genome.gene;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.loader.loaderutils.BasicLineMapParser;
import edu.columbia.gemma.loader.loaderutils.LoaderTools;

/**
 * Parse gene files (ncbi, etc).
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="geneParser"
 * @spring.property name="geneMappings" ref="geneMappings"
 */
public class GeneParserImpl extends BasicLineMapParser implements Parser {
    protected static final Log log = LogFactory.getLog( Parser.class );
    private GeneMappings geneMappings = null;
    private Map map;

    String[] keys = null;
    Method methodToInvoke = null;
    String suffixOfFilename = null;

    /**
     * 
     */
    public GeneParserImpl() {
        map = new HashMap();
    }

    /**
     * @return Returns the geneMappings.
     */
    public GeneMappings getGeneMappings() {
        return this.geneMappings;
    }

    /**
     * Parse the specified file, filename.
     * 
     * @param filename
     * @return
     * @throws IOException, ConfigurationException
     */
    public Map parse( InputStream fis, Method lineParseMethod ) throws IOException {
        methodToInvoke = lineParseMethod;
        parse( fis );
        LoaderTools.debugMap( map );
        return map;
    }

    /**
     * @param filename
     * @return Map
     * @throws IOException
     */
    public Map parseFile( String filename ) throws IOException {
        log.info( "filename: " + filename );

        File file = new File( filename );
        FileInputStream fis = new FileInputStream( file );

        Method lineParseMethod = null;
        try {
            lineParseMethod = LoaderTools.findParseLineMethod( getGeneMappings(), filename );
        } catch ( NoSuchMethodException e ) {
            log.error( e, e );
            return null;
        }
        return this.parse( fis, lineParseMethod );

    }

    /**
     * @return Object
     * @param line
     * @see BasicLineMapParser
     */
    public Object parseOneLine( String line ) {
        assert geneMappings != null;
        assert map != null;
        Gene g = null;

        try {
            Object obj = methodToInvoke.invoke( geneMappings, new Object[] { line } );
            if ( obj == null ) return obj;
            g = ( Gene ) obj;
            map.put( g.getNcbiId(), g );
            return g;
        } catch ( IllegalArgumentException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch ( IllegalAccessException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch ( InvocationTargetException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

    }

    /**
     * @param geneMappings The geneMappings to set.
     */
    public void setGeneMappings( GeneMappings geneMappings ) {
        this.geneMappings = geneMappings;
    }

    /**
     * @return Object
     * @see BasicLineMapParser
     */
    protected Object getKey( Object newItem ) {

        return ( ( Gene ) newItem ).getNcbiId();
    }

    public Collection createOrGetDependencies( Object[] dependencies, Map objectMap ) {
        // TODO code this to create the objects that Gene is associated with. These
        // associations should not be from compositions relationships.
        throw new UnsupportedOperationException();
    }

    public Map parseFromHttp( String url ) throws IOException, ConfigurationException {
        // TODO code this to parse directly from the web.
        throw new UnsupportedOperationException();
    }

}

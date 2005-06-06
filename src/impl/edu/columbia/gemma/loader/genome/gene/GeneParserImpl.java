package edu.columbia.gemma.loader.genome.gene;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.loader.loaderutils.BasicLineMapParser;

/**
 * Parse gene files (ncbi, etc).
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class GeneParserImpl extends BasicLineMapParser implements Parser {
    protected static final Log log = LogFactory.getLog( Parser.class );
    private GeneMappings geneMappings = null;
    private Iterator iter;
    private Map map;

    String[] keys = null;
    Method methodToInvoke = null;
    String suffixOfFilename = null;

    /**
     * 
     */
    public GeneParserImpl() throws ConfigurationException {
        map = new HashMap();
    }

    public Map parseFile( String filename ) throws IOException {
        log.info( "filename: " + filename );

        File file = new File( filename );
        FileInputStream fis = new FileInputStream( file );

        Method lineParseMethod = null;
        try {
            lineParseMethod = this.findParseLineMethod( filename );
        } catch ( NoSuchMethodException e ) {
            log.error( e, e );
            return null;
        }
        return this.parse( fis, lineParseMethod );

    }

    /**
     * Figure out the method to parse one line from a file, based on the file name.
     * 
     * @param fileName
     * @return
     * @throws NoSuchMethodException
     */
    public Method findParseLineMethod( String fileName ) throws NoSuchMethodException {
        String[] f = StringUtils.split( fileName, System.getProperty( "file.separator" ) );
        suffixOfFilename = f[f.length - 1];
        assert geneMappings != null;
        Method[] methods = geneMappings.getClass().getMethods();

        for ( int i = 0; i < methods.length; i++ ) {
            if ( methods[i].getName().toLowerCase().matches( ( "mapFrom" + suffixOfFilename ).toLowerCase() ) ) {
                return methods[i];
            }
        }
        throw new NoSuchMethodException();
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
        debugMap();
        return map;
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
     * Print content of map if debug is set to true.
     * 
     * @param debug
     */
    private void debugMap() {
        if ( log.isDebugEnabled() ) {
            log.info( "Map contains: " );
            iter = map.keySet().iterator();
            int i = 0;
            while ( iter.hasNext() ) {
                log.info( iter.next() );
                i++;
            }
            log.info( "map size: " + i );
        }
    }

    /**
     * @return Object
     * @see BasicLineMapParser
     */
    protected Object getKey( Object newItem ) {

        return ( ( Gene ) newItem ).getNcbiId();
    }

    /**
     * @return Returns the geneMappings.
     */
    public GeneMappings getGeneMappings() {
        return this.geneMappings;
    }

    /**
     * @param geneMappings The geneMappings to set.
     */
    public void setGeneMappings( GeneMappings geneMappings ) {
        this.geneMappings = geneMappings;
    }

}

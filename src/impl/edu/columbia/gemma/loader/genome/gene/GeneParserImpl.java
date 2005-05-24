package edu.columbia.gemma.loader.genome.gene;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
public class GeneParserImpl extends BasicLineMapParser implements GeneParser {
    protected static final Log log = LogFactory.getLog( GeneParser.class );
    private GeneMappings gm = null;
    private Iterator iter;
    private Map map;
    String gFilename = null;
    String[] keys = null;
    Method methodToInvoke = null;
    String suffixOfFilename = null;

    /**
     * 
     */
    public GeneParserImpl() throws ConfigurationException {
        gm = new GeneMappings();
        map = new HashMap();
    }

    /**
     * Parse the specified file, filename.
     * 
     * @param filename
     * @return
     * @throws IOException, ConfigurationException
     */
    public Map parseFile( String filename ) throws IOException {
        File file = new File( filename );
        FileInputStream fis = new FileInputStream( file );

        log.info( "filename: " + filename + " FileInputStream: " + fis.toString() );

        // TODO I don't like this, but I need it in parseOneLine
        gFilename = filename;

        String[] f = StringUtils.split( filename, System.getProperty( "file.separator" ) );
        suffixOfFilename = f[f.length - 1];

        Method[] methods = gm.getClass().getMethods();

        for ( int i = 0; i < methods.length; i++ ) {
            if ( methods[i].getName().toLowerCase().matches( ( "mapFrom" + suffixOfFilename ).toLowerCase() ) ) {
                log.info( methods[i] );
                methodToInvoke = methods[i];
                parse( fis );
                debugMap();
                return map;
            }
        }
        throw new IOException( "Invalid File \"" + filename + "\"" );
    }

    /**
     * @return Object
     * @param line
     * @see BasicLineMapParser
     */
    public Object parseOneLine( String line ) {

        Gene g = null;

        try {
            g = ( Gene ) methodToInvoke.invoke( gm, new Object[] { line, Gene.Factory.newInstance() } );
        } catch ( IllegalArgumentException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( IllegalAccessException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( InvocationTargetException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        map.put( g.getNcbiId(), g );

        return g;

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

}

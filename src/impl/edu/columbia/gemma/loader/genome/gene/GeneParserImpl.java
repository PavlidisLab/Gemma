package edu.columbia.gemma.loader.genome.gene;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
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
    private ParserUtils pu = null;
    String gFilename = null;
    String[] keys = null;

    /**
     * 
     */
    public GeneParserImpl() {
        gm = new GeneMappings();
        pu = new ParserUtils();
        map = new HashMap();
        keys = new String[] { "gene2accession", "gene2go", "gene2refseq", "gene2sts", "gene2unigene", "gene_history",
                "gene_info", "mim2gene" };
    }

    /**
     * Parse the specified file, filename.
     * 
     * @param filename
     * @return
     * @throws IOException, ConfigurationException
     */
    public Map parseFile( String filename ) throws IOException, ConfigurationException {
        File file = new File( filename );
        FileInputStream fis = new FileInputStream( file );

        log.info( "filename: " + filename + " FileInputStream: " + fis.toString() );

        // TODO I don't like this, but I need it in parseOneLine
        gFilename = filename;

        if ( pu.validFile( filename, pu.initializeFileTypes( keys ) ) ) {
            parse( fis );
            debugMap();
            return map;
        } else {
            throw new IOException( "Invalid File \"" + filename + "\"" );
        }
    }

    /**
     * @return Object
     * @param line
     * @see BasicLineMapParser
     */
    public Object parseOneLine( String line ) {

        Gene g = null;

        g = ( Gene ) gm.mapLine( gFilename, line, keys, Gene.Factory.newInstance() );
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

package edu.columbia.gemma.loader.arraydesign;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.symbol.IllegalSymbolException;

import baseCode.io.reader.BasicLineMapParser;

/**
 * Parse an Illumina "manifest.txt" file (tab-delimited). A one-line header is permitted.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class IlluminaProbeReader extends BasicLineMapParser {

    protected static final Log log = LogFactory.getLog( IlluminaProbeReader.class );

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.BasicLineMapParser#parseOneLine(java.lang.String)
     */
    public Object parseOneLine( String line ) {
        String[] sArray = line.split( "\t" );
        if ( sArray.length == 0 ) throw new IllegalArgumentException( "Line format is not valid" );

        String probeSetId = sArray[0];
        if ( probeSetId.startsWith( "Search" ) ) return null;

        if ( sArray.length < 10 ) throw new IllegalArgumentException( "Line format is not valid" );

        String sequence = sArray[9];

        try {
            Sequence ns = DNATools.createDNASequence( sequence, probeSetId );
            Probe ap = new SimpleProbe( probeSetId, ns );

            return ap;

        } catch ( IllegalSymbolException e ) {
            throw new IllegalArgumentException( "a DNA sequence was not valid, or the file format is incorrect." );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.BasicLineMapParser#getKey(java.lang.Object)
     */
    protected String getKey( Object newItem ) {
        return ( ( SimpleProbe ) newItem ).getIdentifier();
    }
}

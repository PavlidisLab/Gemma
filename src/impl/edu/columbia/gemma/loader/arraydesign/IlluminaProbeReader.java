package edu.columbia.gemma.loader.arraydesign;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.expression.designElement.Reporter;
import edu.columbia.gemma.loader.loaderutils.BasicLineMapParser;
import edu.columbia.gemma.genome.biosequence.BioSequence;

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

        String probeId = sArray[0];

        if ( probeId == null || probeId.length() == 0 ) throw new IllegalArgumentException( "Probe id invalid" );

        if ( probeId.startsWith( "Search" ) ) return null;

        if ( sArray.length < 10 ) throw new IllegalArgumentException( "Line format is not valid" );

        String sequence = sArray[9];
        if ( sequence == null || sequence.length() == 0 ) throw new IllegalArgumentException( "Sequence is invalid" );

        Reporter ap = Reporter.Factory.newInstance();

        BioSequence immobChar = BioSequence.Factory.newInstance();
        immobChar.setSequence( sequence );
        ap.setName( probeId );
        ap.setImmobilizedCharacteristic( immobChar );

        return ap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.BasicLineMapParser#getKey(java.lang.Object)
     */
    protected Object getKey( Object newItem ) {
        return ( ( Reporter ) newItem ).getName();
    }
}

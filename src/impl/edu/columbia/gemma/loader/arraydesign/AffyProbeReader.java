package edu.columbia.gemma.loader.arraydesign;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.symbol.IllegalSymbolException;

import baseCode.io.reader.BasicLineMapParser;

/**
 * Reads Affymetrix Probe files.
 * <p>
 * Expected format is tabbed, NOT FASTA: 1494_f_at 1 325 359 1118 TCCCCATGAGTTTGGCCCGCAGAGT Antisense. A one-line header
 * starting with the word "Probe" is permitted.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AffyProbeReader extends BasicLineMapParser {

    protected static final Log log = LogFactory.getLog( AffyProbeReader.class );

    private int sequenceField = 3;

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.BasicLineParser#parseOneLine(java.lang.String)
     */
    public Object parseOneLine( String line ) {
        String[] sArray = line.split( "\t" );
        if ( sArray.length == 0 ) throw new IllegalArgumentException( "Line format is not valid" );

        String probeSetId = sArray[0];
        if ( probeSetId.startsWith( "Probe" ) ) return null;

        // if ( sArray.length < 5 ) throw new IOException( "File format is not valid" );

        String xcoord = sArray[1];
        String ycoord = sArray[2];
        String sequence = sArray[sequenceField];
        int locationInTarget = Integer.parseInt( sArray[sequenceField - 1] ); // unfortunately this depends on the
        // file.
        try {
            Sequence ns = DNATools.createDNASequence( sequence, probeSetId + "_" + xcoord + "_" + ycoord );
            AffymetrixProbe ap = new AffymetrixProbe( probeSetId, xcoord, ns, locationInTarget );

            AffymetrixProbeSet newps = ( AffymetrixProbeSet ) get( probeSetId );

            if ( newps == null ) newps = new AffymetrixProbeSet( probeSetId );

            newps.add( ap );
            return newps;
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
        assert newItem instanceof AffymetrixProbeSet;
        return ( ( AffymetrixProbeSet ) newItem ).getProbeSetId();
    }

    /**
     * Set the index (starting from zero) of the field where the sequence is found. This varies between 4 and 5 in the
     * Affymetrix-provided files.
     * 
     * @param sequenceField
     */
    public void setSequenceField( int sequenceField ) {
        this.sequenceField = sequenceField;
    }

}

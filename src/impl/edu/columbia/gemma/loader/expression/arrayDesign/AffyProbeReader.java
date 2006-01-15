/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package edu.columbia.gemma.loader.expression.arrayDesign;

import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.Reporter;
import edu.columbia.gemma.genome.biosequence.BioSequence;
import edu.columbia.gemma.loader.loaderutils.BasicLineMapParser;

/**
 * Reads Affymetrix Probe files.
 * <p>
 * Expected format is tabbed, NOT FASTA, for example:
 * <p>
 * <code>1494_f_at 1 325 359 1118 TCCCCATGAGTTTGGCCCGCAGAGT Antisense</code>.
 * </p>
 * <p>
 * A one-line header starting with the word "Probe" is permitted. In later versions of the format the second field
 * (column) is omitted.
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AffyProbeReader extends BasicLineMapParser {

    protected static final Log log = LogFactory.getLog( AffyProbeReader.class );

    private int sequenceField = 4;

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.BasicLineParser#parseOneLine(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public Object parseOneLine( String line ) {
        String[] sArray = line.split( "\t" );
        if ( sArray.length == 0 ) throw new IllegalArgumentException( "Line format is not valid" );

        String probeSetId = sArray[0];
        if ( probeSetId.startsWith( "Probe" ) ) return null;
        String sequence = sArray[sequenceField];

        String xcoord = sArray[sequenceField - 3];
        String ycoord = sArray[sequenceField - 2];

        Reporter reporter = Reporter.Factory.newInstance();

        try {
            reporter.setRow( Integer.parseInt( xcoord ) );
            reporter.setCol( Integer.parseInt( ycoord ) );
            reporter.setStartInBioChar( Integer.parseInt( sArray[sequenceField - 1] ) );
        } catch ( NumberFormatException e ) {
            log.warn( "Invalid row: could not parse coordinates." );
            return null;
        }

        reporter.setName( probeSetId + ":" + xcoord + ":" + ycoord );
        BioSequence immobChar = BioSequence.Factory.newInstance();
        immobChar.setSequence( sequence );
        reporter.setImmobilizedCharacteristic( immobChar );

        CompositeSequence probeSet = ( CompositeSequence ) get( probeSetId );

        if ( probeSet == null ) probeSet = CompositeSequence.Factory.newInstance();
        probeSet.setName( probeSetId );
        if ( probeSet.getReporters() == null ) probeSet.setReporters( new HashSet() );

        probeSet.getReporters().add( reporter );
        return probeSet;

    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.BasicLineMapParser#getKey(java.lang.Object)
     */
    protected Object getKey( Object newItem ) {
        assert newItem instanceof CompositeSequence;
        return ( ( CompositeSequence ) newItem ).getName();
    }

    /**
     * Set the index (starting from zero) of the field where the sequence is found. This varies in the
     * Affymetrix-provided files.
     * 
     * @param sequenceField
     */
    public void setSequenceField( int sequenceField ) {
        this.sequenceField = sequenceField;
    }

}

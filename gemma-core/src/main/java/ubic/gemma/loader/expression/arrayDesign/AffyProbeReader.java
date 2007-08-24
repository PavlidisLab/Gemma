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
package ubic.gemma.loader.expression.arrayDesign;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.util.parser.BasicLineMapParser;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.Reporter;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.PolymerType;
import ubic.gemma.model.genome.biosequence.SequenceType;

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
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AffyProbeReader extends BasicLineMapParser {

    protected static final Log log = LogFactory.getLog( AffyProbeReader.class );

    private int sequenceField = 4;

    private Map<String, CompositeSequence> results = new HashMap<String, CompositeSequence>();

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.BasicLineParser#parseOneLine(java.lang.String)
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object parseOneLine( String line ) {
        String[] sArray = line.split( "\t" );
        if ( sArray.length == 0 )
            throw new IllegalArgumentException( "Line format is not valid (not tab-delimited or no fields found)" );

        if ( sArray.length < sequenceField + 1 ) {
            throw new IllegalArgumentException( "Too few fields in line, expected at least " + ( sequenceField + 1 )
                    + " but got " + sArray.length );
        }

        String probeSetId = sArray[0];
        if ( probeSetId.startsWith( "Probe" ) ) return null;
        String sequence = sArray[sequenceField];
        String xcoord;
        String ycoord;
        String index = null;
        if ( sequenceField == 4 ) {
            xcoord = sArray[1];
            ycoord = sArray[2];
        } else {
            index = sArray[1];
            xcoord = sArray[2];
            ycoord = sArray[3];
        }

        Reporter reporter = Reporter.Factory.newInstance();

        try {
            reporter.setRow( Integer.parseInt( xcoord ) );
            reporter.setCol( Integer.parseInt( ycoord ) );
            reporter.setStartInBioChar( Long.parseLong( sArray[sequenceField - 1] ) );
        } catch ( NumberFormatException e ) {
            log.warn( "Invalid row: could not parse coordinates." );
            return null;
        }

        reporter.setName( probeSetId + ( index == null ? "" : "#" + index ) + ":" + xcoord + ":" + ycoord );
        BioSequence immobChar = BioSequence.Factory.newInstance();
        immobChar.setSequence( sequence );
        immobChar.setIsApproximateLength( false );
        immobChar.setLength( new Long( sequence.length() ) );
        immobChar.setType( SequenceType.AFFY_PROBE );
        immobChar.setPolymerType( PolymerType.DNA );

        reporter.setImmobilizedCharacteristic( immobChar );

        CompositeSequence probeSet = get( probeSetId );

        if ( probeSet == null ) probeSet = CompositeSequence.Factory.newInstance();
        probeSet.setName( probeSetId );
        if ( probeSet.getComponentReporters() == null ) probeSet.setComponentReporters( new HashSet() );

        reporter.setCompositeSequence( probeSet );
        probeSet.getComponentReporters().add( reporter );
        return probeSet;

    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.BasicLineMapParser#getKey(java.lang.Object)
     */
    @Override
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

    @Override
    public CompositeSequence get( Object key ) {
        return results.get( key );
    }

    @Override
    public Collection<CompositeSequence> getResults() {
        return new HashSet<CompositeSequence>( results.values() ); // make sure we don't get a HashMap$values
    }

    @Override
    protected void put( Object key, Object value ) {
        results.put( ( String ) key, ( CompositeSequence ) value );
    }

    @Override
    public boolean containsKey( Object key ) {
        return results.containsKey( key );
    }

    @Override
    public Collection getKeySet() {
        return results.keySet();
    }

}

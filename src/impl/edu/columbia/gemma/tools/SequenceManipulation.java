package edu.columbia.gemma.tools;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.Reporter;
import edu.columbia.gemma.sequence.biosequence.BioSequence;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SequenceManipulation {

    /**
     * Compute just any overlap the compare sequence has with the target on the right side.
     * 
     * @param query
     * @param target
     * @return The index of the end of the overlap. If zero, there is no overlap. In other words, this is the amount
     *         that needs to be trimmed off the compare sequence if we are going to join it on to the target without
     *         generating redundancy.
     */
    public static int rightHandOverlap( BioSequence target, BioSequence query ) {

        if ( target == null || query == null ) throw new IllegalArgumentException( "Null parameters" );

        String targetString = target.getSequence();
        String queryString = query.getSequence();

        if ( targetString == null ) throw new IllegalArgumentException( "Target sequence was empty" );
        if ( queryString == null ) throw new IllegalArgumentException( "Query sequence was empty" );

        // match the end of the target with the beginning of the query. We start with the whole thing
        for ( int i = 0; i < targetString.length(); i++ ) {
            String targetSub = targetString.substring( i );

            if ( queryString.indexOf( targetSub ) == 0 ) {
                return targetSub.length();
            }
        }

        return 0;
    }

    /**
     * Convert a CompositeSequence's immobilizedCharacteristics into a single sequence, using a simple merge-join
     * strategy.
     * 
     * @return
     */
    public static BioSequence collapse( CompositeSequence compositeSequence ) {

        Set copyOfProbes = new HashSet();
        for ( Iterator iter = compositeSequence.getReporters().iterator(); iter.hasNext(); ) {
            Reporter next = ( Reporter ) iter.next();

            Reporter copy = Reporter.Factory.newInstance();
            copy.setIdentifier( "copy:" + next.getIdentifier() );
            copy.setImmobilizedCharacteristic( next.getImmobilizedCharacteristic() );
            copy.setStartInBioChar( next.getStartInBioChar() );
            copyOfProbes.add( copy );
        }

        BioSequence collapsed = BioSequence.Factory.newInstance();
        collapsed.setSequence( "" );

        while ( !copyOfProbes.isEmpty() ) {

            int ol = 0;
            String nextSeqStr = null;
            int minLocation = Integer.MAX_VALUE;
            Reporter next = null;
            for ( Iterator iter = copyOfProbes.iterator(); iter.hasNext(); ) {
                Reporter probe = ( Reporter ) iter.next();

                int loc = probe.getStartInBioChar();
                if ( loc <= minLocation ) {
                    minLocation = loc;
                    next = probe;
                }
            }

            ol = SequenceManipulation.rightHandOverlap( collapsed, next.getImmobilizedCharacteristic() );

            nextSeqStr = next.getImmobilizedCharacteristic().getSequence();
            copyOfProbes.remove( next );

            String newSeq = null;
            if ( ol == 0 ) // just tack it on.
                newSeq = collapsed.getSequence() + nextSeqStr;
            else
                // add just the non-overlapping part.
                newSeq = collapsed.getSequence() + nextSeqStr.substring( ol );

            // log.debug( "Overlap of " + nextSeqStr.toUpperCase() + " with " + collapsed.seqString().toUpperCase()
            // + " is " + ol + ", new is " + newSeq.toUpperCase() );

            collapsed.setSequence( newSeq );
        }

        return collapsed;
    }

}

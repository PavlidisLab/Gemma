package edu.columbia.gemma.loader.arraydesign;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biojava.bio.seq.Sequence;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @deprecated - this is temporary. the methods should go elsewhere.
 * @author pavlidis
 * @version $Id$
 */
public class SimpleProbe implements Probe {
    private String probeId;
    private Sequence targetSequence;
    protected static final Log log = LogFactory.getLog( SimpleProbe.class );

    /**
     * @param probeSetId
     * @param ns
     */
    public SimpleProbe( String probeId, Sequence ns ) {
        this.probeId = probeId;
        this.targetSequence = ns;
    }

    /**
     * Compute just any overlap the compare sequence has with the target on the right side.
     * 
     * @param query
     * @return The index of the end of the overlap. If zero, there is no overlap. In other words, this is the amount
     *         that needs to be trimmed off the compare sequence if we are going to join it on to the target without
     *         generating redundancy.
     */
    public int rightHandOverlap( Sequence target ) {
        return this.rightHandOverlap( target, this.getSequence() );
    }

    /**
     * Compute just any overlap the compare sequence has with the target on the right side.
     * 
     * @param query
     * @param target
     * @return The index of the end of the overlap. If zero, there is no overlap. In other words, this is the amount
     *         that needs to be trimmed off the compare sequence if we are going to join it on to the target without
     *         generating redundancy.
     */
    private int rightHandOverlap( Sequence target, Sequence query   ) {

        String targetString = target.seqString();
        String queryString = query.seqString();

        // match the end of the target with the beginning of the query. We start with the whole thing
        for ( int i = 0; i < targetString.length(); i++ ) {
            String targetSub = targetString.substring( i );

            if ( queryString.indexOf( targetSub ) == 0 ) {
                return targetSub.length();
            }
        }

        return 0;
    }

   

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.bio.sequence.Probe#getIdentifier()
     */
    public String getIdentifier() {
        return this.probeId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.bio.sequence.Probe#getSequence()
     */
    public Sequence getSequence() {
        return this.targetSequence;
    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.bio.sequence.Probe#length()
     */
    public int length() {
        return targetSequence.length();
    }

}

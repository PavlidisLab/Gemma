package edu.columbia.gemma.loader.arraydesign;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.symbol.IllegalSymbolException;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @deprecated - this should be converted to a CompositeSequence. The functionality will be moved. (collapse, in
 *             particular)
 * @author pavlidis
 * @version $Id$
 */
public class AffymetrixProbeSet {
    protected static final Log log = LogFactory.getLog( AffymetrixProbeSet.class );
    private Map probes;
    private String probeSetId;
    private Sequence targetSequence;

    public AffymetrixProbeSet( String id ) {
        this( id, null );
    }

    public AffymetrixProbeSet( String id, Sequence targetSequence ) {
        this.probes = new HashMap();
        this.probeSetId = id;
        this.targetSequence = targetSequence;
    }

    /**
     * Try to convert this probe set into a single sequence.
     * 
     * @return
     */
    public Sequence collapse() {
        Sequence collapsed = null;
        try {

            Set copyOfProbes = new HashSet();
            for ( Iterator iter = probes.values().iterator(); iter.hasNext(); ) {
                AffymetrixProbe next = ( AffymetrixProbe ) iter.next();
                copyOfProbes.add( new AffymetrixProbe( "copy", "copy", DNATools.createDNASequence( next.getSequence()
                        .seqString(), "a copy" ), next.getLocationInTarget() ) );
            }

            collapsed = DNATools.createDNASequence( "", "Collapsed" );
            while ( !copyOfProbes.isEmpty() ) {
                int ol = 0;
                String nextSeqStr = null;
                int minLocation = Integer.MAX_VALUE;
                AffymetrixProbe next = null;

                for ( Iterator iter = copyOfProbes.iterator(); iter.hasNext(); ) {
                    AffymetrixProbe probe = ( AffymetrixProbe ) iter.next();

                    int loc = probe.getLocationInTarget();
                    if ( loc <= minLocation ) {
                        minLocation = loc;
                        next = probe;
                    }
                }

                ol = next.rightHandOverlap( collapsed );

                nextSeqStr = next.getSequence().seqString();
                copyOfProbes.remove( next );

                String newSeq = null;
                if ( ol == 0 ) // just tack it on.
                    newSeq = collapsed.seqString() + nextSeqStr;
                else
                    // add just the non-overlapping part.
                    newSeq = collapsed.seqString() + nextSeqStr.substring( ol );

                // log.debug( "Overlap of " + nextSeqStr.toUpperCase() + " with " + collapsed.seqString().toUpperCase()
                // + " is " + ol + ", new is " + newSeq.toUpperCase() );

                collapsed = DNATools.createDNASequence( newSeq, "Collapsed" );
            }

        } catch ( IllegalSymbolException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return collapsed;
    }

    /**
     * @param ap
     */
    public void add( AffymetrixProbe ap ) {
        this.probes.put( ap.getIdentifier(), ap );
    }

    public AffymetrixProbe getProbe( String id ) {
        return ( AffymetrixProbe ) probes.get( id );
    }

    public String getProbeSequence( String id ) {
        return ( ( AffymetrixProbe ) probes.get( id ) ).getSequence().seqString();
    }

    public String getProbeSetId() {
        return this.probeSetId;
    }

    public Sequence getTargetSequence() {
        return this.targetSequence;
    }

    public Iterator iterator() {
        return this.probes.keySet().iterator();
    }

    public void setTargetSequence( Sequence targetSequence ) {
        this.targetSequence = targetSequence;
    }

    public int size() {
        return this.probes.size();
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return new ToStringBuilder( this ).append( "probeSetId", this.probeSetId ).toString();
    }

}

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

import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.Reporter;
import edu.columbia.gemma.sequence.biosequence.BioSequence;
import edu.columbia.gemma.tools.SequenceManipulation;

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
     * @param ap
     */
    public void add( Reporter ap ) {
        this.probes.put( ap.getIdentifier(), ap );
    }

    public Reporter getProbe( String id ) {
        return ( Reporter ) probes.get( id );
    }

    public String getProbeSequence( String id ) {
        return ( ( Reporter ) probes.get( id ) ).getImmobilizedCharacteristic().getSequence();
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

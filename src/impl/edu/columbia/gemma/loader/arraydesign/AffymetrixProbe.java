package edu.columbia.gemma.loader.arraydesign;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biojava.bio.seq.Sequence;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @deprecated - this should be converted to a Reporter.
 * @author pavlidis
 * @version $Id$
 */
public class AffymetrixProbe extends SimpleProbe {

    protected static final Log log = LogFactory.getLog( AffymetrixProbe.class );

    private String probeSetIdentifier;
    private int locationInTarget;

    /**
     * @param identifier
     * @param sequence
     * @param locationInTarget
     */
    public AffymetrixProbe( String probeSetIdentifier, String identifier, Sequence sequence, int locationInTarget ) {
        super( identifier, sequence );
        this.probeSetIdentifier = probeSetIdentifier;
        this.locationInTarget = locationInTarget;
    }

    public int getLocationInTarget() {
        return this.locationInTarget;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return new ToStringBuilder( this ).append( "sequence", this.getSequence() ).append( "identifier", this.getIdentifier() )
                .append( "locationInTarget", this.locationInTarget ).toString();
    }

    public String getProbeSetIdentifier() {
        return this.probeSetIdentifier;
    }
    

}

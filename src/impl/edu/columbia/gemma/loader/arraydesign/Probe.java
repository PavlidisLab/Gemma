package edu.columbia.gemma.loader.arraydesign;

import org.biojava.bio.seq.Sequence;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @deprecated  - this is temporary.
 * @author pavlidis
 * @version $Id$
 */
public interface Probe {

    public String getIdentifier();

    public Sequence getSequence();

    public int length();

}
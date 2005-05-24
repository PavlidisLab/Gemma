package edu.columbia.gemma.loader.genome.gene;

import java.util.Collection;

import edu.columbia.gemma.genome.Gene;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public interface GeneLoader {

    public void create( Collection col );

    public void create( Gene gene );

    public void removeAll();

    public void removeAll( Collection collection );

}

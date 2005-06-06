package edu.columbia.gemma.loader.genome.gene;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.GeneDao;
import edu.columbia.gemma.loader.loaderutils.Loader;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class GeneLoaderImpl implements Loader {
    protected static final Log log = LogFactory.getLog( GeneLoaderImpl.class );

    private GeneDao geneDao;

    /**
     * Persist genes in collection.
     * 
     * @param col
     */
    public void create( Collection col ) {
        assert geneDao != null;
        for ( Iterator iter = col.iterator(); iter.hasNext(); ) {
            Gene g = ( Gene ) iter.next();

            if ( !( geneDao.findByNcbiId( Integer.parseInt( g.getNcbiId() ) ).size() > 0 ) ) geneDao.create( g );
        }
    }

    /**
     * Persist gene.
     * 
     * @param gene
     */
    public void create( Object obj ) {
        // TODO Auto-generated method stub
    }

    /**
     * @return Returns the geneDao.
     */
    public GeneDao getGeneDao() {
        return geneDao;
    }

    /**
     * 
     */
    public void removeAll() {
        assert geneDao != null;
        Collection col = geneDao.findAllGenes();
        geneDao.remove( col );
    }

    public void removeAll( Collection col ) {
        Iterator iter = col.iterator();
        while ( iter.hasNext() ) {
            Gene g = ( Gene ) iter.next();
            this.getGeneDao().remove( g );
        }
    }

    /**
     * @param geneDao The geneDao to set.
     */
    public void setGeneDao( GeneDao geneDao ) {
        this.geneDao = geneDao;
    }
}

package edu.columbia.gemma.loader.association;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.association.Gene2GOAssociation;
import edu.columbia.gemma.association.Gene2GOAssociationDao;
import edu.columbia.gemma.loader.loaderutils.LoaderTools;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="gene2GOAssociationLoader"
 * @spring.property name="gene2GOAssociationDao" ref="gene2GOAssociationDao"
 */
public class Gene2GOAssociationLoaderImpl {

    protected static final Log log = LogFactory.getLog( Gene2GOAssociationLoaderImpl.class );

    private Gene2GOAssociationDao gene2GOAssociationDao;

    /**
     * @param oeCol
     * @param dbEntry TODO
     */
    public void create( Collection<Gene2GOAssociation> g2GoCol ) {

        log.info( "persisting Gemma objects (if object exists it will not be persisted) ..." );

        Collection<Gene2GOAssociation> g2GoColFromDatabase = getGene2GOAssociationDao().findAllGene2GOAssociations();

        int count = 0;
        for ( Gene2GOAssociation g2Go : g2GoCol ) {
            assert gene2GOAssociationDao != null;

            if ( g2GoColFromDatabase.size() == 0 ) {
                getGene2GOAssociationDao().create( g2Go );
                count++;
                LoaderTools.objectsPersistedUpdate( count, 1000, "Gene2GOAssociation objects" );

            } else {
                for ( Gene2GOAssociation g2GoFromDatabase : g2GoColFromDatabase ) {
                    if ( ( !g2Go.getGene().equals( g2GoFromDatabase.getGene() ) )
                            && ( !g2Go.getAssociatedOntologyEntry().equals(
                                    g2GoFromDatabase.getAssociatedOntologyEntry() ) ) ) {
                        getGene2GOAssociationDao().create( g2Go );
                        count++;
                        LoaderTools.objectsPersistedUpdate( count, 1000, "Gene2GOAssociation objects" );
                    }
                }
            }
        }
    }

    /**
     * @param gene2GOAssociation
     */
    public void create( Gene2GOAssociation gene2GOAssociation ) {
        getGene2GOAssociationDao().create( gene2GOAssociation );
    }

    /**
     * @return Returns the gene2GOAssociationDao.
     */
    public Gene2GOAssociationDao getGene2GOAssociationDao() {
        return gene2GOAssociationDao;
    }

    /**
     * @param gene2GOAssociationDao The gene2GOAssociationDao to set.
     */
    public void setGene2GOAssociationDao( Gene2GOAssociationDao gene2GOAssociationDao ) {
        this.gene2GOAssociationDao = gene2GOAssociationDao;
    }

}

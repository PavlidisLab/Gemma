/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package ubic.gemma.model.genome;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;

import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.util.BusinessKey;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.genome.Gene
 */
public class GeneDaoImpl extends ubic.gemma.model.genome.GeneDaoBase {

    private static Log log = LogFactory.getLog( GeneDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#find(ubic.gemma.model.genome.Gene)
     */
    @Override
    public Gene find( Gene gene ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( Gene.class );

            BusinessKey.checkKey( gene );

            BusinessKey.createQueryObject( queryObject, gene );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {

                    /*
                     * this can happen in semi-rare cases in queries by symbol, where the gene symbol is not unique for
                     * the taxon and the query did not have the gene name to further restrict the query.
                     */
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + Gene.class.getName() + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( Gene ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#findOrCreate(ubic.gemma.model.genome.Gene)
     */
    @Override
    public Gene findOrCreate( Gene gene ) {
        Gene existingGene = this.find( gene );
        if ( existingGene != null ) {
            return existingGene;
        }
        // We consider this abnormal because we expect most genes to have been loaded into the system already.
        log.warn( "*** Creating new gene: " + gene + " ***" );
        return ( Gene ) create( gene );
    }

    /**
     * Gets a count of the CompositeSequences related to the gene identified by the given id.
     * @param id
     * @return Collection
     */
    @Override
    protected long handleGetCompositeSequenceCountById( long id ) throws Exception {
        long count = 0;
        final String queryString = "select count(distinct compositeSequence) from GeneImpl as gene,  BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence where gene.products.id=bs2gp.geneProduct.id "
            + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
            + " and gene.id = :id ";
        /*final String queryString = "select count(distinct compositeSequence) from BioSequence2GeneProductImpl as bs2gp,CompositeSequenceImpl as compositeSequence "
                + "where bs2gp.geneProduct.id in (select gene.products.id from GeneImpl as gene where gene.id = :id) and "
                + "bs2gp.bioSequence = compositeSequence.biologicalCharacteristic";*/
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setLong( "id", id );
            queryObject.setMaxResults( 1 );

            count = ( ( Integer ) queryObject.uniqueResult() ).longValue();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return count;
    }
    
    /**
     * Gets all the CompositeSequences related to the gene identified by the given id.
     * @param id
     * @return Collection
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetCompositeSequencesById( long id ) throws Exception {
        Collection<CompositeSequence> compSeq = null;
        final String queryString = "select distinct compositeSequence from GeneImpl as gene,  BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence where gene.products.id=bs2gp.geneProduct.id "
            + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
            + " and gene.id = :id ";        
        /*final String queryString = "select distinct compositeSequence from BioSequence2GeneProductImpl as bs2gp,CompositeSequenceImpl as compositeSequence "
                + "where bs2gp.geneProduct.id in (select gene.products.id from GeneImpl as gene where gene.id = :id) and "
                + "bs2gp.bioSequence = compositeSequence.biologicalCharacteristic";*/
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setLong( "id", id );
            compSeq = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return compSeq;
    }

    /**
     * Gets all the genes referred to by the alias defined by the search string.
     * @param search
     * @return Collection
     */   
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetByGeneAlias( String search ) throws Exception {
        Collection<Gene> genes = null;
        final String queryString = "select distinct gene from GeneImpl as gene inner join gene.aliases where gene.aliases.Alias like :search";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setString( "search", search );
            genes = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return genes;
    }
    
    
    /**
     * Gets all the DesignElementDataVectors that are related to the gene identified by the given ID.
     * @param id 
     * @return Collection
     */    
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetCoexpressedElementsById( long id ) throws Exception {
        Collection<DesignElementDataVector> vectors = null;
        final String queryString = "select distinct compositeSequence.designElementDataVectors from GeneImpl as gene,  BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence where gene.products.id=bs2gp.geneProduct.id "
            + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
            + " and gene.id = :id ";        
        /*final String queryString = "select distinct compositeSequence from BioSequence2GeneProductImpl as bs2gp,CompositeSequenceImpl as compositeSequence "
                + "where bs2gp.geneProduct.id in (select gene.products.id from GeneImpl as gene where gene.id = :id) and "
                + "bs2gp.bioSequence = compositeSequence.biologicalCharacteristic";*/
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setLong( "id", id );
            vectors = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return vectors;
    }
    
    
    /**
     * Gets all the genes that are coexpressed with another gene, identified by the given ID.
     * @param id 
     * @return Collection
     */    
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetCoexpressedGenesById( long id ) throws Exception {
        Collection<Gene> genes = new HashSet<Gene>();
        final String queryStringFirstVector = 
            // source tables
            "select distinct coGene from GeneImpl as gene, BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence," 
            // join table
            + " Probe2ProbeCoexpressionImpl as p2pc,"
            // target tables
            + " GeneImpl as coGene,BioSequence2GeneProductImpl as coBs2gp, CompositeSequenceImpl as coCompositeSequence" 
            + " where gene.products.id=bs2gp.geneProduct.id "
            + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
            + " and compositeSequence.designElementDataVectors=p2pc.firstVector " 
            + " and coCompositeSequence.designElementDataVectors=p2pc.secondVector "
            + " and coCompositeSequence.biologicalCharacteristic=coBs2gp.bioSequence "
            + " and coGene.products.id=coBs2gp.geneProduct.id " 
            + " and gene.id = :id ";   
        final String queryStringSecondVector = 
            // source tables
            "select distinct coGene from GeneImpl as gene, BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence," 
            // join table
            + " Probe2ProbeCoexpressionImpl as p2pc,"
            // target tables
            + " GeneImpl as coGene,BioSequence2GeneProductImpl as coBs2gp, CompositeSequenceImpl as coCompositeSequence" 
            + " where gene.products.id=bs2gp.geneProduct.id "
            + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
            + " and compositeSequence.designElementDataVectors=p2pc.secondVector " 
            + " and coCompositeSequence.designElementDataVectors=p2pc.firstVector "
            + " and coCompositeSequence.biologicalCharacteristic=coBs2gp.bioSequence "
            + " and coGene.products.id=coBs2gp.geneProduct.id " 
            + " and gene.id = :id ";    
        try {
            // do query joining coexpressed genes through the firstVector to the secondVector
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryStringFirstVector );
            queryObject.setLong( "id", id );
            genes.addAll( queryObject.list() );
            // do query joining coexpressed genes through the secondVector to the firstVector           
            queryObject = super.getSession( false ).createQuery( queryStringSecondVector );
            queryObject.setLong( "id", id );
            genes.addAll( queryObject.list() );

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return genes;
    }
    
    /**
     * Gets all the genes that are coexpressed with the given gene.
     * @param gene
     * @return Collection
     */    
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetCoexpressedGenes(Gene gene) throws Exception {
        return this.handleGetCoexpressedGenesById( gene.getId() );
    }
    
    /**
     * Gets all the DesignElementDataVectors that are related to the given gene.
     * @param gene
     * @return Collection
     */    
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetCoexpressedElements( Gene gene ) throws Exception {
        return this.handleGetCoexpressedElementsById( gene.getId() );
    }


}
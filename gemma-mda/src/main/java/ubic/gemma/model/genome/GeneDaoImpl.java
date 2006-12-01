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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Scrollable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;

import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.coexpression.CoexpressionValueObject;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.util.BusinessKey;
import ubic.gemma.util.TaxonUtility;

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
    @SuppressWarnings("unchecked")
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
                    log.error( "Multiple genes found for " + gene + ":" );
                    debug( results );

                    Collections.sort( results, new Comparator<Gene>() {
                        public int compare( Gene arg0, Gene arg1 ) {
                            return arg0.getId().compareTo( arg1.getId() );
                        }
                    } );
                    result = results.iterator().next();
                    log.error( "Returning arbitrary gene: " + result );
                    // throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    // "More than one instance of '" + Gene.class.getName() + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( Gene ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    private void debug( List results ) {

        StringBuilder buf = new StringBuilder();
        buf.append( "\n" );
        for ( Object object : results ) {
            Gene g = ( Gene ) object;
            buf.append( g + "\n" );
        }
        log.error( buf );

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

    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from GeneImpl";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( query );

            return ( Integer ) queryObject.iterate().next();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /**
     * Gets a count of the CompositeSequences related to the gene identified by the given id.
     * 
     * @param id
     * @return Collection
     */
    @Override
    protected long handleGetCompositeSequenceCountById( long id ) throws Exception {
        long count = 0;
        final String queryString = "select count(distinct compositeSequence) from GeneImpl as gene,  BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence where gene.products.id=bs2gp.geneProduct.id "
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence " + " and gene.id = :id ";
        /*
         * final String queryString = "select count(distinct compositeSequence) from BioSequence2GeneProductImpl as
         * bs2gp,CompositeSequenceImpl as compositeSequence " + "where bs2gp.geneProduct.id in (select gene.products.id
         * from GeneImpl as gene where gene.id = :id) and " + "bs2gp.bioSequence =
         * compositeSequence.biologicalCharacteristic";
         */
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
     * 
     * @param id
     * @return Collection
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetCompositeSequencesById( long id ) throws Exception {
        Collection<CompositeSequence> compSeq = null;
        final String queryString = "select distinct compositeSequence from GeneImpl as gene,  BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence where gene.products.id=bs2gp.geneProduct.id "
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence " + " and gene.id = :id ";
        /*
         * final String queryString = "select distinct compositeSequence from BioSequence2GeneProductImpl as
         * bs2gp,CompositeSequenceImpl as compositeSequence " + "where bs2gp.geneProduct.id in (select gene.products.id
         * from GeneImpl as gene where gene.id = :id) and " + "bs2gp.bioSequence =
         * compositeSequence.biologicalCharacteristic";
         */
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
     * 
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
     * 
     * @param id
     * @return Collection
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetCoexpressedElementsById( long id ) throws Exception {
        Collection<DesignElementDataVector> vectors = null;
        final String queryString = "select distinct compositeSequence.designElementDataVectors from GeneImpl as gene,  BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence where gene.products.id=bs2gp.geneProduct.id "
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence " + " and gene.id = :id ";
        /*
         * final String queryString = "select distinct compositeSequence from BioSequence2GeneProductImpl as
         * bs2gp,CompositeSequenceImpl as compositeSequence " + "where bs2gp.geneProduct.id in (select gene.products.id
         * from GeneImpl as gene where gene.id = :id) and " + "bs2gp.bioSequence =
         * compositeSequence.biologicalCharacteristic";
         */
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
     * 
     * @param id
     * @return Collection
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Object handleGetCoexpressedGenes( Gene gene, Collection ees, Integer stringency ) throws Exception {
        Gene givenG = gene;
        long id = givenG.getId();
        
        String p2pClassName;
        if (TaxonUtility.isHuman(givenG.getTaxon()))
            p2pClassName = "HumanProbeCoExpressionImpl";
        else if (TaxonUtility.isMouse(givenG.getTaxon()))
            p2pClassName = "MouseProbeCoExpressionImpl";       
        else if (TaxonUtility.isRat(givenG.getTaxon()))
            p2pClassName = "RatProbeCoExpressionImpl";        
        else //must be other
            p2pClassName = "OtherProbeCoExpressionImpl";
        
        Map<Long,CoexpressionValueObject> geneMap = new HashMap<Long,CoexpressionValueObject>();
        
        String queryStringFirstVector =
            // return values
            "select coGene.id, coGene.name, coGene.officialName,p2pc.firstVector.expressionExperiment.id, p2pc.firstVector.expressionExperiment.shortName, p2pc.firstVector.expressionExperiment.name" 
                // source tables
                + " from GeneImpl as gene, BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence,"
                // join table
                + p2pClassName + " as p2pc,"
                // target tables
                + " GeneImpl as coGene,BioSequence2GeneProductImpl as coBs2gp, CompositeSequenceImpl as coCompositeSequence"
                + " where gene.products.id=bs2gp.geneProduct.id "
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
                + " and compositeSequence.designElementDataVectors.id=p2pc.firstVector.id "
                + " and coCompositeSequence.designElementDataVectors.id=p2pc.secondVector.id "
                + " and coCompositeSequence.biologicalCharacteristic=coBs2gp.bioSequence "
                + " and coGene.products.id=coBs2gp.geneProduct.id " + " and gene.id = :id ";

        String queryStringSecondVector =
            // return values
            "select coGene.id, coGene.name, coGene.officialName,p2pc.secondVector.expressionExperiment.id, p2pc.secondVector.expressionExperiment.shortName, p2pc.secondVector.expressionExperiment.name " 
                // source tables
                + "from GeneImpl as gene, BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence,"
                // join table
                + p2pClassName + " as p2pc,"
                // target tables
                + "GeneImpl as coGene,BioSequence2GeneProductImpl as coBs2gp, CompositeSequenceImpl as coCompositeSequence"
                + " where gene.products.id=bs2gp.geneProduct.id "
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
                + " and compositeSequence.designElementDataVectors.id=p2pc.secondVector.id "
                + " and coCompositeSequence.designElementDataVectors.id=p2pc.firstVector.id "
                + " and coCompositeSequence.biologicalCharacteristic=coBs2gp.bioSequence "
                + " and coGene.products.id=coBs2gp.geneProduct.id " + " and gene.id = :id ";
                
        // OPTIONAL joins
        // if there are expressionExperiment arguments
        Collection<Long> eeIds = new ArrayList<Long>();
        CoexpressionCollectionValueObject coexpressions = new CoexpressionCollectionValueObject();
        if (ees.size() > 0) {
            queryStringFirstVector += " and p2pc.firstVector.expressionExperiment.id in (:ees) ";
            queryStringSecondVector += " and p2pc.secondVector.expressionExperiment.id in (:ees) ";
            for ( Iterator iter = ees.iterator(); iter.hasNext(); ) {
                ExpressionExperiment e = ( ExpressionExperiment ) iter.next();
                eeIds.add( e.getId() );                
            }   
        }
        
        // group by clause
//        queryStringFirstVector += " group by coGene ";
//        queryStringSecondVector += " group by coGene ";
        
        // having clause, if the stringency is given
//        queryStringFirstVector += " having count(distinct p2pc.firstVector.expressionExperiment) >= :stringency ";
//        queryStringSecondVector += " having count(distinct p2pc.secondVector.expressionExperiment) >= :stringency ";
        
        try {
            // do query joining coexpressed genes through the firstVector to the secondVector
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryStringFirstVector );
            queryObject.setLong( "id", id );
            if (ees.size() > 0) {
                queryObject.setParameterList( "ees", eeIds );
            }
//            queryObject.setInteger( "stringency", stringency );
            
            // put genes in the geneSet
            ScrollableResults scroll = queryObject.scroll(ScrollMode.FORWARD_ONLY);
            while (scroll.next()) {
                CoexpressionValueObject vo;
                Long geneId = scroll.getLong( 0 );
                // check to see if geneId is already in the geneMap
                if (geneMap.containsKey( geneId )) {
                    vo = geneMap.get( geneId );
                }
                else {
                    vo = new CoexpressionValueObject();
                    vo.setGeneId( geneId );
                    vo.setGeneName( scroll.getString( 1 ) );
                    vo.setGeneOfficialName( scroll.getString( 2 ) );
                    geneMap.put(geneId,vo);
                }
                // add the expression experiment
                ExpressionExperimentValueObject eeVo = new ExpressionExperimentValueObject();
                eeVo.setId( scroll.getLong( 3 ).toString() );
                eeVo.setShortName( scroll.getString( 4 ) );
                eeVo.setName( scroll.getString( 5 ) );
                vo.addExpressionExperimentValueObject( eeVo );
            }
            
            // do query joining coexpressed genes through the secondVector to the firstVector
            queryObject = super.getSession( false ).createQuery( queryStringSecondVector );
            queryObject.setLong( "id", id );
            if (ees.size() > 0) {
                queryObject.setParameterList( "ees", eeIds );
            }
//            queryObject.setInteger( "stringency", stringency );
            
            // put genes in the geneSet
            scroll = queryObject.scroll(ScrollMode.FORWARD_ONLY);
            while (scroll.next()) {
                CoexpressionValueObject vo;
                Long geneId = scroll.getLong( 0 );
                // check to see if geneId is already in the geneMap
                if (geneMap.containsKey( geneId )) {
                    vo = geneMap.get( geneId );
                }
                else {
                    vo = new CoexpressionValueObject();
                    vo.setGeneId( geneId );
                    vo.setGeneName( scroll.getString( 1 ) );
                    vo.setGeneOfficialName( scroll.getString( 2 ) );
                    geneMap.put(geneId,vo);
                }
                // add the expression experiment
                ExpressionExperimentValueObject eeVo = new ExpressionExperimentValueObject();
                eeVo.setId( scroll.getLong( 3 ).toString() );
                eeVo.setShortName( scroll.getString( 4 ) );
                eeVo.setName( scroll.getString( 5 ) );
                vo.addExpressionExperimentValueObject( eeVo );
            }
            
            // add count of original matches to coexpression data
            coexpressions.setLinkCount( geneMap.size() );
            // parse out stringency failures
            Set keys = geneMap.keySet();
            for ( Object object : keys ) {
                Long key = (Long) object;
                if (geneMap.get( key ).getExpressionExperimentValueObjects().size() >= stringency) {
                    coexpressions.getCoexpressionData().add( geneMap.get( key ) );
                }
            }
            // add count of pruned matches to coexpression data
            coexpressions.setStringencyLinkCount( coexpressions.getCoexpressionData().size() );

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return coexpressions;
    } 

    /**
     * Gets all the DesignElementDataVectors that are related to the given gene.
     * 
     * @param gene
     * @return Collection
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetCoexpressedElements( Gene gene ) throws Exception {
        return this.handleGetCoexpressedElementsById( gene.getId() );
    }

    @Override
    protected Collection handleGetGenesByTaxon( Taxon taxon ) throws Exception {
        Collection<Gene> genes = null;
        final String queryString = "select gene from GeneImpl as gene where gene.taxon = :taxon ";
    
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "taxon", taxon );
            genes = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return genes;
        
    }

}
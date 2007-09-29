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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.type.DoubleType;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;

import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.coexpression.CoexpressionTypeValueObject;
import ubic.gemma.model.coexpression.CoexpressionValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
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
     * @see ubic.gemma.model.genome.GeneDaoBase#handleLoadGenes(ubic.gemma.model.genome.Taxon)
     */
    @Override
    protected Collection handleLoadGenes( Taxon taxon ) throws Exception {
        final String queryString = "select gene from GeneImpl as gene where gene.taxon = :taxon"
                + " and (gene.class = " + CoexpressionCollectionValueObject.GENE_IMPL + " )";

        Collection genes;
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "taxon", taxon );

            genes = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return genes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleLoadPredictedGenes(ubic.gemma.model.genome.Taxon)
     */
    @Override
    protected Collection handleLoadPredictedGenes( Taxon taxon ) throws Exception {
        final String queryString = "select gene from GeneImpl as gene where gene.taxon = :taxon"
                + " and (gene.class = " + CoexpressionCollectionValueObject.PREDICTED_GENE_IMPL + ")";

        Collection predictedGenes;
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "taxon", taxon );

            predictedGenes = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return predictedGenes;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleLoadProbeAlignedRegions(ubic.gemma.model.genome.Taxon)
     */
    @Override
    protected Collection handleLoadProbeAlignedRegions( Taxon taxon ) throws Exception {
        final String queryString = "select gene from GeneImpl as gene where gene.taxon = :taxon"
                + " and (gene.class = " + CoexpressionCollectionValueObject.PROBE_ALIGNED_REGION_IMPL + ")";

        Collection pars;
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "taxon", taxon );

            pars = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return pars;
    }

    /**
     * Gets all the CompositeSequences related to the gene identified by the given gene and arrayDesign.
     * 
     * @param gene, arrayDesign
     * @return Collection
     */
    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleGetCompositeSequences(ubic.gemma.model.genome.Gene,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetCompositeSequences( Gene gene, ArrayDesign arrayDesign ) throws Exception {
        Collection<CompositeSequence> compSeq = null;
        final String queryString = "select distinct cs from GeneImpl as gene inner join gene.products gp,  BioSequence2GeneProductImpl"
                + " as bs2gp, CompositeSequenceImpl as cs where gp=bs2gp.geneProduct "
                + " and cs.biologicalCharacteristic=bs2gp.bioSequence "
                + " and gene = :gene and cs.arrayDesign = :arrayDesign ";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "arrayDesign", arrayDesign );
            queryObject.setParameter( "gene", gene );
            compSeq = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return compSeq;
    }

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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDao#geneValueObjectToEntity(ubic.gemma.model.genome.gene.GeneValueObject)
     */
    public Gene geneValueObjectToEntity( GeneValueObject geneValueObject ) {
        final String queryString = "select distinct gene from GeneImpl gene where gene.id = :id";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setLong( "id", geneValueObject.getId() );
            java.util.List results = queryObject.list();

            if ( ( results == null ) || ( results.size() == 0 ) ) return null;

            return ( Gene ) results.iterator().next();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /**
     * @param stringency
     * @param geneMap
     * @param coexpressions
     */
    @SuppressWarnings("unchecked")
    private void postProcessing( GeneMap geneMap, CoexpressionCollectionValueObject coexpressions ) throws Exception {

        postProcessGeneImpls( geneMap.getGeneImplMap(), coexpressions );

        postProcessProbeAlignedRegions( geneMap.getProbeAlignedRegionMap(), coexpressions );

        postProcessPredictedGenes( geneMap.getPredictedGeneMap(), coexpressions );

    }

    /**
     * @param genes
     * @param coexpressions
     */
    private void postProcessPredictedGenes( Map<Long, CoexpressionValueObject> genes,
            CoexpressionCollectionValueObject coexpressions ) {

        List<Long> allEEIds = new ArrayList<Long>( coexpressions.getGeneCoexpressionType().getExpressionExperimentIds() );

        CoexpressionTypeValueObject predictedCoexpressionType = coexpressions.getPredictedCoexpressionType();
        for ( Long geneId : genes.keySet() ) {
            CoexpressionValueObject coExValObj = genes.get( geneId );
            incrementRawEEContributions( coExValObj.getExpressionExperiments(), predictedCoexpressionType );

            coExValObj.computeExperimentBits( allEEIds );
            if ( ( coExValObj.getPositiveLinkCount() != null ) || ( coExValObj.getNegativeLinkCount() != null ) ) {
                coexpressions.getPredictedCoexpressionData().add( coExValObj );

                if ( coExValObj.getPositiveLinkCount() != null )
                    incrementEEContributions( coExValObj.getEEContributing2PositiveLinks(), predictedCoexpressionType );
                else
                    incrementEEContributions( coExValObj.getEEContributing2NegativeLinks(), predictedCoexpressionType );
            }
        }
        predictedCoexpressionType.setNumberOfGenes( genes.size() );
    }

    /**
     * @param genes
     * @param coexpressions
     */
    private void postProcessProbeAlignedRegions( Map<Long, CoexpressionValueObject> genes,
            CoexpressionCollectionValueObject coexpressions ) {

        int numStringencyProbeAlignedRegions = 0;
        List<Long> allEEIds = new ArrayList<Long>( coexpressions.getGeneCoexpressionType().getExpressionExperimentIds() );

        CoexpressionTypeValueObject probeAlignedCoexpressionType = coexpressions.getProbeAlignedCoexpressionType();
        for ( Long geneId : genes.keySet() ) {
            CoexpressionValueObject coExValObj = genes.get( geneId );

            coExValObj.computeExperimentBits( allEEIds );
            incrementRawEEContributions( coExValObj.getExpressionExperiments(), probeAlignedCoexpressionType );

            if ( ( coExValObj.getPositiveLinkCount() != null ) || ( coExValObj.getNegativeLinkCount() != null ) ) {
                numStringencyProbeAlignedRegions++;
                coexpressions.getProbeAlignedCoexpressionData().add( coExValObj );

                if ( coExValObj.getPositiveLinkCount() != null )
                    incrementEEContributions( coExValObj.getEEContributing2PositiveLinks(),
                            probeAlignedCoexpressionType );
                else
                    incrementEEContributions( coExValObj.getEEContributing2NegativeLinks(),
                            probeAlignedCoexpressionType );
            }

        }

        probeAlignedCoexpressionType.setNumberOfGenes( genes.size() );
    }

    /**
     * @param genes
     * @param coexpressions
     * @throws Exception
     */
    private void postProcessGeneImpls( Map<Long, CoexpressionValueObject> genes,
            CoexpressionCollectionValueObject coexpressions ) throws Exception {

        int positiveLinkCount = 0;
        int negativeLinkCount = 0;
        int numStringencyGenes = 0;

        Map<Long, Collection<Gene>> querySpecificity = getGenes( coexpressions.getQueryGeneProbes() );
        coexpressions.addQueryGeneSpecifityInfo( querySpecificity );
        Collection<Long> allQuerySpecificEE = coexpressions.getQueryGeneSpecificExpressionExperiments();

        Map<Long, Collection<Long>> allSpecificEE = coexpressions.getGeneCoexpressionType()
                .getSpecificExpressionExperiments();
        List<Long> allEEIds = new ArrayList<Long>( coexpressions.getGeneCoexpressionType().getExpressionExperimentIds() );

        for ( Long geneId : genes.keySet() ) {
            CoexpressionValueObject coExValObj = genes.get( geneId );

            // determine which EE's that contributed to this gene's coexpression were non-specific
            // an ee is specific iff the ee is specific for the query gene and the target gene.
            Collection<Long> nonspecificEE = new HashSet<Long>( coExValObj.getExpressionExperiments() );
            Collection<Long> specificEE = new HashSet<Long>( allSpecificEE.get( geneId ) ); // get the EE's that are
            // specific for the target
            // gene
            specificEE.retainAll( allQuerySpecificEE ); // get the EE's that are specific for both the target and
            // the query gene
            nonspecificEE.removeAll( specificEE );
            coExValObj.setNonspecificEE( nonspecificEE );
            coExValObj.computeExperimentBits( allEEIds );

            if ( coExValObj.getGeneName().equalsIgnoreCase( "RPL27" ) ) log.debug( "at gene rpl27" );

            // figure out which genes where culprits for making this gene non-specific
            Collection<Long> probes = coExValObj.getProbes();
            for ( Long eeID : nonspecificEE ) {
                for ( Long probeID : probes ) {
                    if ( coexpressions.getGeneCoexpressionType().getNonSpecificGenes( eeID, probeID ) != null ) {
                        for ( Long geneID : coexpressions.getGeneCoexpressionType().getNonSpecificGenes( eeID, probeID ) ) {
                            coExValObj.addNonSpecificGene( genes.get( geneID ).getGeneName() );
                            if ( geneID == coexpressions.getQueryGene().getId() )
                                coExValObj.setHybridizesWithQueryGene( true );
                        }
                    }
                }

            }
            coExValObj.getNonSpecificGenes().remove( coExValObj.getGeneOfficialName() );

            boolean added = false;

            incrementRawEEContributions( coExValObj.getExpressionExperiments(), coexpressions.getGeneCoexpressionType() );

            if ( coExValObj.getPositiveLinkCount() != null ) {
                numStringencyGenes++;
                positiveLinkCount++;
                added = true;
                // add in coexpressions that match stringency
                coexpressions.getCoexpressionData().add( coExValObj );
                // add in expression experiments that match stringency
                // update the link count for that EE
                incrementEEContributions( coExValObj.getEEContributing2PositiveLinks(), coexpressions
                        .getGeneCoexpressionType() );
            }

            if ( coExValObj.getNegativeLinkCount() != null ) {
                negativeLinkCount++;
                // add in expression experiments that match stringency
                // update the link count for that EE
                incrementEEContributions( coExValObj.getEEContributing2NegativeLinks(), coexpressions
                        .getGeneCoexpressionType() );

                if ( added ) continue; // no point in adding or counting the same element twice
                coexpressions.getCoexpressionData().add( coExValObj );
                numStringencyGenes++;

            }
        }

        // add count of pruned matches to coexpression data
        coexpressions.getGeneCoexpressionType().setPositiveStringencyLinkCount( positiveLinkCount );
        coexpressions.getGeneCoexpressionType().setNegativeStringencyLinkCount( negativeLinkCount );
        coexpressions.getGeneCoexpressionType().setNumberOfGenes( genes.size() );
    }

    /**
     * @param contributingEEs
     * @param coexpressions
     */
    private void incrementEEContributions( Collection<Long> contributingEEs, CoexpressionTypeValueObject coexpressions ) {

        for ( Long eeID : contributingEEs ) {
            ExpressionExperimentValueObject eeVo = coexpressions.getExpressionExperiment( eeID );

            if ( eeVo == null ) {
                log.warn( "Looked for " + eeID + " but not in coexpressions object" );
                continue;
            }
            if ( eeVo.getCoexpressionLinkCount() == null )
                eeVo.setCoexpressionLinkCount( new Long( 1 ) );
            else
                eeVo.setCoexpressionLinkCount( eeVo.getCoexpressionLinkCount() + 1 );

        }

    }

    /**
     * @param contributingEEs
     * @param coexpressions
     */
    private void incrementRawEEContributions( Collection<Long> contributingEEs,
            CoexpressionTypeValueObject coexpressions ) {

        for ( Long eeID : contributingEEs ) {
            ExpressionExperimentValueObject eeVo = coexpressions.getExpressionExperiment( eeID );

            if ( eeVo == null ) {
                log.warn( "Looked for " + eeID + " but not in coexpressions object" );
                continue;
            }
            if ( eeVo.getRawCoexpressionLinkCount() == null )
                eeVo.setRawCoexpressionLinkCount( new Long( 1 ) );
            else
                eeVo.setRawCoexpressionLinkCount( eeVo.getRawCoexpressionLinkCount() + 1 );
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

    /**
     * @param ees
     * @return
     */
    private Collection<Long> getEEIds( Collection ees ) {
        Collection<Long> eeIds = new ArrayList<Long>();
        if ( ees != null && ees.size() > 0 ) {
            for ( Iterator iter = ees.iterator(); iter.hasNext(); ) {
                ExpressionExperiment e = ( ExpressionExperiment ) iter.next();
                eeIds.add( e.getId() );
            }
        }
        return eeIds;
    }

    /**
     * @param givenG
     * @return
     */
    private String getP2PClassName( Gene givenG ) {
        if ( TaxonUtility.isHuman( givenG.getTaxon() ) )
            return "HumanProbeCoExpressionImpl";
        else if ( TaxonUtility.isMouse( givenG.getTaxon() ) )
            return "MouseProbeCoExpressionImpl";
        else if ( TaxonUtility.isRat( givenG.getTaxon() ) )
            return "RatProbeCoExpressionImpl";
        else
            return "OtherProbeCoExpressionImpl";

    }

    /**
     * @param className
     * @return
     */
    private String getP2PTableNameForClassName( String className ) {
        if ( className.equals( "HumanProbeCoExpressionImpl" ) )
            return "HUMAN_PROBE_CO_EXPRESSION";
        else if ( className.equals( "MouseProbeCoExpressionImpl" ) )
            return "MOUSE_PROBE_CO_EXPRESSION";
        else if ( className.equals( "RatProbeCoExpressionImpl" ) )
            return "RAT_PROBE_CO_EXPRESSION";
        else
            return "OTHER_PROBE_CO_EXPRESSION";
    }

    /**
     * @param p2pClassName
     * @param in
     * @param out
     * @return
     */
    private synchronized String getFastNativeQueryString( String p2pClassName, String in, String out,
            Collection<Long> eeIds ) {
        String inKey = in.equals( "firstVector" ) ? "FIRST_DESIGN_ELEMENT_FK" : "SECOND_DESIGN_ELEMENT_FK";
        String outKey = out.equals( "firstVector" ) ? "FIRST_DESIGN_ELEMENT_FK" : "SECOND_DESIGN_ELEMENT_FK";
        String eeClause = "";
        if ( eeIds.size() > 0 ) {
            eeClause += " coexp.EXPRESSION_EXPERIMENT_FK in (";
            eeClause += StringUtils.join( eeIds.iterator(), "," );
            eeClause += ") AND ";
        }

        String p2pClass = getP2PTableNameForClassName( p2pClassName );

        /*
         * This query does not return 'self-links' (gene coexpressed with itself) which happens when two probes for the
         * same gene are correlated. STRAIGHT_JOIN is to ensure that mysql doesn't do something goofy with the index use.
         */
        String query = "SELECT STRAIGHT_JOIN geneout.ID as id, geneout.NAME as genesymb, "
                + "geneout.OFFICIAL_NAME as genename, coexp.EXPRESSION_EXPERIMENT_FK as exper, coexp.PVALUE as pvalue, coexp.SCORE as score, "
                + "gcIn.CS as csIdIn, gcOut.CS as csIdOut, geneout.class as geneType  FROM " + " GENE2CS gcIn "
                + " INNER JOIN " + p2pClass + " coexp ON gcIn.CS=coexp." + inKey + " "
                + " INNER JOIN GENE2CS gcOut ON gcOut.CS=coexp." + outKey + " "
                + " INNER JOIN CHROMOSOME_FEATURE geneout ON geneout.ID=gcOut.GENE" + " where " + eeClause
                + " gcIn.GENE=:id AND geneout.ID <> :id  ";

        return query;
    }

    /**
     * Process a single query result from the coexpression search.
     * 
     * @param geneMap
     * @param scroll
     */
    private void processCoexpQueryResult( GeneMap geneMap, ScrollableResults scroll,
            CoexpressionCollectionValueObject coexpressions ) {
        CoexpressionValueObject vo;
        Long geneId = scroll.getLong( 0 );
        // check to see if geneId is already in the geneMap

        if ( geneMap.contains( geneId ) ) {
            vo = geneMap.get( geneId );
        } else {
            vo = new CoexpressionValueObject();
            vo.setGeneId( geneId );
            vo.setGeneName( scroll.getString( 1 ) );
            vo.setGeneOfficialName( scroll.getString( 2 ) );
            vo.setGeneType( scroll.getString( 8 ) );
            vo.setStringencyFilterValue( coexpressions.getStringency() );
            geneMap.put( vo );
        }

        // add the expression experiment
        Long eeID = scroll.getLong( 3 );
        ExpressionExperimentValueObject eeVo = coexpressions.getExpressionExperiment( vo.getGeneType(), eeID );

        if ( eeVo == null ) {
            eeVo = new ExpressionExperimentValueObject();
            eeVo.setId( eeID );
            coexpressions.addExpressionExperiment( vo.getGeneType(), eeVo );
        }

        vo.addExpressionExperimentValueObject( eeVo );
        coexpressions.addExpressionExperiment( vo.getGeneType(), eeVo );

        Long probeID = scroll.getLong( 7 );
        vo.addScore( eeID, scroll.getDouble( 5 ), probeID );
        vo.addPValue( eeID, scroll.getDouble( 4 ), probeID );

        if ( vo.getGeneType().equalsIgnoreCase( CoexpressionCollectionValueObject.GENE_IMPL ) ) {
            coexpressions.getGeneCoexpressionType().addSpecifityInfo( eeID, probeID, geneId );
            coexpressions.addQuerySpecifityInfo( eeID, scroll.getLong( 6 ) );
        } else if ( vo.getGeneType().equalsIgnoreCase( CoexpressionCollectionValueObject.PREDICTED_GENE_IMPL ) ) {
            coexpressions.getPredictedCoexpressionType().addSpecifityInfo( eeID, probeID, geneId );
        } else if ( vo.getGeneType().equalsIgnoreCase( CoexpressionCollectionValueObject.PROBE_ALIGNED_REGION_IMPL ) ) {
            coexpressions.getProbeAlignedCoexpressionType().addSpecifityInfo( eeID, probeID, geneId );
        }
    }

    /**
     * @param geneMap
     * @param queryObject
     */
    private void processCoexpQuery( GeneMap geneMap, org.hibernate.Query queryObject,
            CoexpressionCollectionValueObject coexpressions ) {
        ScrollableResults scroll = queryObject.scroll( ScrollMode.FORWARD_ONLY );

        while ( scroll.next() ) {
            processCoexpQueryResult( geneMap, scroll, coexpressions );
        }
    }

    /**
     * @param gene
     * @param ees
     * @param id
     * @param eeIds
     * @param queryString
     * @return
     */
    private org.hibernate.Query setCoexpQueryParameters( Session session, Gene gene, long id, String queryString ) {
        org.hibernate.SQLQuery queryObject;
        queryObject = session.createSQLQuery( queryString ); // for native query.

        queryObject.addScalar( "id", new LongType() );
        queryObject.addScalar( "genesymb", new StringType() );
        queryObject.addScalar( "genename", new StringType() );
        queryObject.addScalar( "exper", new LongType() );
        queryObject.addScalar( "pvalue", new DoubleType() );
        queryObject.addScalar( "score", new DoubleType() );
        queryObject.addScalar( "csIdIn", new LongType() );
        queryObject.addScalar( "csIdOut", new LongType() );
        queryObject.addScalar( "geneType", new StringType() );

        queryObject.setLong( "id", id );
        // this is to make the query faster by narrowing down the gene join
        // queryObject.setLong( "taxonId", gene.getTaxon().getId() );

        return queryObject;
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from GeneImpl";
        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( query );

            return ( Integer ) queryObject.iterate().next();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
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
        final String queryString = "select distinct g from GeneImpl as g inner join g.aliases als where als.Alias = :search";
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
     * Gets all the genes that are coexpressed with another gene
     * 
     * @param gene to use as the query
     * @param ees Data sets to restrict the search to.
     * @param stringency minimum number of data sets the coexpression has to occur in before it 'counts'.
     * @return Collection of CoexpressionCollectionValueObjects
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Object handleGetCoexpressedGenes( final Gene gene, Collection ees, Integer stringency ) throws Exception {
        Gene givenG = gene;
        final long id = givenG.getId();
        log.info( "Gene: " + gene.getName() );

        final String p2pClassName = getP2PClassName( givenG );
        final GeneMap geneMap = new GeneMap();
        final CoexpressionCollectionValueObject coexpressions = new CoexpressionCollectionValueObject();

        coexpressions.setStringency( stringency );
        coexpressions.setQueryGene( gene );

        StopWatch overallWatch = new StopWatch();
        overallWatch.start();

        final Collection<Long> eeIds = getEEIds( ees );

        String queryString = "";
        queryString = getFastNativeQueryString( p2pClassName, "firstVector", "secondVector", eeIds );

        Session session = getSessionFactory().openSession();
        org.hibernate.Query queryObject = setCoexpQueryParameters( session, gene, id, queryString );
        processCoexpQuery( geneMap, queryObject, coexpressions );
        session.close();

        overallWatch.stop();
        Long overallElapsed = overallWatch.getTime();
        log.info( "Query took a total of " + overallElapsed + "ms (wall clock time)" );
        coexpressions.setElapsedWallTimeElapsed( overallElapsed );

        StopWatch watch = new StopWatch();
        watch.start();
        log.info( "Starting postprocessing" );
        postProcessing( geneMap, coexpressions );

        watch.stop();
        Long elapsed = watch.getTime();
        coexpressions.setPostProcessTime( elapsed );
        log.info( "Done postprocessing; time for postprocessing: " + elapsed );

        return coexpressions;
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
        final String queryString = "select count(distinct cs) from GeneImpl as gene inner join gene.products gp,  BioSequence2GeneProductImpl"
                + " as bs2gp, CompositeSequenceImpl as cs where gp=bs2gp.geneProduct "
                + " and cs.biologicalCharacteristic=bs2gp.bioSequence " + " and gene.id = :id ";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setLong( "id", id );
            queryObject.setMaxResults( 1 );

            count = ( Long ) queryObject.uniqueResult();

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
        final String queryString = "select distinct cs from GeneImpl as gene  inner join gene.products as gp, BioSequence2GeneProductImpl "
                + " as bs2gp , CompositeSequenceImpl as cs where gp=bs2gp.geneProduct "
                + " and cs.biologicalCharacteristic=bs2gp.bioSequence " + " and gene.id = :id ";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setLong( "id", id );
            compSeq = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return compSeq;
    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetMicroRnaByTaxon( Taxon taxon ) throws Exception {
        Collection<Gene> miRNA = null;
        final String queryString = "select gene from GeneImpl as gene where gene.taxon = :taxon"
                + " and (gene.description like '%micro RNA or sno RNA' OR gene.description = 'miRNA')";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "taxon", taxon );
            miRNA = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return miRNA;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleLoad(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleLoad( Collection ids ) throws Exception {
        Collection<Gene> genes = null;
        final String queryString = "select distinct gene from GeneImpl gene where gene.id in (:ids)";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "ids", ids );
            genes = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return genes;
    }

    // This method has been duplicated from CompositeSequenceService
    // It actually belongs here, as it returns a Map CS to genes.
    // TODO unify this duplication
    @SuppressWarnings("unchecked")
    private Map<Long, Collection<Gene>> getGenes( Collection<Long> csIds ) throws Exception {

        if ( ( csIds == null ) || ( csIds.size() == 0 ) ) {
            log.debug( "getGenes given null or empty set" );
            return null;
        }
        Map<Long, Collection<Gene>> returnVal = new HashMap<Long, Collection<Gene>>();
        for ( Long csID : csIds ) {
            returnVal.put( csID, new HashSet<Gene>() );
        }

        // build the query for fetching the cs -> gene relation
        final String nativeQuery = "select CS, GENE from GENE2CS WHERE CS IN ";

        StringBuilder buf = new StringBuilder();
        buf.append( nativeQuery );
        buf.append( "(" );
        for ( Long csID : csIds ) {
            buf.append( csID );
            buf.append( "," );
        }
        buf.setCharAt( buf.length() - 1, ')' );
        Session session = getSessionFactory().openSession();
        org.hibernate.SQLQuery queryObject = session.createSQLQuery( buf.toString() );
        queryObject.addScalar( "cs", new LongType() );
        queryObject.addScalar( "gene", new LongType() );

        StopWatch watch = new StopWatch();
        log.debug( "Beginning query" );
        watch.start();

        List result = queryObject.list();

        log.debug( "Done with initial query in " + watch.getTime() + " ms, got " + result.size()
                + " cs-to-gene mappings." );
        watch.reset();
        watch.start();

        int count = 0;
        Collection<Long> genesToFetch = new HashSet<Long>();
        Map<Long, Collection<Long>> cs2geneIds = new HashMap<Long, Collection<Long>>();

        for ( Object object : result ) {
            Object[] ar = ( Object[] ) object;
            Long cs = ( Long ) ar[0];
            Long gene = ( Long ) ar[1];
            if ( !cs2geneIds.containsKey( cs ) ) {
                cs2geneIds.put( cs, new HashSet<Long>() );
            }
            cs2geneIds.get( cs ).add( gene );
            genesToFetch.add( gene );
        }

        session.close();

        // nothing found?
        if ( genesToFetch.size() == 0 ) {
            returnVal.clear();
            return returnVal;
        }

        log.debug( "Built cs -> gene map in " + watch.getTime() + " ms; fetching " + genesToFetch.size() + " genes." );
        watch.reset();
        watch.start();

        // fetch the genes
        Collection<Long> batch = new HashSet<Long>();
        Collection<Gene> genes = new HashSet<Gene>();
        String geneQuery = "select g from GeneImpl g where g.id in ( :gs )";

        org.hibernate.Query geneQueryObject = super.getSession( false ).createQuery( geneQuery ).setFetchSize( 1000 );
        int BATCH_SIZE = 10000;
        for ( Long gene : genesToFetch ) {
            batch.add( gene );
            if ( batch.size() == BATCH_SIZE ) {
                geneQueryObject.setParameterList( "gs", batch );
                genes.addAll( geneQueryObject.list() );
                batch.clear();
            }
        }

        if ( batch.size() > 0 ) {
            geneQueryObject.setParameterList( "gs", batch );
            genes.addAll( geneQueryObject.list() );
        }

        log.debug( "Got information on " + genes.size() + " genes in " + watch.getTime() + " ms" );

        Map<Long, Gene> geneIdMap = new HashMap<Long, Gene>();
        for ( Gene g : ( Collection<Gene> ) genes ) {
            Long id = g.getId();
            geneIdMap.put( id, g );
        }

        // fill in the return value.
        for ( Long csId : csIds ) {
            assert csId != null;
            Collection<Long> genesToAttach = cs2geneIds.get( csId );
            if ( genesToAttach == null ) {
                // this means there was no gene for that cs; we should delete it from the result
                returnVal.remove( csId );
                continue;
            }
            for ( Long geneId : genesToAttach ) {
                returnVal.get( csId ).add( geneIdMap.get( geneId ) );
            }
            ++count;
        }

        log.debug( "Done, " + count + " result rows processed, " + returnVal.size() + "/" + csIds.size()
                + " probes are associated with genes" );
        return returnVal;
    }

    private class GeneMap {

        private Map<Long, CoexpressionValueObject> geneImplMap;
        private Map<Long, CoexpressionValueObject> predictedMap;
        private Map<Long, CoexpressionValueObject> probeAlignedMap;

        public GeneMap() {

            super();
            geneImplMap = new HashMap<Long, CoexpressionValueObject>();
            predictedMap = new HashMap<Long, CoexpressionValueObject>();
            probeAlignedMap = new HashMap<Long, CoexpressionValueObject>();

        }

        public boolean contains( Long id ) {

            return geneImplMap.containsKey( id ) || predictedMap.containsKey( id ) || probeAlignedMap.containsKey( id );

        }

        private void addGeneImpl( CoexpressionValueObject cvo ) {

            if ( geneImplMap.containsKey( cvo.getGeneId() ) ) return;

            geneImplMap.put( cvo.getGeneId(), cvo );
        }

        private void addPredictedGene( CoexpressionValueObject cvo ) {

            if ( geneImplMap.containsKey( cvo.getGeneId() ) ) return;

            predictedMap.put( cvo.getGeneId(), cvo );
        }

        private void addProbeAlignedGene( CoexpressionValueObject cvo ) {

            if ( geneImplMap.containsKey( cvo.getGeneId() ) ) return;

            probeAlignedMap.put( cvo.getGeneId(), cvo );
        }

        public CoexpressionValueObject get( Long id ) {

            if ( geneImplMap.containsKey( id ) ) return geneImplMap.get( id );

            if ( predictedMap.containsKey( id ) ) return predictedMap.get( id );

            if ( probeAlignedMap.containsKey( id ) ) return probeAlignedMap.get( id );

            return null;
        }

        public void put( CoexpressionValueObject cvo ) {

            if ( cvo.getGeneType().equalsIgnoreCase( "GeneImpl" ) ) {
                addGeneImpl( cvo );
            } else if ( cvo.getGeneType().equalsIgnoreCase( "PredictedGeneImpl" ) ) {
                addPredictedGene( cvo );
            } else if ( cvo.getGeneType().equalsIgnoreCase( "ProbeAlignedRegionImpl" ) ) {
                addProbeAlignedGene( cvo );
            } else
                log.warn( "There was a coexpressed gene of invalid type. Skipping...  " + cvo.getGeneType() );

            return;

        }

        public Map<Long, CoexpressionValueObject> getGeneImplMap() {
            return geneImplMap;
        }

        public Map<Long, CoexpressionValueObject> getProbeAlignedRegionMap() {
            return probeAlignedMap;
        }

        public Map<Long, CoexpressionValueObject> getPredictedGeneMap() {
            return predictedMap;
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleGetCompositeSequenceMap(java.util.Collection)
     */
    @Override
    protected Map handleGetCompositeSequenceMap( Collection genes ) throws Exception {

        Map<Long, Collection<Long>> geneMap = new HashMap<Long, Collection<Long>>();

        if ( genes == null || genes.size() == 0 ) {
            return null;
        }

        Collection<Long> geneIdList = new ArrayList<Long>();

        for ( Object object : genes ) {
            Gene gene = ( Gene ) object;
            geneIdList.add( gene.getId() );
            // add to gene map
            if ( !geneMap.containsKey( gene.getId() ) ) {
                Collection<Long> csIds = new HashSet<Long>();
                geneMap.put( gene.getId(), csIds );
            }
        }

        String queryString = "SELECT GENE as geneId,CS as csId FROM GENE2CS WHERE " + " GENE in ("
                + StringUtils.join( geneIdList.iterator(), "," ) + ")";

        Session session = getSessionFactory().openSession();
        org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );

        queryObject.addScalar( "geneId", new LongType() );
        queryObject.addScalar( "csId", new LongType() );

        ScrollableResults scroll = queryObject.scroll( ScrollMode.FORWARD_ONLY );

        //

        while ( scroll.next() ) {
            Long geneId = scroll.getLong( 0 );
            Long csId = scroll.getLong( 1 );

            if ( geneMap.containsKey( geneId ) ) {
                Collection<Long> csIds = geneMap.get( geneId );
                csIds.add( csId );
            } else {
                Collection<Long> csIds = new HashSet<Long>();
                csIds.add( csId );
                geneMap.put( geneId, csIds );
            }
        }
        session.close();
        return geneMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleGetCoexpressedGeneMap(int, ubic.gemma.model.genome.Gene)
     */
    @Override
    @Deprecated
    protected Map handleGetCoexpressedGeneMap( int stringincy, Gene gene ) throws Exception {
        throw new UnsupportedOperationException( "Sorry, you shouldn't use this" );
    }

    @Override
    protected Map handleGetCS2GeneMap( Collection css ) throws Exception {
        Map<Long, CompositeSequence> csId2cs = new HashMap<Long, CompositeSequence>();
        for (CompositeSequence cs : (Collection<CompositeSequence>) css) {
            csId2cs.put( cs.getId(), cs );
        }
        Collection<Long> csIds = csId2cs.keySet();
        
        Map<Long, Collection<Long>> csId2geneIds = new HashMap<Long, Collection<Long>>();
        Map<CompositeSequence, Collection<Gene>> cs2genes = new HashMap<CompositeSequence, Collection<Gene>>();
        if ( css == null || css.size() == 0 ) return cs2genes;
        int count = 0;
        int CHUNK_SIZE = 100000;
        Collection<Long> csIdChunk = new HashSet<Long>();
        Session session = getSessionFactory().openSession();

        for ( Long csId : csIds) {
            csIdChunk.add( csId);
            count++;
            if ( count % CHUNK_SIZE == 0 || count == csIds.size() ) {
                String queryString = "SELECT CS as id, GENE as geneId FROM GENE2CS, CHROMOSOME_FEATURE as C WHERE GENE2CS.GENE = C.ID and C.CLASS = 'GeneImpl' and"
                        + " CS in (" + StringUtils.join( csIdChunk.iterator(), "," ) + ")";

                org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
                queryObject.addScalar( "id", new LongType() );
                queryObject.addScalar( "geneId", new LongType() );

                ScrollableResults scroll = queryObject.scroll( ScrollMode.FORWARD_ONLY );
                while ( scroll.next() ) {
                    Long id = scroll.getLong( 0 );
                    Long geneId = scroll.getLong( 1 );
                    Collection<Long> geneIds = csId2geneIds.get( id );
                    if ( geneIds == null ) {
                        geneIds = new HashSet<Long>();
                        csId2geneIds.put( id, geneIds );
                    }
                    geneIds.add( geneId );
                }
                csIdChunk.clear();
            }
        }
        session.close();
        for (Map.Entry<Long, Collection<Long>> ent : csId2geneIds.entrySet()) {
            Collection<Gene> genes = handleLoad(ent.getValue());
            cs2genes.put( csId2cs.get( ent.getKey() ), genes );
        }
        return cs2genes;
    }

}
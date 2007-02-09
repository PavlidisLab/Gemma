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
import org.hibernate.type.DoubleType;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;

import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.coexpression.CoexpressionValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
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

    /**
     * Gets all the CompositeSequences related to the gene identified by the given gene and arrayDesign.
     * 
     * @param gene, arrayDesign
     * @return Collection
     */
    /* (non-Javadoc)
     * @see ubic.gemma.model.genome.GeneDaoBase#handleGetCompositeSequencesById(ubic.gemma.model.genome.Gene, ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetCompositeSequencesById( Gene gene, ArrayDesign arrayDesign ) throws Exception {
        Collection<CompositeSequence> compSeq = null;
        final String queryString = "select distinct compositeSequence from GeneImpl as gene,  BioSequence2GeneProductImpl"
                + " as bs2gp, CompositeSequenceImpl as compositeSequence where gene.products.id=bs2gp.geneProduct.id "
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence " + " and gene = :gene and compositeSequence.arrayDesign = :arrayDesign ";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "arrayDesign", arrayDesign);
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
    private void collectMapInfo( Integer stringency, Map<Long, CoexpressionValueObject> geneMap,
            CoexpressionCollectionValueObject coexpressions ) {
        Collection<ExpressionExperimentValueObject> ees = new HashSet<ExpressionExperimentValueObject>();
        // add count of original matches to coexpression data
        coexpressions.setLinkCount( geneMap.size() );
        // parse out stringency failures
        for ( Long key : geneMap.keySet() ) {
            CoexpressionValueObject v = geneMap.get( key );
            if ( v.getExpressionExperimentValueObjects().size() >= stringency ) {
                // add in coexpressions that match stringency
                coexpressions.getCoexpressionData().add( v );
                // add in expression experiments that match stringency
                ees.addAll( v.getExpressionExperimentValueObjects() );

            }
        }
        // add count of pruned matches to coexpression data
        coexpressions.setStringencyLinkCount( coexpressions.getCoexpressionData().size() );
        // add the distinct set of expression experiments involved
        // in the query to coexpression data
        coexpressions.setExpressionExperiments( ees );
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
     * @param optional collection of ExpressionExperiments on which to limit the search.
     * @param in either "firstVector" or "secondVector"
     * @param out whatever "a" isn't.
     * @return
     */
    @SuppressWarnings("unused")
    private String getQueryString( String p2pClassName, String in, String out ) {
        String queryStringFirstVector =
        // return values
        "select distinct coGene.id, coGene.name, coGene.officialName,p2pc."
                + out
                + ".expressionExperiment.id, p2pc."
                + out
                + ".expressionExperiment.shortName, p2pc."
                + out
                + ".expressionExperiment.name"
                // source tables
                + " from GeneImpl as gene, BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence,"
                // join table
                + p2pClassName
                + " as p2pc,"
                // target tables
                + " GeneImpl as coGene,BioSequence2GeneProductImpl as coBs2gp, CompositeSequenceImpl as coCompositeSequence"
                + " where gene.products.id=bs2gp.geneProduct.id "
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
                + " and compositeSequence.designElementDataVectors.id=p2pc." + out + ".id "
                + " and coCompositeSequence.designElementDataVectors.id=p2pc." + in + ".id "
                + " and coCompositeSequence.biologicalCharacteristic=coBs2gp.bioSequence "
                + " and coGene.products.id=coBs2gp.geneProduct.id "
                + " and gene.id = :id and coGene.taxon.id = :taxonId";

        return queryStringFirstVector;
    }

    /**
     * @param p2pClassName
     * @param in
     * @param out
     * @return
     */
    private String getNativeQueryString( String p2pClassName, String in, String out, Collection<Long> eeIds ) {
        String inKey = in.equals( "firstVector" ) ? "FIRST_VECTOR_FK" : "SECOND_VECTOR_FK";
        String outKey = out.equals( "firstVector" ) ? "FIRST_VECTOR_FK" : "SECOND_VECTOR_FK";
        String eeClause = "";
        if (eeIds.size() > 0) {
            eeClause += " dedvin.EXPRESSION_EXPERIMENT_FK in (";
            eeClause += StringUtils.join( eeIds.iterator(), "," );
            eeClause += ") AND ";
        }

        
        String p2pClass = getP2PTableNameForClassName( p2pClassName );
        String query = "SELECT DISTINCT geneout.ID as id, geneout.NAME as genesymb, "
                + "geneout.OFFICIAL_NAME as genename, dedvout.EXPRESSION_EXPERIMENT_FK as exper, ee.SHORT_NAME as  shortName,inv.NAME as name, outers.PVALUE as pvalue, outers.SCORE as score  FROM DESIGN_ELEMENT_DATA_VECTOR "
                + "dedvout INNER JOIN (SELECT coexp."
                + outKey
                + " AS ID, coexp.PVALUE as PVALUE, coexp.SCORE as SCORE FROM   GENE2CS gc,  DESIGN_ELEMENT_DATA_VECTOR dedvin, "
                + p2pClass
                + " coexp  WHERE gc.GENE=:id and  gc.CS=dedvin.DESIGN_ELEMENT_FK and coexp."
                + inKey
                + "=dedvin.ID)"
                + " AS outers ON dedvout.ID=outers.ID "
                + " INNER JOIN COMPOSITE_SEQUENCE cs2 ON cs2.ID=dedvout.DESIGN_ELEMENT_FK"
                + " INNER JOIN GENE2CS gcout ON gcout.CS=cs2.ID"
                + " INNER JOIN CHROMOSOME_FEATURE geneout ON geneout.ID=gcout.GENE"
                + " INNER JOIN EXPRESSION_EXPERIMENT ee ON ee.ID=dedvout.EXPRESSION_EXPERIMENT_FK"
                + " INNER JOIN INVESTIGATION inv ON ee.ID=inv.ID";

        return query;
    }

    /**
     * Process a single query result from the coexpression search.
     * 
     * @param geneMap
     * @param scroll
     */
    private void processCoexpQueryResult( Map<Long, CoexpressionValueObject> geneMap, ScrollableResults scroll ) {
        CoexpressionValueObject vo;
        Long geneId = scroll.getLong( 0 );
        // check to see if geneId is already in the geneMap
        if ( geneMap.containsKey( geneId ) ) {
            vo = geneMap.get( geneId );
        } else {
            vo = new CoexpressionValueObject();
            vo.setGeneId( geneId );
            vo.setGeneName( scroll.getString( 1 ) );
            vo.setGeneOfficialName( scroll.getString( 2 ) );
            vo.setPValue( scroll.getDouble( 6 ));
            vo.setScore( scroll.getDouble( 7 ));
            geneMap.put( geneId, vo );
        }
        // add the expression experiment
        ExpressionExperimentValueObject eeVo = new ExpressionExperimentValueObject();
        eeVo.setId( scroll.getLong( 3 ).toString() );
        eeVo.setShortName( scroll.getString( 4 ) );
        eeVo.setName( scroll.getString( 5 ) );
        vo.addExpressionExperimentValueObject( eeVo );
    }

    /**
     * @param geneMap
     * @param queryObject
     */
    private void processCoexpQueryResults( Map<Long, CoexpressionValueObject> geneMap, org.hibernate.Query queryObject ) {
        ScrollableResults scroll = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( scroll.next() ) {
            processCoexpQueryResult( geneMap, scroll );
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
    private org.hibernate.Query setCoexpQueryParameters( Gene gene, long id, 
            String queryString ) {
        // org.hibernate.Query queryObject;
        org.hibernate.SQLQuery queryObject;
        // queryObject = super.getSession( false ).createQuery( queryString );
        queryObject = super.getSession( false ).createSQLQuery( queryString ); // for native query.

        queryObject.addScalar( "id", new LongType() );
        queryObject.addScalar( "genesymb", new StringType() );
        queryObject.addScalar( "genename", new StringType() );
        queryObject.addScalar( "exper", new LongType() );
        queryObject.addScalar( "shortName", new StringType() );
        queryObject.addScalar( "name", new StringType() );
        queryObject.addScalar( "pvalue", new DoubleType() );
        queryObject.addScalar( "score", new DoubleType() );
        
        queryObject.setLong( "id", id );
        // this is to make the query faster by narrowing down the gene join
        // queryObject.setLong( "taxonId", gene.getTaxon().getId() );

        return queryObject;
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
        final String queryString = "select distinct compositeSequence.designElementDataVectors from GeneImpl"
                + " as gene,  BioSequence2GeneProductImpl as bs2gp,"
                + " CompositeSequenceImpl as compositeSequence where gene.products.id=bs2gp.geneProduct.id "
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

    @Override
    protected Map handleGetCoexpressedGeneMap( int stringincy, Gene gene ) throws Exception {
        // FIXME refactor to use the methods defined above.
        Gene givenG = gene;
        long id = givenG.getId();

        String p2pClassName = getP2PClassName( givenG );

        Map<Long, Collection<Long>> geneEEsMap = new HashMap<Long, Collection<Long>>();

        String queryStringFirstVector =
        // return values
        "select distinct coGene.id, p2pc.firstVector.expressionExperiment.id"
                // source tables
                + " from GeneImpl as gene, BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence,"
                // join table
                + p2pClassName
                + " as p2pc,"
                // target tables
                + " GeneImpl as coGene,BioSequence2GeneProductImpl as coBs2gp, CompositeSequenceImpl as coCompositeSequence"
                + " where gene.products.id=bs2gp.geneProduct.id "
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
                + " and compositeSequence.designElementDataVectors.id=p2pc.firstVector.id "
                + " and coCompositeSequence.designElementDataVectors.id=p2pc.secondVector.id "
                + " and coCompositeSequence.biologicalCharacteristic=coBs2gp.bioSequence "
                + " and coGene.products.id=coBs2gp.geneProduct.id "
                + " and gene.id = :id and coGene.taxon.id = :taxonId";

        String queryStringSecondVector =
        // return values
        "select distinct coGene.id, p2pc.secondVector.expressionExperiment.id"
                // source tables
                + " from GeneImpl as gene, BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence,"
                // join table
                + p2pClassName
                + " as p2pc,"
                // target tables
                + "GeneImpl as coGene,BioSequence2GeneProductImpl as coBs2gp, CompositeSequenceImpl as coCompositeSequence"
                + " where gene.products.id=bs2gp.geneProduct.id "
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
                + " and compositeSequence.designElementDataVectors.id=p2pc.secondVector.id "
                + " and coCompositeSequence.designElementDataVectors.id=p2pc.firstVector.id "
                + " and coCompositeSequence.biologicalCharacteristic=coBs2gp.bioSequence "
                + " and coGene.products.id=coBs2gp.geneProduct.id "
                + " and gene.id = :id and coGene.taxon.id = :taxonId";

        try {
            // do query joining coexpressed genes through the firstVector to the secondVector
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryStringFirstVector );
            queryObject.setLong( "id", id );
            // this is to make the query faster by narrowing down the gene join
            queryObject.setLong( "taxonId", gene.getTaxon().getId() );

            ScrollableResults scroll = queryObject.scroll( ScrollMode.FORWARD_ONLY );
            while ( scroll.next() ) {
                Collection<Long> eeIds = null;
                Long geneId = scroll.getLong( 0 );
                // check to see if geneId is already in the geneMap
                if ( geneEEsMap.containsKey( geneId ) ) {
                    eeIds = geneEEsMap.get( geneId );
                } else {
                    eeIds = new HashSet<Long>();
                    geneEEsMap.put( geneId, eeIds );
                }
                eeIds.add( scroll.getLong( 1 ) );
            }

            // do query joining coexpressed genes through the secondVector to the firstVector
            queryObject = super.getSession( false ).createQuery( queryStringSecondVector );
            queryObject.setLong( "id", id );
            // this is to make the query faster by narrowing down the gene join
            queryObject.setLong( "taxonId", gene.getTaxon().getId() );
            scroll = queryObject.scroll( ScrollMode.FORWARD_ONLY );
            while ( scroll.next() ) {
                Collection<Long> eeIds = null;
                Long geneId = scroll.getLong( 0 );
                // check to see if geneId is already in the geneMap
                if ( geneEEsMap.containsKey( geneId ) ) {
                    eeIds = geneEEsMap.get( geneId );
                } else {
                    eeIds = new HashSet<Long>();
                    geneEEsMap.put( geneId, eeIds );
                }
                eeIds.add( scroll.getLong( 1 ) );
            }
            Collection<Long> needToRemove = new HashSet<Long>();
            for ( Long geneId : geneEEsMap.keySet() ) {
                Collection<Long> eeIds = geneEEsMap.get( geneId );
                if ( eeIds.size() < stringincy ) needToRemove.add( geneId );
            }
            for ( Long geneId : needToRemove ) {
                geneEEsMap.remove( geneId );
            }
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return geneEEsMap;
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
    protected Object handleGetCoexpressedGenes( Gene gene, Collection ees, Integer stringency ) throws Exception {
        Gene givenG = gene;
        long id = givenG.getId();
        log.info( "Gene: " + gene.getName() );
        
        String p2pClassName = getP2PClassName( givenG );

        Map<Long, CoexpressionValueObject> geneMap = new HashMap<Long, CoexpressionValueObject>();

        CoexpressionCollectionValueObject coexpressions = new CoexpressionCollectionValueObject();

        try {
            Collection<Long> eeIds = getEEIds( ees );

            StopWatch watch = new StopWatch();

            watch.start();
            log.info( "Starting first query" );
            // do query joining coexpressed genes through the firstVector to the secondVector
            // eeIds is an argument because the native SQL query needs to be built with the knowledge
            // of the number of expressionExperimentId arguments.
            String queryString = getNativeQueryString( p2pClassName, "firstVector", "secondVector", eeIds );
            org.hibernate.Query queryObject = setCoexpQueryParameters( gene, id, queryString );
            processCoexpQueryResults( geneMap, queryObject );

            watch.stop();
            Long elapsed = watch.getTime();
            coexpressions.setFirstQueryElapsedTime( elapsed );
            watch.reset();
            log.info( "Elapsed time for first query: "  + elapsed );
            
            watch.start();
            log.info( "Starting second query" );
            queryString = getNativeQueryString( p2pClassName, "secondVector", "firstVector", eeIds );
            queryObject = setCoexpQueryParameters( gene, id, queryString );
            processCoexpQueryResults( geneMap, queryObject );

            watch.stop();
            coexpressions.setSecondQueryElapsedTime( elapsed );

            elapsed = watch.getTime();
            log.info( "Elapsed time for second query: "  + elapsed );            
            watch.reset();
            watch.start();

            log.info( "Starting postprocessing" );
            collectMapInfo( stringency, geneMap, coexpressions );

            watch.stop();
            elapsed = watch.getTime();
            coexpressions.setPostProcessTime( elapsed );
            log.info( "Done postprocessing" );
            log.info( "Elapsed time for postprocessing: "  + elapsed );  
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
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
        final String queryString = "select count(distinct compositeSequence) from GeneImpl as gene,  BioSequence2GeneProductImpl"
                + " as bs2gp, CompositeSequenceImpl as compositeSequence where gene.products.id=bs2gp.geneProduct.id "
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
        final String queryString = "select distinct compositeSequence from GeneImpl as gene,  BioSequence2GeneProductImpl"
                + " as bs2gp, CompositeSequenceImpl as compositeSequence where gene.products.id=bs2gp.geneProduct.id "
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence " + " and gene.id = :id ";

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
}
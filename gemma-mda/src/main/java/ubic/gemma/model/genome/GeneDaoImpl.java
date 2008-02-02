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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.type.DoubleType;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ubic.gemma.model.coexpression.CoexpressedGenesDetails;
import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
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
        return ( Gene ) this.load( geneValueObject.getId() );
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from GeneImpl";
        List r = getHibernateTemplate().find( query );
        return ( Integer ) r.iterator().next();
    }

    /**
     * Gets all the genes referred to by the alias defined by the search string.
     * 
     * @param search
     * @return Collection
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleFindByAlias( String search ) throws Exception {
        final String queryString = "select distinct g from GeneImpl as g inner join g.aliases als where als.alias = :search";
        return getHibernateTemplate().findByNamedParam( queryString, "search", search );
    }

    @Override
    protected Gene handleFindByOfficialSymbol( String symbol, Taxon taxon ) {
        final String queryString = "select distinct g from GeneImpl as g inner join g.taxon t where g.officialSymbol = :symbol and t= :taxon";
        List results = getHibernateTemplate().findByNamedParam( queryString, new String[] { "symbol", "taxon" },
                new Object[] { symbol, taxon } );
        if ( results.size() == 0 ) {
            return null;
        } else if ( results.size() > 1 ) {
            log.warn( "Multiple genes match " + symbol + " in " + taxon + ", return first hit" );
        }
        return ( Gene ) results.iterator().next();
    }

    /**
     * Gets all the genes that are coexpressed with another gene.
     * 
     * @param gene to use as the query
     * @param ees Data sets to restrict the search to.
     * @param stringency minimum number of data sets the coexpression has to occur in before it 'counts'.
     * @param knownGenesOnly
     * @return Collection of CoexpressionCollectionValueObjects. This needs to be 'postprocessed' before it has all the
     *         data needed for web display.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Object handleGetCoexpressedGenes( final Gene gene, Collection ees, Integer stringency,
            boolean knownGenesOnly ) throws Exception {
        Gene givenG = gene;
        final long id = givenG.getId();
        log.debug( "Gene: " + gene.getName() );

        final String p2pClassName = getP2PClassName( givenG );

        final CoexpressionCollectionValueObject coexpressions = new CoexpressionCollectionValueObject( gene, stringency );

        if ( ees.size() == 0 ) {
            log.warn( "No experiments selected" );
            return coexpressions;
        }

        StopWatch overallWatch = new StopWatch();
        overallWatch.start();

        final Collection<Long> eeIds = getEEIds( ees );

        String queryString = "";
        queryString = getFastNativeQueryString( p2pClassName, "firstVector", "secondVector", eeIds, knownGenesOnly );

        Session session = this.getSession( false );
        org.hibernate.Query queryObject = setCoexpQueryParameters( session, gene, id, queryString );

        // This is the actual business of querying the database.
        processCoexpQuery( gene, queryObject, coexpressions );

        if ( coexpressions.getQueryGeneProbes().size() == 0 ) {
            log.debug( "Coexpression query gene " + gene + " has no probes" );
            // should return...
        }

        overallWatch.stop();
        Long overallElapsed = overallWatch.getTime();
        if ( overallElapsed > 1000 )
            log.info( "Query for " + gene.getName() + " took a total of " + overallElapsed + "ms" );
        coexpressions.setDbQuerySeconds( overallElapsed );

        // fill in information about the query gene
        Collection<Long> queryGeneProbeIds = coexpressions.getQueryGeneProbes();
        Map<Long, Collection<Long>> querySpecificity = getCS2GeneMap( queryGeneProbeIds );
        coexpressions.addQueryGeneSpecifityInfo( querySpecificity );
        postProcess( coexpressions );
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
        final String queryString = "select count(distinct cs) from GeneImpl as gene inner join gene.products gp,  BioSequence2GeneProductImpl"
                + " as bs2gp, CompositeSequenceImpl as cs where gp=bs2gp.geneProduct "
                + " and cs.biologicalCharacteristic=bs2gp.bioSequence " + " and gene.id = :id ";
        List r = getHibernateTemplate().findByNamedParam( queryString, "id", id );
        return ( Long ) r.iterator().next();
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

        Session session = this.getSession();
        org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );

        queryObject.addScalar( "geneId", new LongType() );
        queryObject.addScalar( "csId", new LongType() );

        ScrollableResults scroll = queryObject.scroll( ScrollMode.FORWARD_ONLY );

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
        return geneMap;
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

    /**
     * Gets all the CompositeSequences related to the gene identified by the given id.
     * 
     * @param id
     * @return Collection
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetCompositeSequencesById( long id ) throws Exception {
        final String queryString = "select distinct cs from GeneImpl as gene  inner join gene.products as gp, BioSequence2GeneProductImpl "
                + " as bs2gp , CompositeSequenceImpl as cs where gp=bs2gp.geneProduct "
                + " and cs.biologicalCharacteristic=bs2gp.bioSequence " + " and gene.id = :id ";
        return getHibernateTemplate().findByNamedParam( queryString, "id", id );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map handleGetCS2GeneMap( Collection css ) throws Exception {
        // One of these is our return value, depending on whether we need entities or not.
        Map<Long, Collection<Long>> csId2geneIds = new HashMap<Long, Collection<Long>>();
        Map<CompositeSequence, Collection<Gene>> cs2genes = new HashMap<CompositeSequence, Collection<Gene>>();

        if ( css == null || css.size() == 0 ) {
            return cs2genes;
        }

        /*
         * If true, return a map of ids to ids. Otherwise return map of CS to Genes. This is a little dumb, but keeps us
         * from having two very similar methods. Some code uses ids (for simplicity) but really entity objects should be
         * preferred where possible (for type safety and other object-oriented goodness).
         */
        boolean useIds = css.iterator().next() instanceof Long;

        Map<Long, CompositeSequence> csId2cs = new HashMap<Long, CompositeSequence>();
        Collection<Long> csIds = null;
        if ( useIds ) {
            csIds = css;
        } else {
            for ( CompositeSequence cs : ( Collection<CompositeSequence> ) css ) {
                csId2cs.put( cs.getId(), cs );
            }
            csIds = csId2cs.keySet();
        }

        int count = 0;
        int CHUNK_SIZE = 1000;
        Collection<Long> csIdChunk = new HashSet<Long>();
        Session session = this.getSession();

        for ( Long csId : csIds ) {
            csIdChunk.add( csId );
            count++;
            if ( count % CHUNK_SIZE == 0 || count == csIds.size() ) {
                String queryString = "SELECT CS as id, GENE as geneId FROM GENE2CS WHERE CS in ("
                        + StringUtils.join( csIdChunk.iterator(), "," ) + ")";

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
                log.debug( "Processed " + count + " probes" );
            }

        }

        if ( csId2geneIds.keySet().size() != csIds.size() ) {
            log.info( "There were " + ( csIds.size() - csId2geneIds.keySet().size() ) + "/" + csIds.size()
                    + " probes that have no gene mapping." );
        }

        if ( useIds ) return csId2geneIds;

        // get the gene objects so we can return them.
        for ( Map.Entry<Long, Collection<Long>> ent : csId2geneIds.entrySet() ) {
            Collection<Gene> genes = loadMultiple( ent.getValue() );
            cs2genes.put( csId2cs.get( ent.getKey() ), genes );
        }
        return cs2genes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleGetGenesByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetGenesByTaxon( Taxon taxon ) throws Exception {
        final String queryString = "select gene from GeneImpl as gene where gene.taxon = :taxon ";
        return getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleGetMicroRnaByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetMicroRnaByTaxon( Taxon taxon ) throws Exception {
        final String queryString = "select gene from GeneImpl as gene where gene.taxon = :taxon"
                + " and (gene.description like '%micro RNA or sno RNA' OR gene.description = 'miRNA')";
        return getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.GeneDaoBase#handleLoadKnownGenes(ubic.gemma.model.genome.Taxon)
     */
    @Override
    protected Collection handleLoadKnownGenes( Taxon taxon ) throws Exception {
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
     * @see ubic.gemma.model.genome.GeneDaoBase#handleLoad(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleLoadMultiple( Collection ids ) throws Exception {
        if ( ids.size() == 0 ) {
            return new HashSet();
        }
        int BATCH_SIZE = 2000;
        if ( ids.size() > BATCH_SIZE ) {
            log.info( "Loading " + ids.size() + " genes ..." );
        }

        final String queryString = "select gene from GeneImpl gene where gene.id in (:ids)";
        Collection<Long> batch = new HashSet<Long>();
        Collection<Gene> genes = new HashSet<Gene>();

        for ( Long gene : ( Collection<Long> ) ids ) {
            batch.add( gene );
            if ( batch.size() == BATCH_SIZE ) {
                genes.addAll( getHibernateTemplate().findByNamedParam( queryString, "ids", batch ) );
                batch.clear();
            }
        }

        if ( batch.size() > 0 ) {
            genes.addAll( getHibernateTemplate().findByNamedParam( queryString, "ids", batch ) );
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

    @Override
    protected void handleThaw( final Gene gene ) throws Exception {
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                Hibernate.initialize( gene );
                session.lock( gene, LockMode.NONE );
                gene.getProducts().size();
                for ( ubic.gemma.model.genome.gene.GeneProduct gp : gene.getProducts() ) {
                    gp.getAccessions().size();
                    if ( gp.getPhysicalLocation() != null ) gp.getPhysicalLocation().getChromosome().getName();
                }
                gene.getAliases().size();
                gene.getAccessions().size();
                return null;
            }
        } );
    }

    /**
     * @param results
     */
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
    private Collection<Long> getEEIds( Collection<ExpressionExperiment> ees ) {
        Collection<Long> eeIds = new ArrayList<Long>();
        for ( ExpressionExperiment e : ees ) {
            eeIds.add( e.getId() );
        }
        return eeIds;
    }

    /**
     * This query does not return 'self-links' (gene coexpressed with itself) which happens when two probes for the same
     * gene are correlated.
     * <ol >
     * <li>output gene id</li>
     * <li>output gene name</li>
     * <li>output gene official name</li>
     * <li>expression experiment
     * <li>pvalue</li>
     * <li>score</li>
     * <li>query gene probe id</li>
     * <li>output gene probe id</li>
     * <li>output gene type (predicted etc)</li>
     * <li>expression experiment name</li>
     * </ol>
     * 
     * @param p2pClassName
     * @param in
     * @param out
     * @param eeIds this is required.
     * @param knownGenesOnly
     * @return
     */
    private synchronized String getFastNativeQueryString( String p2pClassName, String in, String out,
            Collection<Long> eeIds, boolean knownGenesOnly ) {
        String inKey = in.equals( "firstVector" ) ? "FIRST_DESIGN_ELEMENT_FK" : "SECOND_DESIGN_ELEMENT_FK";
        String outKey = out.equals( "firstVector" ) ? "FIRST_DESIGN_ELEMENT_FK" : "SECOND_DESIGN_ELEMENT_FK";
        String eeClause = "";

        // note that with current index scheme, you have to have EE ids specified.
        if ( eeIds.size() > 0 ) {
            eeClause += " coexp.EXPRESSION_EXPERIMENT_FK in (";
            eeClause += StringUtils.join( eeIds.iterator(), "," );
            eeClause += ") AND ";
        } else {
            log.warn( "This query may run very slowly without EE restriction" );
        }
        // eeClause = " coexp.EXPRESSION_EXPERIMENT_FK = " + eeIds.iterator().next() + " AND ";

        String knownGeneClause = "";
        if ( knownGenesOnly ) {
            knownGeneClause = " gcOut.GTYPE = 'GeneImpl' AND ";
        }

        String p2pClass = getP2PTableNameForClassName( p2pClassName );

        String query = "SELECT gcOut.GENE as id, coexp.EXPRESSION_EXPERIMENT_FK as exper, coexp.PVALUE as pvalue, coexp.SCORE as score, "
                + "gcIn.CS as csIdIn, gcOut.CS as csIdOut, gcOut.GTYPE as geneType FROM GENE2CS gcIn INNER JOIN "
                + p2pClass
                + " coexp ON gcIn.CS=coexp."
                + inKey
                + " "
                + " INNER JOIN GENE2CS gcOut ON gcOut.CS=coexp."
                + outKey
                + " INNER JOIN EXPRESSION_EXPERIMENT ee ON ee.ID=coexp.EXPRESSION_EXPERIMENT_FK "
                + " WHERE "
                + eeClause + knownGeneClause + " gcIn.GENE=:id AND gcOut.GENE <> :id  ";
        // log.info( query );
        return query;
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
     * @param coexpressions
     */
    @SuppressWarnings("unchecked")
    private void postProcess( CoexpressionCollectionValueObject coexpressions ) {
        StopWatch watch = new StopWatch();
        watch.start();

        postProcessKnownGenes( coexpressions );
        postProcessProbeAlignedRegions( coexpressions );
        postProcessPredictedGenes( coexpressions );

        watch.stop();
        Long elapsed = watch.getTime();
        coexpressions.setPostProcessTime( elapsed );
        if ( elapsed > 1000 ) log.info( "Done postprocessing in " + elapsed + "ms." );
    }

    /**
     * @param coexpressions
     */
    @SuppressWarnings("unchecked")
    private void postProcessKnownGenes( CoexpressionCollectionValueObject coexpressions ) {
        CoexpressedGenesDetails knownGeneCoexpression = coexpressions.getKnownGeneCoexpression();
        knownGeneCoexpression.postProcess( coexpressions.getQueryGeneSpecificExpressionExperiments() );
    }

    /**
     * @param coexpressions
     */
    private void postProcessPredictedGenes( CoexpressionCollectionValueObject coexpressions ) {
        CoexpressedGenesDetails predictedCoexpressionType = coexpressions.getPredictedCoexpressionType();
        predictedCoexpressionType.postProcess( coexpressions.getQueryGeneSpecificExpressionExperiments() );
    }

    /**
     * @param coexpressions
     */
    private void postProcessProbeAlignedRegions( CoexpressionCollectionValueObject coexpressions ) {
        CoexpressedGenesDetails probeAlignedCoexpressionType = coexpressions.getProbeAlignedCoexpressionType();
        probeAlignedCoexpressionType.postProcess( coexpressions.getQueryGeneSpecificExpressionExperiments() );
    }

    /**
     * @param queryGene
     * @param geneMap
     * @param queryObject
     */
    private void processCoexpQuery( Gene queryGene, Query queryObject, CoexpressionCollectionValueObject coexpressions ) {
        ScrollableResults scroll = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( scroll.next() ) {
            processCoexpQueryResult( queryGene, scroll, coexpressions );
        }

    }

    /**
     * Process a single query result from the coexpression search, converting it into a
     * CoexpressionCollectionValueObject
     * 
     * @param queryGene
     * @param geneMap
     * @param resultSet
     */
    private void processCoexpQueryResult( Gene queryGene, ScrollableResults resultSet,
            CoexpressionCollectionValueObject coexpressions ) {

        Long geneId = resultSet.getLong( 0 );
        Long eeID = resultSet.getLong( 1 );
        Double score = resultSet.getDouble( 2 );
        Double pvalue = resultSet.getDouble( 3 );
        Long queryGeneProbe = resultSet.getLong( 4 );
        Long outputProbeId = resultSet.getLong( 5 );
        String geneType = resultSet.getString( 6 );

        CoexpressionValueObject geneCoexpressionVo;
        // add the gene (if nto already seen)
        if ( coexpressions.contains( geneId ) ) {
            geneCoexpressionVo = coexpressions.get( geneId );
        } else {
            geneCoexpressionVo = new CoexpressionValueObject();
            geneCoexpressionVo.setGeneId( geneId );
            geneCoexpressionVo.setGeneType( geneType );
            coexpressions.add( geneCoexpressionVo );
        }

        // add the expression experiment.

        ExpressionExperimentValueObject eeVo = coexpressions.getExpressionExperiment( geneType, eeID );
        if ( eeVo == null ) {
            eeVo = new ExpressionExperimentValueObject();
            eeVo.setId( eeID );
            coexpressions.addExpressionExperiment( geneType, eeVo ); // unorganized.
        }
        // add the ee here so we know it is associated with this specific gene.
        geneCoexpressionVo.addExpressionExperimentValueObject( eeVo );

        geneCoexpressionVo.addScore( eeID, score, pvalue, outputProbeId );

        // specificity data
        coexpressions.initializeSpecificityDataStructure( eeID, queryGeneProbe );
        if ( geneType.equals( CoexpressionCollectionValueObject.GENE_IMPL ) ) {
            coexpressions.getKnownGeneCoexpression().addSpecificityInfo( eeID, outputProbeId, geneId );
        } else if ( geneType.equals( CoexpressionCollectionValueObject.PREDICTED_GENE_IMPL ) ) {
            coexpressions.getPredictedCoexpressionType().addSpecificityInfo( eeID, outputProbeId, geneId );
        } else if ( geneType.equals( CoexpressionCollectionValueObject.PROBE_ALIGNED_REGION_IMPL ) ) {
            coexpressions.getProbeAlignedCoexpressionType().addSpecificityInfo( eeID, outputProbeId, geneId );
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

        queryObject.addScalar( "id", new LongType() ); // gene out.
        queryObject.addScalar( "exper", new LongType() );
        queryObject.addScalar( "pvalue", new DoubleType() );
        queryObject.addScalar( "score", new DoubleType() );
        queryObject.addScalar( "csIdIn", new LongType() );
        queryObject.addScalar( "csIdOut", new LongType() );
        queryObject.addScalar( "geneType", new StringType() );
        queryObject.setLong( "id", id );

        return queryObject;
    }
}
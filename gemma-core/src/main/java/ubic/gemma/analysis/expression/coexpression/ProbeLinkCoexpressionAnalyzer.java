/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.analysis.expression.coexpression;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import ubic.gemma.model.analysis.expression.coexpression.CoexpressedGenesDetails;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionValueObject;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.GeneOntologyService;
import ubic.gemma.ontology.OntologyTerm;

/**
 * Perform probe-to-probe coexpression link analysis ("TMM-style").
 * 
 * @spring.bean id="probeLinkCoexpressionAnalyzer"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="probe2ProbeCoexpressionService" ref="probe2ProbeCoexpressionService"
 * @spring.property name="geneOntologyService" ref="geneOntologyService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @author paul
 * @version $Id$
 */
public class ProbeLinkCoexpressionAnalyzer implements InitializingBean {

    private static final String EES_TESTED_GENES_CACHE_NAME = "EEsTestedGenesCache";
    private static Log log = LogFactory.getLog( ProbeLinkCoexpressionAnalyzer.class.getName() );
    private static final int MAX_GENES_TO_COMPUTE_GOOVERLAP = 100;
    private static final int MAX_GENES_TO_COMPUTE_EESTESTEDIN = 100;

    private static final int EETESTEDGENE_CACHE_SIZE = 500; // number of data sets.

    private GeneService geneService;
    private Probe2ProbeCoexpressionService probe2ProbeCoexpressionService;
    private GeneOntologyService geneOntologyService;
    private ExpressionExperimentService expressionExperimentService;

    private Cache eetestedGeneCache;

    public void afterPropertiesSet() throws Exception {
        try {
            CacheManager manager = CacheManager.getInstance();

            if ( manager.cacheExists( EES_TESTED_GENES_CACHE_NAME ) ) {
                return;
            }

            eetestedGeneCache = new Cache( EES_TESTED_GENES_CACHE_NAME, EETESTEDGENE_CACHE_SIZE, false, true, 60000,
                    60000 );

            manager.addCache( eetestedGeneCache );
            eetestedGeneCache = manager.getCache( EES_TESTED_GENES_CACHE_NAME );

        } catch ( CacheException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * @param gene
     * @param ees Collection of ExpressionExperiments that will be considered.
     * @param stringency A positive non-zero integer. If a value less than or equal to zero is entered, the value 1 will
     *        be silently used.
     * @param knownGenesOnly if false, 'predicted genes' and 'probe aligned regions' will be populated.
     * @param limit The maximum number of results that will be fully populated. Set to 0 to fill all (batch mode)
     * @see ubic.gemma.model.genome.GeneDao.getCoexpressedGenes
     * @see ubic.gemma.model.analysis.expression.coexpression.CoexpressionCollectionValueObject
     * @return Fully initialized CoexpressionCollectionValueObject.
     */
    @SuppressWarnings("unchecked")
    public CoexpressionCollectionValueObject linkAnalysis( Gene gene, Collection<BioAssaySet> ees, int stringency,
            boolean knownGenesOnly, int limit ) {

        if ( stringency <= 0 ) stringency = 1;

        if ( log.isInfoEnabled() )
            log.info( "Link query for " + gene.getName() + " stringency=" + stringency + " knowngenesonly?="
                    + knownGenesOnly );

        /*
         * Identify data sets the query gene is expressed in - this is fast (?) and provides an upper bound for EEs we
         * need to search in the first place.
         */
        Collection<ExpressionExperiment> eesQueryTestedIn = probe2ProbeCoexpressionService
                .getExpressionExperimentsLinkTestedIn( gene, ees, false );

        if ( eesQueryTestedIn.size() == 0 ) {
            CoexpressionCollectionValueObject r = new CoexpressionCollectionValueObject( gene, stringency );
            r.setErrorState( "No experiments have coexpression data for  " + gene.getOfficialSymbol() );
            return r;
        }

        /*
         * Perform the coexpression search, some postprocessing done. If eesQueryTestedIn is empty, this returns real
         * quick.
         */
        CoexpressionCollectionValueObject coexpressions = ( CoexpressionCollectionValueObject ) geneService
                .getCoexpressedGenes( gene, eesQueryTestedIn, stringency, knownGenesOnly );

        /*
         * Finish the postprocessing.
         */
        coexpressions.setEesQueryGeneTestedIn( eesQueryTestedIn );
        if ( coexpressions.getAllGeneCoexpressionData( stringency ).size() == 0 ) {
            return coexpressions;
        }

        // don't fill in the gene info etc if we're in batch mode.
        if ( limit > 0 ) {
            filter( coexpressions, limit ); // remove excess
            fillInEEInfo( coexpressions ); // do first...
            fillInGeneInfo( stringency, coexpressions );
            computeGoStats( coexpressions, stringency );
        }

        computeEesTestedIn( ees, coexpressions, eesQueryTestedIn, stringency, limit );

        log.debug( "Analysis completed" );
        return coexpressions;
    }

    /**
     * @param coexpressions
     * @param limit
     */
    private void filter( CoexpressionCollectionValueObject coexpressions, int limit ) {
        CoexpressedGenesDetails coexps = coexpressions.getKnownGeneCoexpression();
        coexps.filter( limit );

        coexps = coexpressions.getPredictedCoexpressionType();
        coexps.filter( limit );

        coexps = coexpressions.getProbeAlignedCoexpressionType();
        coexps.filter( limit );
    }

    /**
     * @param coexpressions
     */
    private void fillInEEInfo( CoexpressionCollectionValueObject coexpressions ) {
        Collection<Long> eeIds = new HashSet<Long>();
        log.debug( "Filling in EE info" );

        CoexpressedGenesDetails coexps = coexpressions.getKnownGeneCoexpression();
        fillInEEInfo( coexpressions, eeIds, coexps );

        coexps = coexpressions.getPredictedCoexpressionType();
        fillInEEInfo( coexpressions, eeIds, coexps );

        coexps = coexpressions.getProbeAlignedCoexpressionType();
        fillInEEInfo( coexpressions, eeIds, coexps );

    }

    /**
     * @param coexpressions
     * @param eeIds
     * @param coexps
     */
    private void fillInEEInfo( CoexpressionCollectionValueObject coexpressions, Collection<Long> eeIds,
            CoexpressedGenesDetails coexps ) {
        for ( ExpressionExperimentValueObject evo : coexpressions.getExpressionExperiments() ) {
            eeIds.add( evo.getId() );
        }
        Collection<ExpressionExperimentValueObject> ees = expressionExperimentService.loadValueObjects( eeIds );
        Map<Long, ExpressionExperimentValueObject> em = new HashMap<Long, ExpressionExperimentValueObject>();
        for ( ExpressionExperimentValueObject evo : ees ) {
            em.put( evo.getId(), evo );
        }

        for ( ExpressionExperimentValueObject evo : coexps.getExpressionExperiments() ) {
            em.put( evo.getId(), evo );
        }
        for ( ExpressionExperimentValueObject evo : coexpressions.getExpressionExperiments() ) {
            ExpressionExperimentValueObject ee = em.get( evo.getId() );
            evo.setShortName( ee.getShortName() );
            evo.setName( ee.getName() );
        }

        for ( ExpressionExperimentValueObject evo : coexps.getExpressionExperiments() ) {
            ExpressionExperimentValueObject ee = em.get( evo.getId() );
            evo.setShortName( ee.getShortName() );
            evo.setName( ee.getName() );
        }
    }

    /**
     * @param stringency
     * @param unorganizedGeneCoexpression
     * @param coexpressions
     */
    private void fillInGeneInfo( int stringency, CoexpressionCollectionValueObject coexpressions ) {
        log.debug( "Filling in Gene info" );
        CoexpressedGenesDetails coexp = coexpressions.getKnownGeneCoexpression();
        fillInGeneInfo( stringency, coexpressions, coexp );

        coexp = coexpressions.getPredictedCoexpressionType();
        fillInGeneInfo( stringency, coexpressions, coexp );

        coexp = coexpressions.getProbeAlignedCoexpressionType();
        fillInGeneInfo( stringency, coexpressions, coexp );

    }

    /**
     * @param stringency
     * @param coexpressions
     * @param coexp
     */
    private void fillInGeneInfo( int stringency, CoexpressionCollectionValueObject coexpressions,
            CoexpressedGenesDetails coexp ) {
        StopWatch timer = new StopWatch();
        timer.start();
        List<CoexpressionValueObject> coexpressionData = coexp.getCoexpressionData( stringency );
        Collection<Long> geneIds = new HashSet<Long>();
        for ( CoexpressionValueObject cod : coexpressionData ) {
            geneIds.add( cod.getGeneId() );
        }

        Collection<Gene> genes = geneService.loadMultiple( geneIds ); // this can be slow if there are a lot.
        Map<Long, Gene> gm = new HashMap<Long, Gene>();
        for ( Gene g : genes ) {
            gm.put( g.getId(), g );
        }
        for ( CoexpressionValueObject cod : coexpressionData ) {
            Gene g = gm.get( cod.getGeneId() );
            cod.setGeneName( g.getName() );
            cod.setGeneOfficialName( g.getOfficialName() );
            cod.setGeneType( g.getClass().getSimpleName() );
            coexpressions.add( cod );
        }
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( "Filled in gene info: " + timer.getTime() + "ms" );
        }
    }

    /**
     * Fill in gene tested information for genes coexpressed with the query.
     * 
     * @param ees ExpressionExperiments, including all that were used at the start of the query (including those the
     *        query gene is NOT expressed in)
     * @param coexpressions
     * @param eesQueryTestedIn
     * @param stringency
     * @param limit if zero, they are all collected and a batch-mode cache is used. This is MUCH slower if you are
     *        analyzing a single CoexpressionCollectionValueObject, but faster (and more memory-intensive) if many are
     *        going to be looked at (as in the case of a bulk Gene2Gene analysis).
     */
    @SuppressWarnings("unchecked")
    private void computeEesTestedIn( Collection<BioAssaySet> ees, CoexpressionCollectionValueObject coexpressions,
            Collection eesQueryTestedIn, int stringency, int limit ) {

        List<CoexpressionValueObject> coexpressionData = coexpressions.getKnownGeneCoexpressionData( stringency );

        if ( limit == 0 ) {
            // when we expecte to be analyzing many query genes. Note that we pass in the full set of experiments, not
            // just the ones in which the query gene was tested in.
            computeEesTestedInBatch( ees, coexpressionData );
        } else {
            // for when we are looking at just one gene at a time
            computeEesTestedIn( eesQueryTestedIn, coexpressionData );
        }

        /*
         * We can add this analysis to the predicted and pars if we want. I'm leaving it out for now.
         */
    }

    /**
     * For the genes that the query is coexpressed with; this retrieves the information for all the coexpressionData
     * passed in (no limit). This method uses a cache to speed repeated calls, so it is very slow at first and then
     * faster.
     * 
     * @param ees, including ALL experiments that were intially started with, NOT just the ones that the query gene was
     *        tested in.
     * @param coexpressionData information on the genes needed to be examined.
     */
    @SuppressWarnings("unchecked")
    private void computeEesTestedInBatch( Collection<BioAssaySet> ees, List<CoexpressionValueObject> coexpressionData ) {

        Map<Long, CoexpressionValueObject> gmap = new HashMap<Long, CoexpressionValueObject>();
        for ( CoexpressionValueObject o : coexpressionData ) {
            gmap.put( o.getGeneId(), o );
        }

        log.debug( "Computing EEs tested in for " + coexpressionData.size() + " genes coexpressed with query." );

        for ( BioAssaySet ee : ees ) {
            Element element = this.eetestedGeneCache.get( ee.getId() );
            Collection<Long> genes;
            if ( element != null ) {
                if ( log.isDebugEnabled() ) log.debug( "Cache hit" );
                genes = ( Collection<Long> ) element.getValue();
            } else {
                genes = probe2ProbeCoexpressionService.getGenesTestedBy( ee, false );
                eetestedGeneCache.put( new Element( ee.getId(), genes ) );
                log.info( "Cached " + genes.size() + " genes assayed by " + ee );
            }

            for ( Long g : genes ) {
                if ( !gmap.containsKey( g ) ) continue;
                gmap.get( g ).getDatasetsTestedIn().add( ee.getId() );
            }
        }

        // debugging.
        for ( CoexpressionValueObject o : coexpressionData ) {
            assert o.getDatasetsTestedIn().size() > 0;// has to be at least stringency actually.
        }

    }

    /**
     * For the genes that the query is coexpressed with. This is limited to the top MAX_GENES_TO_COMPUTE_EESTESTEDIN.
     * This is not very fast if MAX_GENES_TO_COMPUTE_EESTESTEDIN is large. We use this version for on-line requests.
     * 
     * @param eesQueryTestedIn, limited to the ees that the query gene is tested in.
     * @param coexpressionData
     */
    @SuppressWarnings("unchecked")
    private void computeEesTestedIn( Collection<ExpressionExperiment> eesQueryTestedIn,
            List<CoexpressionValueObject> coexpressionData ) {
        Collection<Long> coexGeneIds = new HashSet<Long>();

        int i = 0;
        Map<Long, CoexpressionValueObject> gmap = new HashMap<Long, CoexpressionValueObject>();

        for ( CoexpressionValueObject o : coexpressionData ) {
            coexGeneIds.add( o.getGeneId() );
            gmap.put( o.getGeneId(), o );
            i++;
            if ( i >= MAX_GENES_TO_COMPUTE_EESTESTEDIN ) break;
        }

        log.debug( "Computing EEs tested in for " + coexGeneIds.size() + " genes." );

        Map<Long, Collection<BioAssaySet>> eesTestedIn = probe2ProbeCoexpressionService
                .getExpressionExperimentsTestedIn( coexGeneIds, eesQueryTestedIn, false );
        for ( Long g : eesTestedIn.keySet() ) {
            CoexpressionValueObject cvo = gmap.get( g );
            assert cvo != null;
            assert eesTestedIn.get( g ).size() <= eesQueryTestedIn.size();

            Collection<Long> ids = new HashSet<Long>();
            for ( BioAssaySet ee : eesTestedIn.get( g ) ) {
                ids.add( ee.getId() );
            }

            cvo.setDatasetsTestedIn( ids );
        }
    }

    /**
     * @param geneOntologyService
     */
    public void setGeneOntologyService( GeneOntologyService geneOntologyService ) {
        this.geneOntologyService = geneOntologyService;
    }

    /**
     * @param geneService
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @param probe2ProbeCoexpressionService
     */
    public void setProbe2ProbeCoexpressionService( Probe2ProbeCoexpressionService probe2ProbeCoexpressionService ) {
        this.probe2ProbeCoexpressionService = probe2ProbeCoexpressionService;
    }

    /**
     * @param queryGene
     * @param numQueryGeneGOTerms
     * @param coexpressionData
     */
    private void computeGoOverlap( Gene queryGene, int numQueryGeneGOTerms,
            List<CoexpressionValueObject> coexpressionData ) {
        Collection<Long> overlapIds = new HashSet<Long>();
        int i = 0;
        for ( CoexpressionValueObject cvo : coexpressionData ) {
            overlapIds.add( cvo.getGeneId() );
            cvo.setNumQueryGeneGOTerms( numQueryGeneGOTerms );
            if ( i++ > MAX_GENES_TO_COMPUTE_GOOVERLAP ) break;
        }

        Map<Long, Collection<OntologyTerm>> overlap = geneOntologyService
                .calculateGoTermOverlap( queryGene, overlapIds );

        for ( CoexpressionValueObject cvo : coexpressionData ) {
            cvo.setGoOverlap( overlap.get( cvo.getGeneId() ) );
        }
    }

    /**
     * @param coexpressions
     */
    private void computeGoStats( CoexpressionCollectionValueObject coexpressions, int stringency ) {

        // don't compute this if we aren't loading GO into memory.
        if ( !geneOntologyService.isGeneOntologyLoaded() ) {
            return;
        }

        log.debug( "Computing GO stats" );

        Gene queryGene = coexpressions.getQueryGene();
        int numQueryGeneGOTerms = geneOntologyService.getGOTerms( queryGene ).size();
        coexpressions.setQueryGeneGoTermCount( numQueryGeneGOTerms );
        if ( numQueryGeneGOTerms == 0 ) return;

        if ( coexpressions.getAllGeneCoexpressionData( stringency ).size() == 0 ) return;

        List<CoexpressionValueObject> knownGeneCoexpressionData = coexpressions
                .getKnownGeneCoexpressionData( stringency );
        computeGoOverlap( queryGene, numQueryGeneGOTerms, knownGeneCoexpressionData );

        // Only known genes have GO terms, so we don't need to look at Predicted and PARs.
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

}

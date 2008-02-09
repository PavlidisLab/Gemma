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
package ubic.gemma.analysis.coexpression;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.coexpression.CoexpressedGenesDetails;
import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.coexpression.CoexpressionValueObject;
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

    /**
     * We won't return more genes than this (per gene type category)
     */
    private static final int MAX_GENES_TO_RETURN = 300;
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
     * @param ees
     * @param stringency A positive non-zero integer. If a value less than or equal to zero is entered, the value 1 will
     *        be silently used.
     * @param knownGenesOnly if false, 'predicted genes' and 'probe aligned regions' will be populated.
     * @param limit The maximum number of results that will be fully populated. Set to 0 to fill all (batch mode)
     * @see ubic.gemma.model.genome.GeneDao.getCoexpressedGenes
     * @see ubic.gemma.model.coexpression.CoexpressionCollectionValueObject
     * @return Fully initialized CoexpressionCollectionValueObject.
     */
    @SuppressWarnings("unchecked")
    public CoexpressionCollectionValueObject linkAnalysis( Gene gene, Collection<ExpressionExperiment> ees,
            int stringency, boolean knownGenesOnly, int limit ) {

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
            fillInEEInfo( coexpressions ); // do first...
            fillInGeneInfo( stringency, coexpressions );
            computeGoStats( coexpressions, stringency );
        }

        computeEesTestedIn( gene, ees, coexpressions, eesQueryTestedIn, stringency, limit );

        log.debug( "Analysis completed" );
        return coexpressions;
    }

    private void fillInEEInfo( CoexpressionCollectionValueObject coexpressions ) {
        Collection<Long> eeIds = new HashSet<Long>();
        log.info( "Filling in EE info" );

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
    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
    private void fillInGeneInfo( int stringency, CoexpressionCollectionValueObject coexpressions ) {
        log.info( "Filling in Gene info" );
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
    @SuppressWarnings("unchecked")
    private void fillInGeneInfo( int stringency, CoexpressionCollectionValueObject coexpressions,
            CoexpressedGenesDetails coexp ) {
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
    }

    /**
     * Fill in gene tested information for genes coexpressed with the query.
     * 
     * @param gene
     * @param ees
     * @param coexpressions
     * @param eesQueryTestedIn
     * @param stringency
     * @param limit if zero, they are all collected and a batch-mode cache is used. This is MUCH slower if you are
     *        analyzing a single CoexpressionCollectionValueObject, but faster (and more memory-intensive) if many are
     *        going to be looked at (as in the case of a bulk Gene2Gene analysis).
     */
    @SuppressWarnings("unchecked")
    private void computeEesTestedIn( Gene gene, Collection<ExpressionExperiment> ees,
            CoexpressionCollectionValueObject coexpressions, Collection eesQueryTestedIn, int stringency, int limit ) {

        List<CoexpressionValueObject> coexpressionData = coexpressions.getKnownGeneCoexpressionData( stringency );

        if ( limit == 0 ) {
            computeEesTestedInBatch( gene, ees, coexpressionData );
        } else {
            computeEesTestedIn( gene, ees, coexpressionData );
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
     * @param gene that is coexpressed with the query gene.
     * @param ees
     * @param coexpressionData
     * @param limit how many to populate. If <= 0, all will be done.
     * @param coexpressionData
     */
    @SuppressWarnings("unchecked")
    private void computeEesTestedInBatch( Gene gene, Collection<ExpressionExperiment> ees,
            List<CoexpressionValueObject> coexpressionData ) {

        Map<Long, CoexpressionValueObject> gmap = new HashMap<Long, CoexpressionValueObject>();
        for ( CoexpressionValueObject o : coexpressionData ) {
            gmap.put( o.getGeneId(), o );
        }

        log.info( "Computing EEs tested in for " + coexpressionData.size() + " genes coexpressed with query." );

        for ( ExpressionExperiment ee : ees ) {
            Element element = this.eetestedGeneCache.get( ee.getId() );
            Collection<Long> genes;
            if ( element != null ) {
                if ( log.isDebugEnabled() ) log.debug( "Cache hit" );
                genes = ( Collection<Long> ) element.getValue();
            } else {
                log.info( "Caching genes assayed by " + ee );
                genes = probe2ProbeCoexpressionService.getGenesTestedBy( ee, false );
                eetestedGeneCache.put( new Element( ee.getId(), genes ) );
            }

            for ( Long g : genes ) {
                if ( !gmap.containsKey( g ) ) continue;
                gmap.get( g ).getDatasetsTestedIn().add( ee );
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
     * @param gene
     * @param ees
     * @param coexpressionData
     */
    @SuppressWarnings("unchecked")
    private void computeEesTestedIn( Gene gene, Collection<ExpressionExperiment> ees,
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

        log.info( "Computing EEs tested in for " + coexGeneIds.size() + " genes." );

        Map<Long, Collection<ExpressionExperiment>> eesTestedIn = probe2ProbeCoexpressionService
                .getExpressionExperimentsLinkTestedIn( gene, coexGeneIds, ees, false );
        for ( Long g : eesTestedIn.keySet() ) {
            CoexpressionValueObject o = gmap.get( g );
            o.setDatasetsTestedIn( eesTestedIn.get( g ) );
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

        log.info( "Computing GO stats" );

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

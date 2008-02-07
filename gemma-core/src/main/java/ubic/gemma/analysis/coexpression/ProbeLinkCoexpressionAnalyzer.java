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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
public class ProbeLinkCoexpressionAnalyzer {

    private static Log log = LogFactory.getLog( ProbeLinkCoexpressionAnalyzer.class.getName() );
    private static final int MAX_GENES_TO_COMPUTE_GOOVERLAP = 100;
    private static final int MAX_GENES_TO_COMPUTE_EESTESTEDIN = 100;

    /**
     * We won't return more genes than this (per gene type category)
     */
    private static final int MAX_GENES_TO_RETURN = 300;

    private GeneService geneService;
    private Probe2ProbeCoexpressionService probe2ProbeCoexpressionService;
    private GeneOntologyService geneOntologyService;
    private ExpressionExperimentService expressionExperimentService;

    /**
     * @param gene
     * @param ees
     * @param stringency A positive non-zero integer. If a value less than or equal to zero is entered, the value 1 will
     *        be silently used.
     * @param knownGenesOnly if false, 'predicted genes' and 'probe aligned regions' will be populated.
     * @see ubic.gemma.model.genome.GeneDao.getCoexpressedGenes
     * @see ubic.gemma.model.coexpression.CoexpressionCollectionValueObject
     * @return Fully initialized CoexpressionCollectionValueObject.
     */
    @SuppressWarnings("unchecked")
    public CoexpressionCollectionValueObject linkAnalysis( Gene gene, Collection<ExpressionExperiment> ees,
            int stringency, boolean knownGenesOnly ) {

        if ( stringency <= 0 ) stringency = 1;

        log.info( "Starting link query for " + gene + " stringency=" + stringency + " knowngenesonly?="
                + knownGenesOnly );

        /*
         * Identify data sets the query gene is expressed in - this is fast (?) and provides an upper bound for EEs we
         * need to search in the first place.
         */
        /*
         * FIXME Commented out until the database is populated with this information.
         */
        // Collection<ExpressionExperiment> eesQueryTestedIn = probe2ProbeCoexpressionService
        // .getExpressionExperimentsLinkTestedIn( gene, ees, false );
        Collection<ExpressionExperiment> eesQueryTestedIn = ees;

        /*
         * Perform the coexpression search, some postprocessing done.
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

        fillInEEInfo( coexpressions ); // do first...

        fillInGeneInfo( stringency, coexpressions );

        computeGoStats( coexpressions, stringency );

        if ( coexpressions.getAllGeneCoexpressionData( stringency ).size() == 0 ) return coexpressions;

        computeEesTestedIn( gene, ees, coexpressions, eesQueryTestedIn, stringency );

        log.info( "Analysis completed" );
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
     */
    @SuppressWarnings("unchecked")
    private void computeEesTestedIn( Gene gene, Collection<ExpressionExperiment> ees,
            CoexpressionCollectionValueObject coexpressions, Collection eesQueryTestedIn, int stringency ) {

        List<CoexpressionValueObject> coexpressionData = coexpressions.getKnownGeneCoexpressionData( stringency );
        computeEesTestedIn( gene, ees, coexpressionData );

        /*
         * We can add this analysis to the predicted and pars if we want. I'm leaving it out for now.
         */
    }

    /**
     * For the genes that the query is coexpressed with. This is limited to the top MAX_GENES_TO_COMPUTE_EESTESTEDIN
     * 
     * @param gene
     * @param ees
     * @param coexpressionData
     */
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

        // Only known genes have GO terms.
        // List<CoexpressionValueObject> predictedGeneCoexpressionData = coexpressions
        // .getPredictedCoexpressionData( stringency );
        // computeGoOverlap( queryGene, numQueryGeneGOTerms, predictedGeneCoexpressionData );
        //
        // List<CoexpressionValueObject> probeAlignedRegionCoexpressiondata = coexpressions
        // .getProbeAlignedCoexpressionData( stringency );
        // computeGoOverlap( queryGene, numQueryGeneGOTerms, probeAlignedRegionCoexpressiondata );

    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

}

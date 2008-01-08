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

import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.coexpression.CoexpressionValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
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
 * @author paul
 * @version $Id$
 */
public class ProbeLinkCoexpressionAnalyzer {

    private static final int MAX_GENES_TO_COMPUTE_GOOVERLAP = 50;
    GeneService geneService;
    Probe2ProbeCoexpressionService probe2ProbeCoexpressionService;
    GeneOntologyService geneOntologyService;

    /**
     * @param gene
     * @param ees
     * @param stringency A positive non-zero integer. If a value less than or equal to zero is entered, the value 1 will
     *        be silently used.
     * @see ubic.gemma.model.genome.GeneDao.getCoexpressedGenes
     * @see ubic.gemma.model.coexpression.CoexpressionCollectionValueObject
     * @return Fully initialized CoexpressionCollectionValueObject.
     */
    public CoexpressionCollectionValueObject linkAnalysis( Gene gene, Collection<ExpressionExperiment> ees,
            int stringency ) {

        if ( stringency <= 0 ) stringency = 1;

        CoexpressionCollectionValueObject coexpressions = ( CoexpressionCollectionValueObject ) geneService
                .getCoexpressedGenes( gene, ees, stringency );

        computeGoStats( coexpressions );

        if ( coexpressions.getAllGeneCoexpressionData().size() == 0 ) return coexpressions;

        computeEesTestedIn( gene, ees, coexpressions );

        return coexpressions;
    }

    /**
     * Fill in gene tested information for genes coexpressed with the query.
     * 
     * @param gene
     * @param ees
     * @param coexpressions
     */
    @SuppressWarnings("unchecked")
    private void computeEesTestedIn( Gene gene, Collection<ExpressionExperiment> ees,
            CoexpressionCollectionValueObject coexpressions ) {

        /*
         * First for the query gene - this is fast and provides an upper bound.
         */
        Collection eesQueryTestedIn = probe2ProbeCoexpressionService.getExpressionExperimentsLinkTestedIn( gene, ees,
                false );

        coexpressions.setEesQueryGeneTestedIn( eesQueryTestedIn );

        /*
         * For all the rest of the genes. This is going to be slower. Note we do a lot of object <--> id transformations
         * here, which is annoying and wasteful.
         */
        Collection<Long> coexGeneIds = new HashSet<Long>();

        Map<Long, CoexpressionValueObject> gmap = new HashMap<Long, CoexpressionValueObject>();
        for ( CoexpressionValueObject o : coexpressions.getAllGeneCoexpressionData() ) {
            coexGeneIds.add( o.getGeneId() );
            gmap.put( o.getGeneId(), o );
        }

        Collection<Gene> coexGenes = geneService.loadMultiple( coexGeneIds ); // this step might be avoidable.
        Map<Gene, Collection<ExpressionExperiment>> eesTestedIn = probe2ProbeCoexpressionService
                .getExpressionExperimentsLinkTestedIn( gene, coexGenes, ees, false );
        for ( Gene g : eesTestedIn.keySet() ) {
            CoexpressionValueObject o = gmap.get( g.getId() );
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
    private void computeGoStats( CoexpressionCollectionValueObject coexpressions ) {

        // don't compute this if we aren't loading GO into memory.
        if ( !geneOntologyService.isGeneOntologyLoaded() ) {
            return;
        }

        Gene queryGene = coexpressions.getQueryGene();
        int numQueryGeneGOTerms = geneOntologyService.getGOTerms( queryGene ).size();
        coexpressions.setQueryGeneGoTermCount( numQueryGeneGOTerms );
        if ( numQueryGeneGOTerms == 0 ) return;

        if ( coexpressions.getAllGeneCoexpressionData().size() == 0 ) return;

        List<CoexpressionValueObject> knownGeneCoexpressionData = coexpressions.getKnownGeneCoexpressionData( 0 );
        computeGoOverlap( queryGene, numQueryGeneGOTerms, knownGeneCoexpressionData );

        List<CoexpressionValueObject> predictedGeneCoexpressionData = coexpressions.getPredictedCoexpressionData( 0 );
        computeGoOverlap( queryGene, numQueryGeneGOTerms, predictedGeneCoexpressionData );

        List<CoexpressionValueObject> probeAlignedRegionCoexpressiondata = coexpressions
                .getProbeAlignedCoexpressionData( 0 );
        computeGoOverlap( queryGene, numQueryGeneGOTerms, probeAlignedRegionCoexpressiondata );

    }

}

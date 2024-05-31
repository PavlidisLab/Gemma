/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

package ubic.gemma.web.services;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.*;

/**
 * Allows access to the differential expression analysis. Given 1) an expression experiment set id 2) a collection of
 * gene ids, 3) a taxon id, and a 4) threshold (enter 1 to include all results) The Expression Experiment Set ID (1) can
 * be found by using the ExpressionExperimentSetIDEndpoint, which will return all the expression experiment set ids for
 * all taxons and their corresponding description. The stringency is the minimum number of times we found a particular
 * relationship. Returns a list consisting of 4 fields: 1) the gene id, 2) the EE ID, 3) probe name, and 4) the q value
 *
 * @author gavin
 */
public class DifferentialExpressionProbeResultEndpoint extends AbstractGemmaEndpoint {

    private static final Log log = LogFactory.getLog( DifferentialExpressionProbeResultEndpoint.class );
    /**
     * The local name of the expected request/response.
     */
    private static final String LOCAL_NAME = "differentialExpressionProbeResult";
    private ExpressionExperimentService expressionExperimentService;
    private GeneService geneService;
    private TaxonService taxonService;
    private DifferentialExpressionResultService differentialExpressionResultService;
    private ExpressionExperimentSetService expressionExperimentSetService;

    public void setDifferentialExpressionResultService(
            DifferentialExpressionResultService differentialExpressionAnalysisResultService ) {
        this.differentialExpressionResultService = differentialExpressionAnalysisResultService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setExpressionExperimentSetService( ExpressionExperimentSetService expressionExperimentSetService ) {
        this.expressionExperimentSetService = expressionExperimentSetService;
    }

    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    /**
     * Reads the given <code>requestElement</code>, and sends a the response back.
     *
     * @param requestElement the contents of the SOAP message as DOM elements
     * @param document       a DOM document to be used for constructing <code>Node</code>s
     * @return the response element
     */
    @Override
    protected Element invokeInternal( Element requestElement, Document document ) {
        StopWatch watch = new StopWatch();
        watch.start();
        setLocalName( LOCAL_NAME );

        // taxon input
        Collection<String> taxonInput = getSingleNodeValue( requestElement, "taxon_id" );
        String taxonId = "";
        for ( String id : taxonInput ) {
            taxonId = id;
        }
        Taxon taxon = taxonService.load( Long.parseLong( taxonId ) );
        if ( taxon == null ) {
            String msg = "No taxon with id, " + taxonId + ", can be found.";
            return buildBadResponse( document, msg );
        }

        // gene ids input
        Collection<String> geneInput = getArrayValues( requestElement, "gene_ids" );
        Collection<Long> geneIDLong = new HashSet<Long>();
        for ( String id : geneInput )
            geneIDLong.add( Long.parseLong( id ) );
        Collection<Gene> rawGeneCol = geneService.loadThawed( geneIDLong );
        if ( rawGeneCol.isEmpty() ) {
            String msg = "None of the gene id's can be found.";
            return buildBadResponse( document, msg );
        }
        Collection<Gene> geneCol = retainGenesInCorrectTaxon( rawGeneCol, taxon );
        if ( geneCol == null || geneCol.isEmpty() ) {
            String msg = "Input genes do not match input taxon.";
            return buildBadResponse( document, msg );
        }

        // expression experiment set id input
        Collection<String> analysisInput = getSingleNodeValue( requestElement, "expression_experiment_set_id" );
        String analysisId = "";
        for ( String id : analysisInput ) {
            analysisId = id;
        }
        ExpressionExperimentSet ees = expressionExperimentSetService.load( Long.parseLong( analysisId ) );
        if ( ees == null ) {
            String msg = "No matching expression experiment set can be found for id, " + analysisId;
            return buildBadResponse( document, msg );
        }
        if ( !( ees.getTaxon().getId() ).equals( taxon.getId() ) ) {
            String msg =
                    "Expression experiment set " + analysisId + " does not match input taxon " + taxon.getCommonName();
            return buildBadResponse( document, msg );
        }
        Collection<ExpressionExperiment> eeCol = getExperiments( ees );
        Collection<BioAssaySet> bioAssaySets = new HashSet<BioAssaySet>();
        bioAssaySets.addAll( eeCol );

        // threshold input
        Collection<String> thresholdInput = getSingleNodeValue( requestElement, "threshold" );
        String threshold = "";
        for ( String id : thresholdInput ) {
            threshold = id;
        }

        log.info( "XML input read: " + geneInput.size() + " gene ids,  & taxon id " + taxonId
                + ", & expression experiment set id " + analysisId + ", and threshold " + threshold );

        Element responseWrapper = document.createElementNS( NAMESPACE_URI, LOCAL_NAME );
        Element responseElement = document.createElementNS( NAMESPACE_URI, LOCAL_NAME + RESPONSE );
        responseWrapper.appendChild( responseElement );

        for ( Gene gene : geneCol ) {
            Map<ExpressionExperimentValueObject, List<DifferentialExpressionValueObject>> results = differentialExpressionResultService
                    .find( gene, EntityUtils.getIds( bioAssaySets ), Double.parseDouble( threshold ), -1 );

            for ( ExpressionExperimentValueObject ee : results.keySet() ) {
                // main call to the DifferentialExpressionAnalysisService to retrieve
                // DifferentialExpressionAnalysisResultSet collection
                Collection<DifferentialExpressionValueObject> parCol = results.get( ee );

                // check that a DifferentialExpressionAnalysisResult is not null
                if ( parCol == null || parCol.isEmpty() ) {
                    log.error( "No probe analysis results can be found for gene: " + gene.getOfficialSymbol()
                            + " & experiment: " + ee );
                    buildXMLResponse( document, responseElement, gene.getId().toString(), ee.getId().toString(), null );
                } else
                    buildXMLResponse( document, responseElement, gene.getId().toString(), ee.getId().toString(),
                            parCol );

            }
        }
        watch.stop();
        Long time = watch.getTime();

        log.info( "XML response for differential expression probe results built in " + time + "ms." );
        return responseWrapper;

    }

    private void buildXMLResponse( Document document, Element responseElement, String gene, String ee,
            Collection<DifferentialExpressionValueObject> parCol ) {

        if ( parCol != null ) {
            for ( DifferentialExpressionValueObject par : parCol ) {
                // gene id output
                Element e1 = document.createElement( "gene_id" );
                e1.appendChild( document.createTextNode( gene ) );
                responseElement.appendChild( e1 );
                // ee id output
                Element e2 = document.createElement( "ee_id" );
                e2.appendChild( document.createTextNode( ee ) );
                responseElement.appendChild( e2 );

                Element e3 = document.createElement( "probe" );
                e3.appendChild( document.createTextNode( par.getProbe() ) );
                responseElement.appendChild( e3 );

                Element e4 = document.createElement( "q_value" );
                e4.appendChild( document.createTextNode( par.getCorrP().toString() ) );
                responseElement.appendChild( e4 );
            }
        } else {
            // gene id output
            Element e1 = document.createElement( "gene_id" );
            e1.appendChild( document.createTextNode( gene ) );
            responseElement.appendChild( e1 );

            // ee id output
            Element e2 = document.createElement( "ee_id" );
            e2.appendChild( document.createTextNode( ee ) );
            responseElement.appendChild( e2 );

            Element e3 = document.createElement( "probe" );
            e3.appendChild( document.createTextNode( "NaN" ) );
            responseElement.appendChild( e3 );

            Element e4 = document.createElement( "q_value" );
            e4.appendChild( document.createTextNode( "NaN" ) );
            responseElement.appendChild( e4 );
        }

    }

    private Collection<ExpressionExperiment> getExperiments( ExpressionExperimentSet expressionExperimentSet ) {
        List<Long> ids = new ArrayList<Long>( expressionExperimentSet.getExperiments().size() );
        for ( BioAssaySet dataset : expressionExperimentSet.getExperiments() ) {
            ids.add( dataset.getId() );
        }
        return expressionExperimentService.load( ids );
    }

    private Collection<Gene> retainGenesInCorrectTaxon( Collection<Gene> rawGeneCol, Taxon taxon ) {
        Collection<Gene> genesToUse = new HashSet<Gene>();
        for ( Gene gene : rawGeneCol ) {
            if ( gene.getTaxon().getId().equals( taxon.getId() ) )
                genesToUse.add( gene );
            else
                log.error( "Gene does not match " + taxon.toString() + ": " + gene.getOfficialSymbol() );
        }
        return genesToUse;
    }

}

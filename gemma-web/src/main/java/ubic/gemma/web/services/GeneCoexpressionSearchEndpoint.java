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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ubic.gemma.analysis.expression.coexpression.CoexpressionMetaValueObject;
import ubic.gemma.analysis.expression.coexpression.CoexpressionValueObjectExt;
import ubic.gemma.analysis.expression.coexpression.GeneCoexpressionService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSetService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;

public class GeneCoexpressionSearchEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog( GeneCoexpressionSearchEndpoint.class );

    private GeneService geneService;

    private GeneCoexpressionService geneCoexpressionService;

    private ExpressionExperimentSetService expressionExperimentSetService;

    /**
     * The local name of the expected request/response.
     */
    public static final String LOCAL_NAME = "geneCoexpressionSearch";

    /**
     * The maximum number of coexpression results to return per input gene; a value of zero will return all possible
     * results (ie. max is infinity). We limit this to avoid results sets from blowing up ridiculously.
     */
    public static final int MAX_RESULTS = 100;

    public void setgeneCoexpressionService( GeneCoexpressionService geneCoexpressionService ) {
        this.geneCoexpressionService = geneCoexpressionService;
    }

    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    public void setExpressionExperimentSetService( ExpressionExperimentSetService service ) {
        expressionExperimentSetService = service;
    }

    /**
     * Reads the given <code>requestElement</code>, and sends a the response back.
     * 
     * @param requestElement the contents of the SOAP message as DOM elements
     * @param document a DOM document to be used for constructing <code>Node</code>s
     * @return the response element
     */
    @Override
    protected Element invokeInternal( Element requestElement, Document document ) {

        try {
            StopWatch watch = new StopWatch();
            watch.start();
            setLocalName( LOCAL_NAME );

            String queryGeneId = getNodeValue( requestElement, "query_gene_id" );
            String pairQueryGeneId = getOptionalNodeValue( requestElement, "pair_query_gene_id" );
            String stringency = getNodeValue( requestElement, "stringency" );

            Collection<Gene> queryGenes = new LinkedList<Gene>();
            Gene queryGene = geneService.findByNCBIId( Integer.parseInt( queryGeneId ) );
            if ( queryGene == null ) {
                String msg = "Query gene with id [" + queryGeneId + "] cannot be found.";
                return buildBadResponse( document, msg );
            }
            queryGenes.add( queryGene );

            if ( pairQueryGeneId != null ) {
                Gene queryGene2 = geneService.findByNCBIId( Integer.parseInt( pairQueryGeneId ) );
                if ( queryGene2 == null ) {
                    String msg = "Query gene with id [" + pairQueryGeneId + "] cannot be found.";
                    return buildBadResponse( document, msg );
                }

                queryGenes.add( queryGene2 );
            }

            Collection<ExpressionExperimentSet> eeSets = expressionExperimentSetService.findByName( "All mouse" ); // .load(
                                                                                                                   // 5662l
                                                                                                                   // );
                                                                                                                   // //
                                                                                                                   // uses
                                                                                                                   // 'All
                                                                                                                   // mouse'
                                                                                                                   // by
                                                                                                                   // default.
            ExpressionExperimentSet eeSet = eeSets.iterator().next();
            Collection<BioAssaySet> experiments = eeSet.getExperiments();
            Collection<Long> inputEeIds = new ArrayList<Long>();
            for ( BioAssaySet e : experiments ) {
                inputEeIds.add( e.getId() );
            }

            CoexpressionMetaValueObject metaVO;
            if ( pairQueryGeneId == null ) {
                metaVO = geneCoexpressionService.coexpressionSearch( inputEeIds, queryGenes,
                        Integer.valueOf( stringency ), MAX_RESULTS, false, false );
            } else {
                metaVO = geneCoexpressionService.coexpressionSearch( inputEeIds, queryGenes,
                        Integer.valueOf( stringency ), MAX_RESULTS, true, false );
            }

            Collection<CoexpressionValueObjectExt> coexpressedGenes = metaVO.getKnownGeneResults();

            if ( coexpressedGenes.isEmpty() ) {
                String msg = "No coexpressed genes found.";
                return buildBadResponse( document, msg );
            }

            Element responseWrapper = document.createElementNS( NAMESPACE_URI, LOCAL_NAME );
            Element responseElement = document.createElementNS( NAMESPACE_URI, LOCAL_NAME + RESPONSE );
            responseWrapper.appendChild( responseElement );

            for ( CoexpressionValueObjectExt cvo : coexpressedGenes ) {
                Element item = document.createElement( "CoexpressionSearchResult" );

                Element foundGeneElement = document.createElement( "found_gene_id" );
                foundGeneElement.appendChild( document.createTextNode( cvo.getFoundGene().getNcbiId() == null ? ""
                        : cvo.getFoundGene().getNcbiId().toString() ) );
                item.appendChild( foundGeneElement );

                Element numExperimentsElement = document.createElement( "num_experiments_tested" );
                numExperimentsElement.appendChild( document.createTextNode( cvo.getNumTestedIn().toString() ) );
                item.appendChild( numExperimentsElement );

                Element numCoexpressedElement = document.createElement( "num_experiments_coexpressed" );
                numCoexpressedElement.appendChild( document.createTextNode( String.valueOf( cvo
                        .getSupportingExperiments().size() ) ) );
                item.appendChild( numCoexpressedElement );

                Element gemmaURL = document.createElement( "gemma_details_url" );
                gemmaURL.appendChild( document
                        .createTextNode( "http://www.chibi.ubc.ca/Gemma/searchCoexpression.html?g=" + queryGene.getId()
                                + "," + cvo.getFoundGene().getId() + "&s=" + stringency
                                + "&t=2&q&a=5662&an=All%20mouse" ) );
                item.appendChild( gemmaURL );

                responseElement.appendChild( item );
            }
            watch.stop();
            Long time = watch.getTime();

            if ( time > 1000 ) {
                log.info( "XML response for " + coexpressedGenes.size() + " results built in " + time + "ms." );
            }
            return responseWrapper;
        } catch ( Exception e ) {
            return buildBadResponse( document, e.getMessage() );
        }

    }

}

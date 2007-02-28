/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.web.controller.expression.designElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ajaxanywhere.AAUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.sequence.BlatResultGeneSummary;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultService;
import ubic.gemma.web.controller.BaseMultiActionController;

/**
 * @author joseph
 * @version $Id $
 * @spring.bean id="compositeSequenceController"
 * @spring.property name="compositeSequenceService" ref="compositeSequenceService"
 * @spring.property name="blatResultService" ref="blatResultService"
 * @spring.property name="methodNameResolver" ref="compositeSequenceActions"
 */
public class CompositeSequenceController extends BaseMultiActionController {
    private CompositeSequenceService compositeSequenceService = null;
    private BlatResultService blatResultService = null;

    /**
     * @param compositeSequenceService the compositeSequenceService to set
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @SuppressWarnings("unchecked")
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {

        String sId = request.getParameter( "id" );
        Collection<CompositeSequence> compositeSequences = new ArrayList<CompositeSequence>();
        // if no IDs are specified, then show an error message
        if ( sId == null ) {
            addMessage( request, "object.notfound", new Object[] { "All composite sequences cannot be listed. " } );
        }

        // if ids are specified, then display only those bioSequences
        else {
            String[] idList = StringUtils.split( sId, ',' );
            Collection ids = new ArrayList<Long>();
            
            for ( int i = 0; i < idList.length; i++ ) {
                Long id = Long.parseLong( idList[i] );
                ids.add( id );
            }
            compositeSequences.addAll( compositeSequenceService.load( ids ) );
        }
        return new ModelAndView( "compositeSequences" ).addObject( "compositeSequences", compositeSequences );

    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );
        Collection<Long> csIds = new ArrayList<Long>();
        csIds.add( id );
        
        Collection<CompositeSequence> compositeSequences = compositeSequenceService.load( csIds);
        if ( compositeSequences.size() == 0 ) {
            addMessage( request, "object.notfound", new Object[] { "composite sequence " + id } );
            return new ModelAndView( "mainMenu.html" );
        }
        
        
        CompositeSequence cs = compositeSequences.iterator().next();
        Collection bs2gps = cs.getBiologicalCharacteristic().getBioSequence2GeneProduct();
        Map<BlatResult, BlatResultGeneSummary> blatResults = new HashMap<BlatResult,BlatResultGeneSummary>();
        for ( Object object : bs2gps ) {
            BioSequence2GeneProduct bs2gp = (BioSequence2GeneProduct) object;
            if (bs2gp instanceof BlatAssociation) {
                BlatAssociation blatAssociation =  (BlatAssociation) bs2gp;
                GeneProduct geneProduct = blatAssociation.getGeneProduct();
                Gene gene = geneProduct.getGene();
                BlatResult blatResult = blatAssociation.getBlatResult();
                if (blatResults.containsKey( blatResult )) {
                    blatResults.get( blatResult ).addGene( geneProduct, gene );
                }
                else {
                    BlatResultGeneSummary summary = new BlatResultGeneSummary();
                    summary.addGene( geneProduct, gene );
                    summary.setBlatResult( blatResult );
                    blatResults.put( blatResult, summary );
                }
            }
        }
        
        ModelAndView mav = new ModelAndView("compositeSequence.detail");
        mav.addObject( "compositeSequence", cs );
        mav.addObject( "blatResults", blatResults );
        return mav;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings({ "unused", "unchecked" })
    public ModelAndView showAbbreviated( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );
        Collection<Long> csIds = new ArrayList<Long>();
        csIds.add( id );
        
        Collection<CompositeSequence> compositeSequences = compositeSequenceService.load( csIds);
        if ( compositeSequences.size() == 0 ) {
            addMessage( request, "object.notfound", new Object[] { "composite sequence " + id } );
            return new ModelAndView( "mainMenu.html" );
        }
        
        
        CompositeSequence cs = compositeSequences.iterator().next();
        Collection bs2gps = cs.getBiologicalCharacteristic().getBioSequence2GeneProduct();
        
        
        Map<BlatResult, BlatResultGeneSummary> blatResults = new HashMap<BlatResult,BlatResultGeneSummary>();
        
        for ( Object object : bs2gps ) {
            BioSequence2GeneProduct bs2gp = (BioSequence2GeneProduct) object;
            if (bs2gp instanceof BlatAssociation) {
                BlatAssociation blatAssociation =  (BlatAssociation) bs2gp;
                GeneProduct geneProduct = blatAssociation.getGeneProduct();
                Gene gene = geneProduct.getGene();
                BlatResult blatResult = blatAssociation.getBlatResult();
                if (blatResults.containsKey( blatResult )) {
                    blatResults.get( blatResult ).addGene( geneProduct, gene );
                }
                else {
                    BlatResultGeneSummary summary = new BlatResultGeneSummary();
                    summary.addGene( geneProduct, gene );
                    summary.setBlatResult( blatResult );
                    blatResults.put( blatResult, summary );
                }
            }
        }
        
        /*
         * Pick up blat results that didn't map to genes.
         */
        Collection<BlatResult> allBlatResultsForCs = blatResultService.findByBioSequence(  cs.getBiologicalCharacteristic());
        for ( BlatResult blatResult : allBlatResultsForCs ) {
            if (!blatResults.containsKey( blatResult )) {
                BlatResultGeneSummary summary = new BlatResultGeneSummary();
                summary.setBlatResult( blatResult );
                // no gene...
                blatResults.put( blatResult, summary );
            }
        }

        if (AAUtils.isAjaxRequest(request)){
            AAUtils.addZonesToRefresh(request, "csTable");
        }
        
        ModelAndView mav = new ModelAndView("compositeSequence.detail.abbreviated");
        mav.addObject( "compositeSequence", cs );
        mav.addObject( "blatResults", blatResults );
        return mav;
    }

    public void setBlatResultService( BlatResultService blatResultService ) {
        this.blatResultService = blatResultService;
    }
    
    
}
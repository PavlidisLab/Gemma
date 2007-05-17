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

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ajaxanywhere.AAUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.sequence.ArrayDesignMapResultService;
import ubic.gemma.analysis.sequence.BlatResultGeneSummary;
import ubic.gemma.analysis.sequence.CompositeSequenceMapValueObject;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultService;
import ubic.gemma.web.controller.BaseMultiActionController;
import ubic.gemma.web.propertyeditor.SequenceTypePropertyEditor;
import ubic.gemma.web.remote.EntityDelegator;

/**
 * @author joseph
 * @version $Id$
 * @spring.bean id="compositeSequenceController"
 * @spring.property name="compositeSequenceService" ref="compositeSequenceService"
 * @spring.property name="blatResultService" ref="blatResultService"
 * @spring.property name="methodNameResolver" ref="compositeSequenceActions"
 * @spring.property name="arrayDesignMapResultService" ref="arrayDesignMapResultService"
 */
public class CompositeSequenceController extends BaseMultiActionController {
    private CompositeSequenceService compositeSequenceService = null;
    private BlatResultService blatResultService = null;
    private ArrayDesignMapResultService arrayDesignMapResultService = null;

    public void setBlatResultService( BlatResultService blatResultService ) {
        this.blatResultService = blatResultService;
    }

    /**
     * @param compositeSequenceService the compositeSequenceService to set
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    /**
     * Exposed for AJAX calls.
     * 
     * @param ids
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<CompositeSequenceMapValueObject> getCsSummaries( Collection<Long> ids ) {
        Collection compositeSequences = compositeSequenceService.load( ids );
        Collection<Object[]> rawSummaries = compositeSequenceService.getRawSummary( compositeSequences, 0 );
        return arrayDesignMapResultService.getSummaryMapValueObjects( rawSummaries );
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unchecked")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );
        CompositeSequence cs = compositeSequenceService.load( id );
        if ( cs == null ) {
            addMessage( request, "object.notfound", new Object[] { "composite sequence " + id } );
            return new ModelAndView( "mainMenu.html" );
        }

        Map<BlatResult, BlatResultGeneSummary> blatResults = getBlatMappingSummary( cs );

        ModelAndView mav = new ModelAndView( "compositeSequence.detail" );
        mav.addObject( "compositeSequence", cs );
        mav.addObject( "blatResults", blatResults );
        return mav;
    }

    /**
     * The difference between this and 'show' is the view, and this method is more ajax-aware. (don't know why this is
     * called 'abbreviated'). Probably these methods could be combined.
     * 
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unchecked")
    public ModelAndView showAbbreviated( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );
        CompositeSequence cs = compositeSequenceService.load( id );
        if ( cs == null ) {
            addMessage( request, "object.notfound", new Object[] { "composite sequence " + id } );
            return new ModelAndView( "mainMenu.html" );
        }

        Map<BlatResult, BlatResultGeneSummary> blatResults = getBlatMappingSummary( cs );

        if ( AAUtils.isAjaxRequest( request ) ) {
            AAUtils.addZonesToRefresh( request, "csTable" );
        }

        ModelAndView mav = new ModelAndView( "compositeSequence.detail.abbreviated" );
        mav.addObject( "compositeSequence", cs );
        mav.addObject( "blatResults", blatResults );
        return mav;
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
     * @param cs
     * @param blatResults
     */
    @SuppressWarnings("unchecked")
    private void addBlatResultsLackingGenes( CompositeSequence cs, Map<BlatResult, BlatResultGeneSummary> blatResults ) {
        /*
         * Pick up blat results that didn't map to genes.
         */
        Collection<BlatResult> allBlatResultsForCs = blatResultService.findByBioSequence( cs
                .getBiologicalCharacteristic() );
        for ( BlatResult blatResult : allBlatResultsForCs ) {
            if ( !blatResults.containsKey( blatResult ) ) {
                BlatResultGeneSummary summary = new BlatResultGeneSummary();
                summary.setBlatResult( blatResult );
                // no gene...
                blatResults.put( blatResult, summary );
            }
        }
    }

    /**
     * Exposed for AJAX calls.
     * 
     * @param csd
     * @return
     */
    public Collection<BlatResultGeneSummary> getBlatMappingSummary( EntityDelegator csd ) {
        CompositeSequence cs = compositeSequenceService.load( csd.getId() );
        return this.getBlatMappingSummary( cs ).values();
    }

    /**
     * @param cs
     * @return
     */
    private Map<BlatResult, BlatResultGeneSummary> getBlatMappingSummary( CompositeSequence cs ) {
        BioSequence bs = cs.getBiologicalCharacteristic();
        Map<BlatResult, BlatResultGeneSummary> blatResults = new HashMap<BlatResult, BlatResultGeneSummary>();
        // if the biosequence does not exist, then return null
        if ( bs == null ) {
            return blatResults;
        }
        // if there is no bs2gp entry, then return null
        if ( bs.getBioSequence2GeneProduct() == null ) {
            return blatResults;
        }
        Collection bs2gps = cs.getBiologicalCharacteristic().getBioSequence2GeneProduct();

        for ( Object object : bs2gps ) {
            BioSequence2GeneProduct bs2gp = ( BioSequence2GeneProduct ) object;
            if ( bs2gp instanceof BlatAssociation ) {
                BlatAssociation blatAssociation = ( BlatAssociation ) bs2gp;
                GeneProduct geneProduct = blatAssociation.getGeneProduct();
                Gene gene = geneProduct.getGene();
                BlatResult blatResult = blatAssociation.getBlatResult();
                if ( blatResults.containsKey( blatResult ) ) {
                    blatResults.get( blatResult ).addGene( geneProduct, gene );
                } else {
                    BlatResultGeneSummary summary = new BlatResultGeneSummary();
                    summary.addGene( geneProduct, gene );
                    summary.setBlatResult( blatResult );
                    blatResults.put( blatResult, summary );
                }
            }
        }

        addBlatResultsLackingGenes( cs, blatResults );
        return blatResults;
    }

    @Override
    protected void initBinder( ServletRequest request, ServletRequestDataBinder binder ) throws Exception {
        super.initBinder( request, binder );
        binder.registerCustomEditor( SequenceType.class, new SequenceTypePropertyEditor() );
    }

    public void setArrayDesignMapResultService( ArrayDesignMapResultService arrayDesignMapResultService ) {
        this.arrayDesignMapResultService = arrayDesignMapResultService;
    }

}
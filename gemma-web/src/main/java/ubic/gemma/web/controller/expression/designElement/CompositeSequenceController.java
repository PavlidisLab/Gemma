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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.sequence.ArrayDesignMapResultService;
import ubic.gemma.analysis.sequence.CompositeSequenceMapValueObject;
import ubic.gemma.analysis.sequence.GeneMappingSummary;
import ubic.gemma.analysis.sequence.ProbeMapUtils;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.search.SearchSettingsImpl;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BioSequenceValueObject;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultService;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultValueObject;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.web.controller.BaseController;
import ubic.gemma.web.propertyeditor.SequenceTypePropertyEditor;
import ubic.gemma.web.remote.EntityDelegator;

/**
 * @author joseph
 * @author paul
 * @version $Id$
 */
@Controller
@RequestMapping("/compositeSequence")
public class CompositeSequenceController extends BaseController {

    @Autowired
    private ArrayDesignMapResultService arrayDesignMapResultService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private BlatResultService blatResultService;
    @Autowired
    private CompositeSequenceService compositeSequenceService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private BioSequenceService bioSequenceService;

    /**
     * Search for probes.
     * 
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/filter")
    public ModelAndView filter( HttpServletRequest request, HttpServletResponse response ) {
        String filter = request.getParameter( "filter" );
        String arid = request.getParameter( "arid" );

        ModelAndView mav = new ModelAndView( "compositeSequences.geneMap" );

        // Validate the filtering search criteria.
        if ( StringUtils.isBlank( filter ) ) {
            mav.getModel().put( "message", "No search critera provided" );
            // return showAll( request, response );
        } else {
            Collection<CompositeSequenceMapValueObject> compositeSequenceSummary = search( filter, arid );

            if ( ( compositeSequenceSummary == null ) || ( compositeSequenceSummary.size() == 0 ) ) {
                mav.getModel().put( "message", "Your search yielded no results" );
                compositeSequenceSummary = new ArrayList<CompositeSequenceMapValueObject>();
            } else {
                mav.getModel().put( "message", compositeSequenceSummary.size() + " probes matched your search." );
            }
            mav.addObject( "arrayDesign", loadArrayDesign( arid ) );
            mav.addObject( "sequenceData", compositeSequenceSummary );
            mav.addObject( "numCompositeSequences", compositeSequenceSummary.size() );
        }

        return mav;
    }

    /**
     * Exposed for AJAX calls (Probe browser) FIXME can probably remove soon since we should use getGeneCsSummaries?
     * Might be another use for this
     * 
     * @param ids
     * @return
     */
    public Collection<CompositeSequenceMapValueObject> getCsSummaries( Collection<Long> ids ) {

        if ( ids == null || ids.size() == 0 ) {
            return new HashSet<>();
        }

        Collection<CompositeSequence> compositeSequences = compositeSequenceService.loadMultiple( ids );
        Collection<Object[]> rawSummaries = compositeSequenceService.getRawSummary( compositeSequences, 0 );
        return arrayDesignMapResultService.getSummaryMapValueObjects( rawSummaries );
    }

    /**
     * Exposed for AJAX calls (Elements tab on gene details page)
     * 
     * @param geneId
     * @return
     */
    public Collection<CompositeSequenceMapValueObject> getGeneCsSummaries( Long geneId ) {

        if ( geneId == null ) {
            throw new IllegalArgumentException( "Gene ID must not be null" );
        }

        Collection<CompositeSequence> compositeSequences = geneService.getCompositeSequencesById( geneId );
        Collection<Object[]> rawSummaries = compositeSequenceService.getRawSummary( compositeSequences, 0 );

        if ( rawSummaries == null || rawSummaries.isEmpty() ) {
            return new HashSet<>();
        }

        return arrayDesignMapResultService.getSummaryMapValueObjects( rawSummaries );
    }

    @Autowired
    private GeneService geneService;

    /**
     * Exposed for AJAX calls.
     * 
     * @param csd
     * @return
     */
    public Collection<GeneMappingSummary> getGeneMappingSummary( EntityDelegator csd ) {
        log.debug( "Started proccessing AJAX call: getGeneMappingSummary" );
        if ( csd == null || csd.getId() == null ) {
            return new HashSet<>();
        }
        CompositeSequence cs = compositeSequenceService.load( csd.getId() );
        compositeSequenceService.thaw( Arrays.asList( new CompositeSequence[] { cs } ) );

        log.debug( "Finished proccessing AJAX call: getGeneMappingSummary" );
        return this.getGeneMappingSummary( cs );
    }

    @InitBinder
    public void initBinder( WebDataBinder binder ) {
        binder.registerCustomEditor( SequenceType.class, new SequenceTypePropertyEditor() );
    }

    /**
     * @param searchString
     * @param arrayDesign
     * @return
     */
    public Collection<CompositeSequenceMapValueObject> search( String searchString, String arrayDesignId ) {

        if ( StringUtils.isBlank( searchString ) ) {
            return new HashSet<CompositeSequenceMapValueObject>();
        }

        /*
         * There have to be a few ways of searching: - by ID, by bioSequence, by Gene name. An array design may or may
         * not be given.
         */
        ArrayDesign arrayDesign = loadArrayDesign( arrayDesignId );

        Map<Class<?>, List<SearchResult>> search = searchService.search( SearchSettingsImpl.compositeSequenceSearch(
                searchString, arrayDesign ) );

        Collection<CompositeSequence> css = new HashSet<CompositeSequence>();
        if ( search.containsKey( CompositeSequence.class ) ) {

            Collection<SearchResult> searchResults = search.get( CompositeSequence.class );

            for ( SearchResult sr : searchResults ) {
                CompositeSequence cs = ( CompositeSequence ) sr.getResultObject();
                if ( arrayDesign == null || cs.getArrayDesign().equals( arrayDesign ) ) {
                    css.add( cs );
                }
            }
        }

        return getSummaries( css );
    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @RequestMapping(value = "/show")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );
        CompositeSequence cs = compositeSequenceService.load( id );
        if ( cs == null ) {
            addMessage( request, "object.notfound", new Object[] { "composite sequence " + id } );
        }

        compositeSequenceService.thaw( Arrays.asList( new CompositeSequence[] { cs } ) );

        ModelAndView mav = new ModelAndView( "compositeSequence.detail" );

        mav.addObject( "compositeSequence", cs );
        return mav;
    }

    /**
     * Note that duplicate hits will be ignored here. See bug 4037.
     * 
     * @param cs
     * @param blatResults
     */
    private void addBlatResultsLackingGenes( CompositeSequence cs, Map<Integer, GeneMappingSummary> blatResults ) {
        /*
         * Pick up blat results that didn't map to genes.
         */
        BioSequence biologicalCharacteristic = bioSequenceService.thaw( cs.getBiologicalCharacteristic() );

        Collection<BlatResultValueObject> allBlatResultsForCs = BlatResultValueObject
                .convert2ValueObjects( blatResultService.thaw( blatResultService
                        .findByBioSequence( biologicalCharacteristic ) ) );
        for ( BlatResultValueObject blatResult : allBlatResultsForCs ) {
            if ( !blatResults.containsKey( ProbeMapUtils.hashBlatResult( blatResult ) ) ) {
                GeneMappingSummary summary = new GeneMappingSummary();
                summary.setBlatResult( blatResult );
                summary.setCompositeSequence( compositeSequenceService.convertToValueObject( cs ) );
                // no gene...
                blatResults.put( ProbeMapUtils.hashBlatResult( blatResult ), summary );
            }
        }
    }

    /**
     * @param cs
     * @return
     */
    private Collection<GeneMappingSummary> getGeneMappingSummary( CompositeSequence cs ) {
        BioSequence biologicalCharacteristic = cs.getBiologicalCharacteristic();

        biologicalCharacteristic = bioSequenceService.thaw( biologicalCharacteristic );

        Map<Integer, GeneMappingSummary> results = new HashMap<>();
        if ( biologicalCharacteristic == null || biologicalCharacteristic.getBioSequence2GeneProduct() == null ) {
            return results.values();
        }

        Collection<BioSequence2GeneProduct> bs2gps = biologicalCharacteristic.getBioSequence2GeneProduct();

        for ( BioSequence2GeneProduct bs2gp : bs2gps ) {
            GeneProductValueObject geneProduct = new GeneProductValueObject( bs2gp.getGeneProduct() );

            GeneValueObject gene = new GeneValueObject( bs2gp.getGeneProduct().getGene() );

            assert gene != null;

            BlatResultValueObject blatResult = null;

            if ( ( bs2gp instanceof BlatAssociation ) ) {
                BlatAssociation blatAssociation = ( BlatAssociation ) bs2gp;
                blatResult = new BlatResultValueObject( blatResultService.thaw( blatAssociation.getBlatResult() ) );
            } else if ( bs2gp instanceof AnnotationAssociation ) {
                /*
                 * Make a dummy blat result
                 */
                blatResult = new BlatResultValueObject();
                blatResult.setQuerySequence( BioSequenceValueObject.fromEntity( biologicalCharacteristic ) );
                blatResult.setId( biologicalCharacteristic.getId() );
            }

            if ( blatResult == null ) {
                continue;
            }

            if ( results.containsKey( ProbeMapUtils.hashBlatResult( blatResult ) ) ) {
                results.get( ProbeMapUtils.hashBlatResult( blatResult ) ).addGene( geneProduct, gene );
            } else {
                GeneMappingSummary summary = new GeneMappingSummary();
                summary.addGene( geneProduct, gene );
                summary.setBlatResult( blatResult );
                summary.setCompositeSequence( compositeSequenceService.convertToValueObject( cs ) );
                results.put( ProbeMapUtils.hashBlatResult( blatResult ), summary );
            }

        }

        addBlatResultsLackingGenes( cs, results );

        if ( results.size() == 0 ) {
            // add a 'dummy' that at least contains the information about the CS. This is a bit of a hack...
            GeneMappingSummary summary = new GeneMappingSummary();
            summary.setCompositeSequence( compositeSequenceService.convertToValueObject( cs ) );
            BlatResultValueObject newInstance = new BlatResultValueObject();
            newInstance.setQuerySequence( BioSequenceValueObject.fromEntity( biologicalCharacteristic ) );
            newInstance.setId( -1L );
            summary.setBlatResult( newInstance );
            results.put( ProbeMapUtils.hashBlatResult( newInstance ), summary );
        }

        return results.values();
    }

    /**
     * @param compositeSequences
     * @return
     */
    private Collection<CompositeSequenceMapValueObject> getSummaries( Collection<CompositeSequence> compositeSequences ) {
        Collection<CompositeSequenceMapValueObject> compositeSequenceSummary = new HashSet<CompositeSequenceMapValueObject>();
        if ( compositeSequences.size() > 0 ) {
            Collection<Object[]> rawSummaries = compositeSequenceService.getRawSummary( compositeSequences, 0 );
            compositeSequenceSummary = arrayDesignMapResultService.getSummaryMapValueObjects( rawSummaries );
        }
        return compositeSequenceSummary;
    }

    /**
     * @param arrayDesignId
     * @return
     */
    private ArrayDesign loadArrayDesign( String arrayDesignId ) {
        ArrayDesign arrayDesign = null;
        if ( arrayDesignId != null ) {
            try {
                arrayDesign = arrayDesignService.load( Long.parseLong( arrayDesignId ) );
            } catch ( NumberFormatException e ) {
                // Fail gracefully, please.
            }
        }
        return arrayDesign;
    }

}
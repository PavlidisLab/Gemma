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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.core.analysis.sequence.ArrayDesignMapResultService;
import ubic.gemma.core.analysis.sequence.CompositeSequenceMapValueObject;
import ubic.gemma.core.analysis.sequence.GeneMappingSummary;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.web.controller.BaseController;
import ubic.gemma.web.propertyeditor.SequenceTypePropertyEditor;
import ubic.gemma.web.remote.EntityDelegator;

import java.util.*;

/**
 * @author joseph
 * @author paul
 */
@Controller
@RequestMapping("/compositeSequence")
public class CompositeSequenceController extends BaseController {

    @Autowired
    private ArrayDesignMapResultService arrayDesignMapResultService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private CompositeSequenceService compositeSequenceService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private GeneService geneService;

    @RequestMapping(value = "/filter", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView filter( @RequestParam("filter") String filter, @RequestParam("arid") String arid ) {
        ModelAndView mav = new ModelAndView( "compositeSequences.geneMap" );

        // Validate the filtering search criteria.
        if ( StringUtils.isBlank( filter ) ) {
            mav.getModel().put( "message", "No search critera provided" );
            // return showAll( request, response );
        } else {
            Collection<CompositeSequenceMapValueObject> compositeSequenceSummary = null;
            try {
                compositeSequenceSummary = search( filter, arid );
            } catch ( SearchException e ) {
                throw new IllegalArgumentException( "Invalid search settings.", e );
            }

            if ( compositeSequenceSummary == null || compositeSequenceSummary.isEmpty() ) {
                mav.getModel().put( "message", "Your search yielded no results" );
                compositeSequenceSummary = new ArrayList<>();
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
     */
    public Collection<CompositeSequenceMapValueObject> getCsSummaries( Collection<Long> ids ) {

        if ( ids == null || ids.size() == 0 ) {
            return new HashSet<>();
        }

        Collection<CompositeSequence> compositeSequences = compositeSequenceService.load( ids );
        Collection<Object[]> rawSummaries = compositeSequenceService.getRawSummary( compositeSequences );
        return arrayDesignMapResultService.getSummaryMapValueObjects( rawSummaries );
    }

    /**
     * Exposed for AJAX calls (Elements tab on gene details page)
     */
    @SuppressWarnings("unused") // Can be used in JS
    public Collection<CompositeSequenceMapValueObject> getGeneCsSummaries( Long geneId ) {

        if ( geneId == null ) {
            throw new IllegalArgumentException( "Gene ID must not be null" );
        }

        Collection<CompositeSequence> compositeSequences = geneService.getCompositeSequencesById( geneId );
        Collection<Object[]> rawSummaries = compositeSequenceService.getRawSummary( compositeSequences );

        if ( rawSummaries == null || rawSummaries.isEmpty() ) {
            return new HashSet<>();
        }

        return arrayDesignMapResultService.getSummaryMapValueObjects( rawSummaries );
    }

    /**
     * Exposed for AJAX calls.
     */
    public Collection<GeneMappingSummary> getGeneMappingSummary( EntityDelegator<CompositeSequence> csd ) {
        log.debug( "Started processing AJAX call: getGeneMappingSummary" );
        if ( csd == null || csd.getId() == null ) {
            return new HashSet<>();
        }
        CompositeSequence cs = compositeSequenceService.loadOrFail( csd.getId() );

        // unnecessary see https://github.com/PavlidisLab/Gemma/issues/176
        //     compositeSequenceService.thaw( Collections.singletonList( cs ) );

        log.debug( "Finished processing AJAX call: getGeneMappingSummary" );
        return compositeSequenceService.getGeneMappingSummary( cs.getBiologicalCharacteristic(),
                compositeSequenceService.loadValueObjectWithGeneMappingSummary( cs ) );
    }

    @InitBinder
    public void initBinder( WebDataBinder binder ) {
        binder.registerCustomEditor( SequenceType.class, new SequenceTypePropertyEditor() );
    }

    public Collection<CompositeSequenceMapValueObject> search( String searchString, String arrayDesignId ) throws SearchException {

        if ( StringUtils.isBlank( searchString ) ) {
            return new HashSet<>();
        }

        /*
         * There have to be a few ways of searching: - by ID, by bioSequence, by Gene name. An array design may or may
         * not be given.
         */
        ArrayDesign arrayDesign = loadArrayDesign( arrayDesignId );

        SearchService.SearchResultMap search = searchService.search( SearchSettings.compositeSequenceSearch( searchString, arrayDesign ) );

        Collection<CompositeSequence> css = new HashSet<>();
        Collection<SearchResult<CompositeSequence>> searchResults = search.getByResultObjectType( CompositeSequence.class );
        for ( SearchResult<CompositeSequence> sr : searchResults ) {
            CompositeSequence cs = sr.getResultObject();
            if ( cs != null && ( arrayDesign == null || cs.getArrayDesign().equals( arrayDesign ) ) ) {
                css.add( cs );
            }
        }

        return getSummaries( css );
    }

    @RequestMapping(value = "/show", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView show( @RequestParam("id") Long id ) {
        ModelAndView mav = new ModelAndView( "compositeSequence.detail" );
        CompositeSequence cs = compositeSequenceService.load( id );
        if ( cs == null ) {
            messageUtil.saveMessage( "object.notfound", new Object[] { "composite sequence " + id }, "??" + "object.notfound" + "??" );
            return mav;
        }
        cs = compositeSequenceService.thaw( cs );
        mav.addObject( "compositeSequence", cs );
        return mav;
    }

    private Collection<CompositeSequenceMapValueObject> getSummaries(
            Collection<CompositeSequence> compositeSequences ) {
        Collection<CompositeSequenceMapValueObject> compositeSequenceSummary = new HashSet<>();
        if ( compositeSequences.size() > 0 ) {
            Collection<Object[]> rawSummaries = compositeSequenceService.getRawSummary( compositeSequences );
            compositeSequenceSummary = arrayDesignMapResultService.getSummaryMapValueObjects( rawSummaries );
        }
        return compositeSequenceSummary;
    }

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
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
package ubic.gemma.web.controller.expression.arrayDesign;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.analysis.report.ArrayDesignReportService;
import ubic.gemma.analysis.sequence.ArrayDesignMapResultService;
import ubic.gemma.analysis.sequence.CompositeSequenceMapValueObject;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.search.SearchService;
import ubic.gemma.util.ToStringUtil;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BackgroundProcessingMultiActionController;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.remote.ListRange;
import ubic.gemma.web.taglib.arrayDesign.ArrayDesignHtmlUtil;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="arrayDesignController" name="arrayDesignController"
 * @springproperty name="validator" ref="arrayDesignValidator"
 * @spring.property name = "arrayDesignService" ref="arrayDesignService"
 * @spring.property name = "compositeSequenceService" ref="compositeSequenceService"
 * @spring.property name = "arrayDesignReportService" ref="arrayDesignReportService"
 * @spring.property name = "arrayDesignMapResultService" ref="arrayDesignMapResultService"
 * @spring.property name="methodNameResolver" ref="arrayDesignActions"
 * @spring.property name="searchService" ref="searchService"
 * @spring.property name="auditTrailService" ref="auditTrailService"
 */
public class ArrayDesignController extends BackgroundProcessingMultiActionController implements InitializingBean {

    /**
     * How long an item in the cache lasts when it is not accessed.
     */
    private static final int ARRAY_INFO_CACHE_TIME_TO_IDLE = 60;

    /**
     * How long after creation before an object is evicted.
     */
    private static final int ARRAY_INFO_CACHE_TIME_TO_DIE = 2000;

    /**
     * How many array designs can stay in memory
     */
    private static final int ARRAY_INFO_CACHE_SIZE = 5;

    /**
     * Instead of showing all the probes for the array, we might only fetch some of them.
     */
    private static final int NUM_PROBES_TO_SHOW = 100;

    private static boolean AJAX = true;

    @SuppressWarnings("unused")
    private static Log log = LogFactory.getLog( ArrayDesignController.class.getName() );

    private SearchService searchService;
    private ArrayDesignService arrayDesignService = null;
    private ArrayDesignReportService arrayDesignReportService = null;
    private ArrayDesignMapResultService arrayDesignMapResultService = null;
    private CompositeSequenceService compositeSequenceService = null;
    private final String messageName = "Array design with name";
    private final String identifierNotFound = "Must provide a valid Array Design identifier";
    
    private AuditTrailService auditTrailService;

    private Cache cache;

    /**
     * @param arrayDesignReportService the arrayDesignReportService to set
     */
    public void setArrayDesignReportService( ArrayDesignReportService arrayDesignReportService ) {
        this.arrayDesignReportService = arrayDesignReportService;
    }

    /**
     * @param arrayDesignService The arrayDesignService to set.
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @return the ontologyService
     */
    public AuditTrailService getAuditTrailService() {
        return auditTrailService;
    }

    /**
     * @param ausitTrailService the auditTrailService to set
     */
    public void setAuditTrailService( AuditTrailService auditTrailService ) {
        this.auditTrailService = auditTrailService;
    }

    /**
     * Show (some of) the probes from an array.
     * 
     * @param request
     * @param response
     * @return
     */
    public ModelAndView showCompositeSequences( HttpServletRequest request, HttpServletResponse response ) {

        String arrayDesignIdStr = request.getParameter( "id" );

        if ( arrayDesignIdStr == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Must provide an Array Design name or Id" );
        }

        ArrayDesign arrayDesign = arrayDesignService.load( Long.parseLong( arrayDesignIdStr ) );
        ModelAndView mav = new ModelAndView( "compositeSequences.geneMap" );

        if ( !AJAX ) {
            Collection<CompositeSequenceMapValueObject> compositeSequenceSummary = getDesignSummaries( arrayDesign );
            if ( compositeSequenceSummary == null || compositeSequenceSummary.size() == 0 ) {
                throw new RuntimeException( "No probes found for " + arrayDesign );
            }
            mav.addObject( "sequenceData", compositeSequenceSummary );
            mav.addObject( "numCompositeSequences", compositeSequenceSummary.size() );
        }

        mav.addObject( "arrayDesign", arrayDesign );

        return mav;
    }

    /**
     * @param arrayDesign
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<CompositeSequenceMapValueObject> getDesignSummaries( ArrayDesign arrayDesign ) {
        Collection rawSummaries = compositeSequenceService.getRawSummary( arrayDesign, NUM_PROBES_TO_SHOW );
        Collection<CompositeSequenceMapValueObject> summaries = arrayDesignMapResultService
                .getSummaryMapValueObjects( rawSummaries );
        return summaries;
    }

    /**
     * @param arrayDesign
     * @param offset how many from start
     * @param how many to return
     * @param sortBy name of field to sort by.
     * @param sortDirection DESC or ASC
     * @return
     */
    @SuppressWarnings("unchecked")
    public ListRange getCsSummaryRange( EntityDelegator ed, int offset, int size, final String sortBy,
            final String sortDirection ) {
        ArrayDesign arrayDesign = arrayDesignService.load( ed.getId() );
        Element element = cache.get( arrayDesign );
        List res;
        if ( element == null ) {
            // Experimental; this returns the entire array design info, which is then cached. This is pretty memory
            // intensive.
            Collection rawSummaries = compositeSequenceService.getRawSummary( arrayDesign, -1 );
            Collection<CompositeSequenceMapValueObject> summaries = arrayDesignMapResultService
                    .getSmallerSummaryMapValueObjects( rawSummaries );
            res = new ArrayList();
            res.addAll( summaries );
            cache.put( new Element( arrayDesign, res ) );
        } else {
            res = ( List ) element.getValue();
        }

        int dir = 1;
        if ( sortDirection != null && sortDirection.equalsIgnoreCase( "DESC" ) ) {
            dir = -1;
        }

        final int desc = dir;

        // we need to lock res as it is potentially sorted in different directions by different users.
        synchronized ( res ) {
            Collections.sort( res, new Comparator<CompositeSequenceMapValueObject>() {
                public int compare( CompositeSequenceMapValueObject o1, CompositeSequenceMapValueObject o2 ) {
                    try {

                        Object property = PropertyUtils.getProperty( o1, sortBy );
                        Object property2 = PropertyUtils.getProperty( o2, sortBy );

                        if ( property == null || property2 == null ) return 0;

                        if ( property instanceof Comparable ) {
                            return desc * ( ( Comparable ) property ).compareTo( ( ( Comparable ) property2 ) );
                        } else if ( property instanceof Collection ) {
                            // This is lame - sort by size. Should sort by members themselves.
                            return desc * ( ( ( Collection ) property ).size() - ( ( Collection ) property2 ).size() );

                        } else if ( property instanceof Map ) {
                            return desc
                                    * ( ( ( Map ) property ).values().size() - ( ( Map ) property2 ).values().size() );
                        }
                    } catch ( Exception e ) {
                        return 0;
                    }
                    return 0;
                }

                // /**
                // * FIXME this isn't used.
                // *
                // * @param it
                // * @param it2
                // * @return
                // */
                // private int compareCollections( Iterator it, Iterator it2 ) {
                // while ( it.hasNext() ) {
                // if ( it2.hasNext() ) {
                // return ( ( Comparable ) it.next() ).compareTo( ( Comparable ) it2.next() );
                // } else {
                // return 1;
                // }
                // }
                // return 0;
                // }
            } );

            // return just the values.
            offset = Math.max( 0, offset );
            offset = Math.min( res.size() - 1, offset );
            int endpoint = Math.min( res.size() - 1, offset + size );
            ListRange result = new ListRange();
            result.setData( res.subList( offset, endpoint ).toArray() );
            result.setTotalSize( res.size() );
            return result;
        }
    }

    /**
     * Exposed for AJAX calls.
     * 
     * @param ed
     * @return
     */
    public Collection<CompositeSequenceMapValueObject> getCsSummaries( EntityDelegator ed ) {
        ArrayDesign arrayDesign = arrayDesignService.load( ed.getId() );
        return this.getDesignSummaries( arrayDesign );
    }

    /**
     * AJAX
     * 
     * @param ed
     * @return the taskid
     */
    public String updateReport( EntityDelegator ed ) {
        GenerateSummary runner = new GenerateSummary( null, arrayDesignReportService, ed.getId() );
        runner.setDoForward( false );
        return ( String ) startJob( null, runner ).getModel().get( "taskId" );
    }

    /**
     * AJAX
     * 
     * @param ed
     * @return the HTML to display.
     */
    public Map<String, String> getReportHtml( EntityDelegator ed ) {
        assert ed.getId() != null;
        ArrayDesignValueObject summary = arrayDesignReportService.getSummaryObject( ed.getId() );
        Map<String, String> result = new HashMap<String, String>();

        result.put( "id", ed.getId().toString() );
        if ( summary == null )
            result.put( "html", "Not available" );
        else
            result.put( "html", ArrayDesignHtmlUtil.getSummaryHtml( summary ) );
        return result;
    }

    /**
     * AJAX
     * 
     * @return the taskid
     */
    public String remove( EntityDelegator ed ) {
        ArrayDesign arrayDesign = arrayDesignService.load( ed.getId() );
        if ( arrayDesign == null ) {
            throw new EntityNotFoundException( ed.getId() + " not found" );
        }
        Collection assays = arrayDesignService.getAllAssociatedBioAssays( ed.getId() );
        if ( assays.size() != 0 ) {
            throw new IllegalArgumentException( "Cannot delete " + arrayDesign
                    + ", it is used by an expression experiment" );
        }
        return ( String ) startJob( null, new RemoveArrayJob( null, arrayDesign, arrayDesignService ) ).getModel().get(
                "taskId" );
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return
     */
    // @SuppressWarnings({ "unused", "unchecked" })
    @SuppressWarnings("unchecked")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
        String name = request.getParameter( "name" );
        String idStr = request.getParameter( "id" );

        if ( ( name == null ) && ( idStr == null ) ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Must provide an Array Design name or Id" );
        }
        ArrayDesign arrayDesign = null;
        if ( idStr != null ) {
            arrayDesign = arrayDesignService.load( Long.parseLong( idStr ) );
            request.setAttribute( "id", idStr );
        } else if ( name != null ) {
            arrayDesign = arrayDesignService.findArrayDesignByName( name );
            request.setAttribute( "name", name );
        }

        if ( arrayDesign == null ) {
            throw new EntityNotFoundException( idStr + " not found" );
        }
        long id = arrayDesign.getId();

        Long numCompositeSequences = new Long( arrayDesignService.getCompositeSequenceCount( arrayDesign ) );
        Collection<ExpressionExperiment> ee = arrayDesignService.getExpressionExperiments( arrayDesign );
        Long numExpressionExperiments = new Long( ee.size() );
        Taxon t = arrayDesignService.getTaxon( id );
        String taxon = "";
        if ( t != null ) {
            taxon = t.getScientificName();
        } else {
            taxon = "(Taxon not known)";
        }
        String colorString = formatTechnologyType( arrayDesign );

        ArrayDesignValueObject summary = arrayDesignReportService.getSummaryObject( id );

        String eeIds = formatExpressionExperimentIds( ee );

        ModelAndView mav = new ModelAndView( "arrayDesign.detail" );
        
        AuditEvent troubleEvent = auditTrailService.getLastTroubleEvent( arrayDesign );
        if ( troubleEvent != null ) {
            mav.addObject( "troubleEvent", troubleEvent );
            mav.addObject( "troubleEventDescription", StringEscapeUtils.escapeHtml( ToStringUtil.toString( troubleEvent ) ) );
        }
        AuditEvent validatedEvent = auditTrailService.getLastValidationEvent( arrayDesign );
        if ( validatedEvent != null ) {
            mav.addObject( "validatedEvent", validatedEvent );
            mav.addObject( "validatedEventDescription", StringEscapeUtils.escapeHtml( ToStringUtil.toString( validatedEvent ) ) );
        }

        Collection<ArrayDesign> subsumees = arrayDesign.getSubsumedArrayDesigns();
        ArrayDesign subsumer = arrayDesign.getSubsumingArrayDesign();

        Collection<ArrayDesign> mergees = arrayDesign.getMergees();
        ArrayDesign merger = arrayDesign.getMergedInto();

        mav.addObject( "subsumer", subsumer );
        mav.addObject( "subsumees", subsumees );
        mav.addObject( "merger", merger );
        mav.addObject( "mergees", mergees );
        mav.addObject( "taxon", taxon );
        mav.addObject( "arrayDesign", arrayDesign );
        mav.addObject( "numCompositeSequences", numCompositeSequences );
        mav.addObject( "numExpressionExperiments", numExpressionExperiments );

        mav.addObject( "expressionExperimentIds", eeIds );
        mav.addObject( "technologyType", colorString );
        mav.addObject( "summary", summary );
        return mav;
    }

    private String formatExpressionExperimentIds( Collection<ExpressionExperiment> ee ) {
        String[] eeIdList = new String[ee.size()];
        int i = 0;
        for ( ExpressionExperiment e : ee ) {
            eeIdList[i] = e.getId().toString();
            i++;
        }
        String eeIds = StringUtils.join( eeIdList, "," );
        return eeIds;
    }

    /**
     * @param arrayDesign
     * @return
     */
    private String formatTechnologyType( ArrayDesign arrayDesign ) {
        String techType = arrayDesign.getTechnologyType().getValue();
        String colorString = "";
        if ( techType.equalsIgnoreCase( "ONECOLOR" ) ) {
            colorString = "one-color";
        } else if ( techType.equalsIgnoreCase( "TWOCOLOR" ) ) {
            colorString = "two-color";
        } else if ( techType.equalsIgnoreCase( "DUALMODE" ) ) {
            colorString = "dual mode";
        } else {
            colorString = "No color";
        }
        return colorString;
    }

    /**
     * Show all array designs, or according to a list of IDs passed in.
     * 
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings("unchecked")
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {

        String sId = request.getParameter( "id" );
        Collection<ArrayDesignValueObject> valueObjects = new ArrayList<ArrayDesignValueObject>();
        ArrayDesignValueObject summary = arrayDesignReportService.getSummaryObject();
        // if no IDs are specified, then load all expressionExperiments and show the summary (if available)
        if ( sId == null ) {
            this.saveMessage( request, "Displaying all Arrays" );
            valueObjects.addAll( arrayDesignService.loadAllValueObjects() );
            arrayDesignReportService.fillInValueObjects( valueObjects );
        }

        // if ids are specified, then display only those arrayDesigns
        else {
            Collection ids = new ArrayList<Long>();

            String[] idList = StringUtils.split( sId, ',' );
            for ( int i = 0; i < idList.length; i++ ) {
                ids.add( new Long( idList[i] ) );
            }
            valueObjects.addAll( arrayDesignService.loadValueObjects( ids ) );
        }

        arrayDesignReportService.fillEventInformation( valueObjects );
        arrayDesignReportService.fillInSubsumptionInfo( valueObjects );

        /*
         * for ( ArrayDesignValueObject ad : arrayDesigns ) { ad.setLastSequenceAnalysis(
         * arrayDesignReportService.getLastSequenceAnalysisEvent( ad.getId() ) ); ad.setLastGeneMapping(
         * arrayDesignReportService.getLastGeneMappingEvent( ad.getId() ) ); ad.setLastSequenceUpdate(
         * arrayDesignReportService.getLastSequenceUpdateEvent( ad.getId() ) ); }
         */

        Long numArrayDesigns = new Long( valueObjects.size() );
        ModelAndView mav = new ModelAndView( "arrayDesigns" );
        mav.addObject( "arrayDesigns", valueObjects );
        mav.addObject( "numArrayDesigns", numArrayDesigns );
        mav.addObject( "summary", summary );

        return mav;
    }

    /**
     * Build summary report for an array design
     * 
     * @param request
     * @param response
     * @return
     */
    public ModelAndView generateSummary( HttpServletRequest request, HttpServletResponse response ) {

        String sId = request.getParameter( "id" );

        // if no IDs are specified, then load all expressionExperiments and show the summary (if available)
        if ( sId == null ) {
            return startJob( request, new GenerateSummary( request, arrayDesignReportService ) );
        } else {
            Long id = Long.parseLong( sId );
            return startJob( request, new GenerateSummary( request, arrayDesignReportService, id ) );
        }
    }

    /**
     * Delete an arrayDesign.
     * 
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView delete( HttpServletRequest request, HttpServletResponse response ) {
        String stringId = request.getParameter( "id" );

        if ( stringId == null ) {
            // should be a validation error.
            throw new EntityNotFoundException( "Must provide an id" );
        }

        Long id = null;
        try {
            id = Long.parseLong( stringId );
        } catch ( NumberFormatException e ) {
            throw new EntityNotFoundException( "Identifier was invalid" );
        }

        ArrayDesign arrayDesign = arrayDesignService.load( id );
        if ( arrayDesign == null ) {
            throw new EntityNotFoundException( "Array design with id=" + id + " not found" );
        }

        // check that no EE depend on the arraydesign we want to delete
        // Do this by checking if there are any bioassays that depend this AD
        Collection assays = arrayDesignService.getAllAssociatedBioAssays( id );
        if ( assays.size() != 0 ) {
            // String eeName = ( ( BioAssay ) assays.iterator().next() )
            // todo tell user what EE depends on this array design
            addMessage( request, "Array  " + arrayDesign.getName()
                    + " can't be deleted. Dataset has a dependency on this Array.", new Object[] { messageName,
                    arrayDesign.getName() } );
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html" ) );
        }

        return startJob( request, new RemoveArrayJob( request, arrayDesign, arrayDesignService ) );

    }

    /**
     * shows a list of BioAssays for an expression experiment subset
     * 
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView showExpressionExperiments( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );
        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( identifierNotFound );
        }

        ArrayDesign arrayDesign = arrayDesignService.load( id );
        if ( arrayDesign == null ) {
            this.addMessage( request, "errors.objectnotfound", new Object[] { "Array Design " } );
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html" ) );
        }

        Collection ees = arrayDesignService.getExpressionExperiments( arrayDesign );
        Collection<Long> eeIds = new ArrayList<Long>();
        for ( Object object : ees ) {
            eeIds.add( ( ( ExpressionExperiment ) object ).getId() );
        }
        String ids = StringUtils.join( eeIds.toArray(), "," );
        return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html?id="
                + ids ) );
    }

    /**
     * Show array designs that match search criteria.
     * 
     * @param request
     * @param response
     * @return
     */
    public ModelAndView filter( HttpServletRequest request, HttpServletResponse response ) {
        String filter = request.getParameter( "filter" );

        // Validate the filtering search criteria.
        if ( StringUtils.isBlank( filter ) ) {
            this.saveMessage( request, "No search critera provided" );
            return showAll( request, response );
        }

        Collection<ArrayDesign> searchResults = searchService.compassArrayDesignSearch( filter );

        if ( ( searchResults == null ) || ( searchResults.size() == 0 ) ) {
            this.saveMessage( request, "Your search yielded no results" );
            return showAll( request, response );
        }

        String list = "";

        if ( searchResults.size() == 1 ) {
            ArrayDesign arrayDesign = searchResults.iterator().next();
            this.saveMessage( request, "Matched one : " + arrayDesign.getName() + "(" + arrayDesign.getShortName()
                    + ")" );
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showArrayDesign.html?id=" + arrayDesign.getId() ) );
        } else {
            for ( ArrayDesign ad : searchResults )
                list += ad.getId() + ",";

            this.saveMessage( request, "Search Criteria: " + filter );
            this.saveMessage( request, searchResults.size() + " Array Designs matched your search." );
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html?id=" + list ) );
        }

    }

    /**
     * @return the searchService
     */
    public SearchService getSearchService() {
        return searchService;
    }

    /**
     * @param searchService the searchService to set
     */
    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    /**
     * Inner class used for deleting array designs
     */
    class RemoveArrayJob extends BackgroundControllerJob<ModelAndView> {

        private ArrayDesignService arrayDesignService;
        private ArrayDesign ad;

        public RemoveArrayJob( HttpServletRequest request, ArrayDesign ad, ArrayDesignService arrayDesignService ) {
            super( getMessageUtil() );
            this.arrayDesignService = arrayDesignService;
            this.ad = ad;
        }

        @SuppressWarnings("unchecked")
        public ModelAndView call() throws Exception {

            init();

            ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication()
                    .getName(), "Deleting Array: " + ad.getShortName() );

            arrayDesignService.remove( ad );
            saveMessage( "Array " + ad.getShortName() + " removed from Database." );
            ad = null;

            ProgressManager.destroyProgressJob( job, true );
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html" ) );

        }
    }

    /**
     * Inner class used for building array desing summary
     */
    class GenerateSummary extends BackgroundControllerJob<ModelAndView> {

        private ArrayDesignReportService arrayDesignReportService;
        private Long id;

        public GenerateSummary( HttpServletRequest request, ArrayDesignReportService arrayDesignReportService ) {
            super( getMessageUtil() );
            this.arrayDesignReportService = arrayDesignReportService;
            id = null;
        }

        public GenerateSummary( HttpServletRequest request, ArrayDesignReportService arrayDesignReportService, Long id ) {
            super( getMessageUtil() );
            this.arrayDesignReportService = arrayDesignReportService;
            this.id = id;
        }

        @SuppressWarnings("unchecked")
        public ModelAndView call() throws Exception {

            init();

            ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication()
                    .getName(), "Generating ArrayDesign Report summary", false );

            if ( id == null ) {
                if ( this.getDoForward() ) saveMessage( "Generated summary for all platforms" );
                job.updateProgress( "Generated summary for all platforms" );
                arrayDesignReportService.generateArrayDesignReport();
            } else {
                if ( this.getDoForward() )  saveMessage( "Generating summary for platform " + id );
                job.updateProgress( "Generating summary for specified platform" );
                ArrayDesignValueObject report = arrayDesignReportService.generateArrayDesignReport( id );
                job.setPayload( report );
            }

            // ProgressManager.destroyProgressJob( job, this.getDoForward() );
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesignStatistics.html" ) );

        }
    }

    /**
     * @param arrayDesignMapResultService the arrayDesignMapResultService to set
     */
    public void setArrayDesignMapResultService( ArrayDesignMapResultService arrayDesignMapResultService ) {
        this.arrayDesignMapResultService = arrayDesignMapResultService;
    }

    /**
     * @param compositeSequenceService the compositeSequenceService to set
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        try {
            CacheManager manager = CacheManager.getInstance();

            cache = new Cache( "ArrayDesignCompositeSequenceCache", ARRAY_INFO_CACHE_SIZE,
                    MemoryStoreEvictionPolicy.LFU, false, null, false, ARRAY_INFO_CACHE_TIME_TO_DIE,
                    ARRAY_INFO_CACHE_TIME_TO_IDLE, false, 500, null );

            manager.addCache( cache );
            cache = manager.getCache( "ArrayDesignCompositeSequenceCache" );

        } catch ( CacheException e ) {
            throw new RuntimeException();
        }

    }
}

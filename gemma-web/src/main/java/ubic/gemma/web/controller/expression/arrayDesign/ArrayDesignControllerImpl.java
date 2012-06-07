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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.analysis.report.ArrayDesignReportService;
import ubic.gemma.analysis.sequence.ArrayDesignMapResultService;
import ubic.gemma.analysis.sequence.CompositeSequenceMapValueObject;
import ubic.gemma.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.analysis.service.ArrayDesignAnnotationServiceImpl;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.expression.arrayDesign.AlternateName;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.security.SecurityServiceImpl;
import ubic.gemma.security.audit.AuditableUtil;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.remote.JsonReaderResponse;
import ubic.gemma.web.remote.ListBatchCommand;
import ubic.gemma.web.taglib.arrayDesign.ArrayDesignHtmlUtil;
import ubic.gemma.web.taglib.displaytag.ArrayDesignValueObjectComparator;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * Note: do not use parameterized collections as parameters for ajax methods in this class! Type information is lost
 * during proxy creation so DWR can't figure out what type of collection the method should take. See bug 2756. Use
 * arrays instead.
 * 
 * @author keshav
 * @version $Id$
 */
@Controller
@RequestMapping("/arrays")
public class ArrayDesignControllerImpl extends AbstractTaskService implements ArrayDesignController {

    /**
     * Inner class used for building array design summary
     */
    class GenerateSummary extends BackgroundJob<TaskCommand> {

        public GenerateSummary( TaskCommand command ) {
            super( command );
        }

        @Override
        public TaskResult processJob() {

            if ( this.command.getEntityId() == null ) {
                log.info( "Generating summary for all platforms" );
                arrayDesignReportService.generateArrayDesignReport();
                return new TaskResult( command, new ModelAndView( new RedirectView(
                        "/Gemma/arrays/showAllArrayDesignStatistics.html" ) ) );
            }
            ArrayDesignValueObject report = arrayDesignReportService.generateArrayDesignReport( this.command
                    .getEntityId() );
            return new TaskResult( command, report );

        }
    }

    /**
     * Inner class used for deleting array designs
     */
    class RemoveArrayJob extends BackgroundJob<TaskCommand> {

        public RemoveArrayJob( TaskCommand command ) {
            super( command );
        }

        @Override
        public TaskResult processJob() {
            ArrayDesign ad = arrayDesignService.load( command.getEntityId() );
            if ( ad == null ) {
                throw new IllegalArgumentException( "Could not load platform with id=" + command.getEntityId() );
            }
            arrayDesignService.remove( ad );
            return new TaskResult( command, new ModelAndView( new RedirectView(
                    "/Gemma/arrays/showAllArrayDesigns.html" ) ).addObject( "message", "Array " + ad.getShortName()
                    + " removed from Database." ) );

        }
    }

    private static boolean AJAX = true;

    /**
     * Instead of showing all the probes for the array, we might only fetch some of them.
     */
    private static final int NUM_PROBES_TO_SHOW = 500;

    @Autowired
    private ArrayDesignMapResultService arrayDesignMapResultService = null;

    @Autowired
    private ArrayDesignReportService arrayDesignReportService = null;

    @Autowired
    private ArrayDesignService arrayDesignService = null;

    @Autowired
    private AuditableUtil auditableUtil;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private CompositeSequenceService compositeSequenceService = null;

    @Autowired
    private SearchService searchService;

    @Autowired
    private TaxonService taxonService;

    /* (non-Javadoc)
     * @see ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignController#addAlternateName(java.lang.Long, java.lang.String)
     */
    @Override
    public String addAlternateName( Long arrayDesignId, String alternateName ) {
        ArrayDesign ad = arrayDesignService.load( arrayDesignId );
        if ( ad == null ) {
            throw new IllegalArgumentException( "No such platform with id=" + arrayDesignId );
        }

        if ( StringUtils.isBlank( alternateName ) ) {
            return formatAlternateNames( ad );
        }

        AlternateName newName = AlternateName.Factory.newInstance( alternateName );

        ad.getAlternateNames().add( newName );

        arrayDesignService.update( ad );
        return formatAlternateNames( ad );
    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignController#browse(ubic.gemma.web.remote.ListBatchCommand, java.util.Collection, boolean, boolean)
     */
    @Override
    public JsonReaderResponse<ArrayDesignValueObject> browse( ListBatchCommand batch, Long[] ids,
            boolean showMerged, boolean showOrphans ) {

        Collection<ArrayDesignValueObject> valueObjects = getArrayDesigns( ids, showMerged, showOrphans );

        if ( !SecurityServiceImpl.isUserAdmin() ) {
            auditableUtil.removeTroubledArrayDesigns( valueObjects );
        }
        int count = valueObjects.size();

        arrayDesignReportService.fillInValueObjects( valueObjects );
        arrayDesignReportService.fillEventInformation( valueObjects );
        arrayDesignReportService.fillInSubsumptionInfo( valueObjects );

        JsonReaderResponse<ArrayDesignValueObject> returnVal = new JsonReaderResponse<ArrayDesignValueObject>(
                new ArrayList<ArrayDesignValueObject>( valueObjects ), count );

        return returnVal;
    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignController#delete(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    @RequestMapping("/deleteArrayDesign.html")
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
            throw new EntityNotFoundException( "Platform with id=" + id + " not found" );
        }

        // check that no EE depend on the arraydesign we want to delete
        // Do this by checking if there are any bioassays that depend this AD
        Collection<BioAssay> assays = arrayDesignService.getAllAssociatedBioAssays( id );
        if ( assays.size() != 0 ) {
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html" ) ).addObject(
                    "message", "Array  " + arrayDesign.getName()
                            + " can't be deleted. Dataset has a dependency on this Array." );
        }

        RemoveArrayJob job = new RemoveArrayJob( new TaskCommand( arrayDesign.getId() ) );
        super.startTask( job );

        return new ModelAndView().addObject( "taskId", job.getTaskId() );

    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignController#downloadAnnotationFile(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    @RequestMapping("/downloadAnnotationFile.html")
    public ModelAndView downloadAnnotationFile( HttpServletRequest request, HttpServletResponse response ) {

        String arrayDesignIdStr = request.getParameter( "id" );
        if ( arrayDesignIdStr == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Must provide a platform name or Id" );
        }

        String fileType = request.getParameter( "fileType" );
        if ( fileType == null )
            fileType = ArrayDesignAnnotationService.STANDARD_FILE_SUFFIX;
        else if ( fileType.equalsIgnoreCase( "noParents" ) )
            fileType = ArrayDesignAnnotationService.NO_PARENTS_FILE_SUFFIX;
        else if ( fileType.equalsIgnoreCase( "bioProcess" ) )
            fileType = ArrayDesignAnnotationService.BIO_PROCESS_FILE_SUFFIX;
        else
            fileType = ArrayDesignAnnotationService.STANDARD_FILE_SUFFIX;

        ArrayDesign arrayDesign = arrayDesignService.load( Long.parseLong( arrayDesignIdStr ) );
        String fileBaseName = ArrayDesignAnnotationServiceImpl.mungeFileName( arrayDesign.getShortName() );
        String fileName = fileBaseName + fileType + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX;

        File f = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + fileName );
        InputStream reader;
        try {
            reader = new BufferedInputStream( new FileInputStream( f ) );
        } catch ( FileNotFoundException fnfe ) {
            log.warn( "Annotation file " + fileName + " can't be found at " + fnfe );
            return null;
        }

        response.setHeader( "Content-disposition", "attachment; filename=" + fileName );
        response.setContentType( "application/octet-stream" );

        try {
            OutputStream outputStream = response.getOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ( ( len = reader.read( buf ) ) > 0 ) {
                outputStream.write( buf, 0, len );
            }
            reader.close();

        } catch ( IOException ioe ) {
            log.warn( "Failure during streaming of annotation file " + fileName + " Error: " + ioe );
        }

        return null;
    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignController#filter(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    @RequestMapping("/filterArrayDesigns.html")
    public ModelAndView filter( HttpServletRequest request, HttpServletResponse response ) {

        StopWatch overallWatch = new StopWatch();
        overallWatch.start();

        String filter = request.getParameter( "filter" );

        // Validate the filtering search criteria.
        if ( StringUtils.isBlank( filter ) ) {
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html" ) ).addObject(
                    "message", "No search criteria provided" );
        }

        Collection<SearchResult> searchResults = searchService.search( SearchSettings.arrayDesignSearch( filter ) )
                .get( ArrayDesign.class );

        if ( ( searchResults == null ) || ( searchResults.size() == 0 ) ) {
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html" ) ).addObject(
                    "message", "No search criteria provided" );

        }

        String list = "";

        if ( searchResults.size() == 1 ) {
            ArrayDesign arrayDesign = arrayDesignService.load( searchResults.iterator().next().getId() );
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showArrayDesign.html?id=" + arrayDesign.getId() ) )
                    .addObject( "message", "Matched one : " + arrayDesign.getName() + "(" + arrayDesign.getShortName()
                            + ")" );
        }

        for ( SearchResult ad : searchResults ) {
            list += ad.getId() + ",";
        }

        overallWatch.stop();
        Long overallElapsed = overallWatch.getTime();
        log.info( "Generating the AD list:  (" + list + ") took: " + overallElapsed / 1000 + "s " );

        return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html?id=" + list ) ).addObject(
                "message", searchResults.size() + " Platforms matched your search." );

    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignController#generateSummary(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    @RequestMapping("/generateArrayDesignSummary.html")
    public ModelAndView generateSummary( HttpServletRequest request, HttpServletResponse response ) {

        String sId = request.getParameter( "id" );

        // if no IDs are specified, then load all expressionExperiments and show the summary (if available)
        GenerateSummary job;
        if ( StringUtils.isBlank( sId ) ) {
            job = new GenerateSummary( new TaskCommand() );
            startTask( job );
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html" ) ).addObject(
                    "taskId", job.getTaskId() );
        }

        try {
            Long id = Long.parseLong( sId );
            job = new GenerateSummary( new TaskCommand( id ) );
            startTask( job );
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html?id=" + sId ) )
                    .addObject( "taskId", job.getTaskId() );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( "Invalid ID: " + sId );
        }

    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignController#getArrayDesigns(java.util.Collection, boolean, boolean)
     */
    @Override
    public Collection<ArrayDesignValueObject> getArrayDesigns( Long[] arrayDesignIds, boolean showMergees,
            boolean showOrphans ) {
        List<ArrayDesignValueObject> result = new ArrayList<ArrayDesignValueObject>();

        // If no IDs are specified, then load all expressionExperiments and show the summary (if available)
        if ( arrayDesignIds == null || arrayDesignIds.length == 0) {
            result.addAll( arrayDesignService.loadAllValueObjects() );

        } else {// if ids are specified, then display only those arrayDesigns

            Collection<Long> adCol = new LinkedList<Long>(Arrays.asList( arrayDesignIds ));
            result.addAll( arrayDesignService.loadValueObjects( adCol ) );
        }

        // Filter...
        Collection<ArrayDesignValueObject> toHide = new HashSet<ArrayDesignValueObject>();
        for ( ArrayDesignValueObject a : result ) {
            if ( !showMergees && a.getIsMergee() && a.getExpressionExperimentCount() == 0 ) {
                toHide.add( a );
            }
            if ( !showOrphans && ( a.getExpressionExperimentCount() == null || a.getExpressionExperimentCount() == 0 ) ) {
                toHide.add( a );
            }
        }
        result.removeAll( toHide );

        Collections.sort( result, new ArrayDesignValueObjectComparator() );

        return result;
    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignController#getCsSummaries(ubic.gemma.web.remote.EntityDelegator)
     */
    @Override
    public Collection<CompositeSequenceMapValueObject> getCsSummaries( EntityDelegator ed ) {
        ArrayDesign arrayDesign = arrayDesignService.load( ed.getId() );
        return this.getDesignSummaries( arrayDesign );
    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignController#getDesignSummaries(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public Collection<CompositeSequenceMapValueObject> getDesignSummaries( ArrayDesign arrayDesign ) {
        Collection<Object[]> rawSummaries = compositeSequenceService.getRawSummary( arrayDesign, NUM_PROBES_TO_SHOW );
        Collection<CompositeSequenceMapValueObject> summaries = arrayDesignMapResultService
                .getSummaryMapValueObjects( rawSummaries );
        return summaries;
    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignController#getReportHtml(ubic.gemma.web.remote.EntityDelegator)
     */
    @Override
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

    /* (non-Javadoc)
     * @see ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignController#getSummaryForArrayDesign(java.lang.Long)
     */
    @Override
    public String getSummaryForArrayDesign( Long id ) {

        Collection<Long> ids = new ArrayList<Long>();
        ids.add( id );
        Collection<ArrayDesignValueObject> advos = arrayDesignService.loadValueObjects( ids );
        arrayDesignReportService.fillInValueObjects( advos );

        if ( !advos.isEmpty() && advos.toArray()[0] != null ) {
            ArrayDesignValueObject advo = ( ArrayDesignValueObject ) advos.toArray()[0];
            StringBuilder buf = new StringBuilder();

            buf.append( "<div style=\"float:left\" >" );

            if ( advo.getNumProbeAlignments() != null ) {
                buf.append( ArrayDesignHtmlUtil.getSummaryHtml( advo ) );
            } else {
                buf.append( "[Not avail.]" );
            }

            buf.append( "</div>" );
            return buf.toString();
        }
        return "[Not avail.]";
    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignController#loadArrayDesignsForShowAll(java.util.Collection)
     */
    @Override
    public Collection<ArrayDesignValueObject> loadArrayDesignsForShowAll( Long[] arrayDesignIds ) {

        Collection<ArrayDesignValueObject> valueObjects = getArrayDesigns( arrayDesignIds, true, true );

        if ( SecurityServiceImpl.isUserAdmin() ) {
            arrayDesignReportService.fillEventInformation( valueObjects );
        } else {
            auditableUtil.removeTroubledArrayDesigns( valueObjects );
        }

        arrayDesignReportService.fillInSubsumptionInfo( valueObjects );
        arrayDesignReportService.fillInValueObjects( valueObjects );

        return valueObjects;
    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignController#loadArrayDesignsSummary()
     */
    @Override
    public ArrayDesignValueObject loadArrayDesignsSummary() {

        return arrayDesignReportService.getSummaryObject();
    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignController#remove(ubic.gemma.web.remote.EntityDelegator)
     */
    @Override
    public String remove( EntityDelegator ed ) {
        ArrayDesign arrayDesign = arrayDesignService.load( ed.getId() );
        if ( arrayDesign == null ) {
            throw new EntityNotFoundException( ed.getId() + " not found" );
        }
        Collection<BioAssay> assays = arrayDesignService.getAllAssociatedBioAssays( ed.getId() );
        if ( assays.size() != 0 ) {
            throw new IllegalArgumentException( "Cannot delete " + arrayDesign
                    + ", it is used by an expression experiment" );
        }

        RemoveArrayJob job = new RemoveArrayJob( new TaskCommand( arrayDesign.getId() ) );

        super.startTask( job );

        return job.getTaskId();
    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignController#showAllArrayDesigns(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    @RequestMapping("/showAllArrayDesigns.html")
    public ModelAndView showAllArrayDesigns( HttpServletRequest request, HttpServletResponse response ) {
        return new ModelAndView( "arrayDesigns" );
    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignController#showArrayDesign(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    @RequestMapping({ "/showArrayDesign.html", "/" })
    public ModelAndView showArrayDesign( HttpServletRequest request, HttpServletResponse response ) {
        String name = request.getParameter( "name" );
        String idStr = request.getParameter( "id" );

        if ( ( name == null ) && ( idStr == null ) ) {
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html" ) ).addObject(
                    "message", "Must provide an platform name or id. Displaying all platforms" );

        }
        ArrayDesign arrayDesign = null;
        if ( idStr != null ) {
            arrayDesign = arrayDesignService.load( Long.parseLong( idStr ) );
            request.setAttribute( "id", idStr );
        } else if ( name != null ) {
            arrayDesign = arrayDesignService.findByName( name );
            request.setAttribute( "name", name );
        }

        if ( arrayDesign == null ) {
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html" ) ).addObject(
                    "message", "Unable to load platform with id: " + idStr + ". Displaying all platforms" );
        }

        arrayDesign = arrayDesignService.thawLite( arrayDesign );

        long id = arrayDesign.getId();

        Integer numCompositeSequences = arrayDesignService.getCompositeSequenceCount( arrayDesign );
        Collection<ExpressionExperiment> ee = arrayDesignService.getExpressionExperiments( arrayDesign );
        int numExpressionExperiments = ee.size();

        Collection<Taxon> t = arrayDesignService.getTaxa( id );

        Taxon primaryTaxon = arrayDesign.getPrimaryTaxon();

        String taxa = formatTaxa( primaryTaxon, t );

        String colorString = formatTechnologyType( arrayDesign );

        ArrayDesignValueObject summary = arrayDesignReportService.getSummaryObject( id );

        String eeIds = formatExpressionExperimentIds( ee );

        ModelAndView mav = new ModelAndView( "arrayDesign.detail" );

        AuditEvent troubleEvent = auditTrailService.getLastTroubleEvent( arrayDesign );
        if ( troubleEvent != null ) {
            mav.addObject( "troubleEvent", troubleEvent );
            mav.addObject( "troubleEventDescription", StringEscapeUtils.escapeHtml( troubleEvent.toString() ) );
        }
        AuditEvent validatedEvent = auditTrailService.getLastValidationEvent( arrayDesign );
        if ( validatedEvent != null ) {
            mav.addObject( "validatedEvent", validatedEvent );
            mav.addObject( "validatedEventDescription", StringEscapeUtils.escapeHtml( validatedEvent.toString() ) );
        }

        Collection<ArrayDesign> subsumees = arrayDesignService.thawLite( arrayDesign.getSubsumedArrayDesigns() );

        ArrayDesign subsumer = arrayDesign.getSubsumingArrayDesign();

        Collection<ArrayDesign> mergees = arrayDesignService.thawLite( arrayDesign.getMergees() );

        ArrayDesign merger = arrayDesign.getMergedInto();

        getAnnotationFileLinks( arrayDesign, mav );

        mav.addObject( "subsumer", subsumer );
        mav.addObject( "subsumees", subsumees );
        mav.addObject( "merger", merger );
        mav.addObject( "mergees", mergees );
        mav.addObject( "taxon", taxa );
        mav.addObject( "arrayDesign", arrayDesign );
        mav.addObject( "alternateNames", this.formatAlternateNames( arrayDesign ) );
        mav.addObject( "numCompositeSequences", numCompositeSequences );
        mav.addObject( "numExpressionExperiments", numExpressionExperiments );

        mav.addObject( "expressionExperimentIds", eeIds );
        mav.addObject( "technologyType", colorString );
        mav.addObject( "summary", summary );
        return mav;
    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignController#showCompositeSequences(javax.servlet.http.HttpServletRequest)
     */
    @Override
    @RequestMapping("/showCompositeSequenceSummary.html")
    public ModelAndView showCompositeSequences( HttpServletRequest request ) {

        String arrayDesignIdStr = request.getParameter( "id" );

        if ( arrayDesignIdStr == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Must provide a platform name or Id" );
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

    /* (non-Javadoc)
     * @see ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignController#showExpressionExperiments(javax.servlet.http.HttpServletRequest)
     */
    @Override
    @RequestMapping("/showExpressionExperiments.html")
    public ModelAndView showExpressionExperiments( HttpServletRequest request ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );
        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Not found" );
        }

        ArrayDesign arrayDesign = arrayDesignService.load( id );
        if ( arrayDesign == null ) {
            return new ModelAndView( new RedirectView( "/Gemma/arrays/showAllArrayDesigns.html" ) ).addObject(
                    "message", "Platform with id=" + id + " not found" );
        }

        Collection<ExpressionExperiment> ees = arrayDesignService.getExpressionExperiments( arrayDesign );
        Collection<Long> eeIds = new ArrayList<Long>();
        for ( ExpressionExperiment object : ees ) {
            eeIds.add( object.getId() );
        }
        String ids = StringUtils.join( eeIds.toArray(), "," );
        return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html?id="
                + ids ) );
    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignController#updateReport(ubic.gemma.web.remote.EntityDelegator)
     */
    @Override
    public String updateReport( EntityDelegator ed ) {
        GenerateSummary runner = new GenerateSummary( new TaskCommand( ed.getId() ) );
        super.startTask( runner );
        return runner.getTaskId();
    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignController#updateReportById(java.lang.Long)
     */
    @Override
    public String updateReportById( Long id ) {
        GenerateSummary runner = new GenerateSummary( new TaskCommand( id ) );
        super.startTask( runner );
        return runner.getTaskId();
    }

    @Override
    protected BackgroundJob<?> getInProcessRunner( TaskCommand command ) {
        return null;
    }

    @Override
    protected BackgroundJob<?> getSpaceRunner( TaskCommand command ) {
        return null;
    }

    /**
     * @param ad
     * @return
     */
    private String formatAlternateNames( ArrayDesign ad ) {
        Collection<String> names = new HashSet<String>();
        for ( AlternateName an : ad.getAlternateNames() ) {
            names.add( an.getName() );
        }
        return StringUtils.join( names, "; " );
    }

    private String formatExpressionExperimentIds( Collection<ExpressionExperiment> ee ) {
        Collection<Long> eeIds = EntityUtils.getIds( ee );
        String eeIdString = StringUtils.join( eeIds, "," );
        return eeIdString;
    }

    /**
     * Method to format taxon list for display.
     * 
     * @param primaryTaxon
     * @param taxonSet Collection of taxon used to create array/platform
     * @return Alpabetically sorted semicolon separated list of scientific names of taxa used on array/platform
     */
    private String formatTaxa( Taxon primaryTaxon, Collection<Taxon> taxonSet ) {

        taxonService.thaw( primaryTaxon );

        String taxonListString = primaryTaxon.getScientificName();
        if ( !taxonSet.isEmpty() ) {
            Collection<String> taxonList = new TreeSet<String>();
            for ( Taxon taxon : taxonSet ) {
                if ( taxon.equals( primaryTaxon ) ) continue;

                taxonService.thaw( taxon );
                taxonList.add( taxon.getScientificName() );
            }

            if ( taxonList.size() > 0 ) {
                taxonListString = taxonListString + " (primary; Also contains sequences from: "
                        + StringUtils.join( taxonList, "; " ) + ")";
            }

        }

        return taxonListString;
    }

    /**
     * @param arrayDesign
     * @return
     */
    private String formatTechnologyType( ArrayDesign arrayDesign ) {
        TechnologyType technologyType = arrayDesign.getTechnologyType();

        if ( technologyType == null ) {
            return "Not specified";
        }

        String techType = technologyType.getValue();
        String colorString = "";
        if ( techType.equalsIgnoreCase( "ONECOLOR" ) ) {
            colorString = "one-color";
        } else if ( techType.equalsIgnoreCase( "TWOCOLOR" ) ) {
            colorString = "two-color";
        } else if ( techType.equalsIgnoreCase( "DUALMODE" ) ) {
            colorString = "dual mode";
        } else {
            colorString = "Not specified";
        }
        return colorString;
    }

    /**
     * @param arrayDesign
     * @param mav
     */
    private void getAnnotationFileLinks( ArrayDesign arrayDesign, ModelAndView mav ) {

        String mungedShortName = ArrayDesignAnnotationServiceImpl.mungeFileName( arrayDesign.getShortName() );
        File fnp = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + mungedShortName
                + ArrayDesignAnnotationService.NO_PARENTS_FILE_SUFFIX
                + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX );

        File fap = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + mungedShortName
                + ArrayDesignAnnotationService.STANDARD_FILE_SUFFIX
                + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX );

        File fbp = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + mungedShortName
                + ArrayDesignAnnotationService.BIO_PROCESS_FILE_SUFFIX
                + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX );

        // context here is Gemma/arrays
        if ( fnp.exists() ) {
            mav.addObject( "noParentsAnnotationLink", "downloadAnnotationFile.html?id=" + arrayDesign.getId()
                    + "&fileType=noParents" );
        }
        if ( fap.exists() ) {
            mav.addObject( "allParentsAnnotationLink", "downloadAnnotationFile.html?id=" + arrayDesign.getId()
                    + "&fileType=allParents" );
        }
        if ( fbp.exists() ) {
            mav.addObject( "bioProcessAnnotationLink", "downloadAnnotationFile.html?id=" + arrayDesign.getId()
                    + "&fileType=bioProcess" );
        }

    }
}

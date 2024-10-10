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

import gemma.gsec.util.SecurityUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.analysis.sequence.ArrayDesignMapResultService;
import ubic.gemma.core.analysis.sequence.CompositeSequenceMapValueObject;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationServiceImpl;
import ubic.gemma.core.job.AbstractTask;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.AlternateName;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.remote.JsonReaderResponse;
import ubic.gemma.web.remote.ListBatchCommand;
import ubic.gemma.web.taglib.arrayDesign.ArrayDesignHtmlUtil;
import ubic.gemma.web.util.EntityNotFoundException;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

/**
 * Note: do not use parametrized collections as parameters for ajax methods in this class! Type information is lost
 * during proxy creation so DWR can't figure out what type of collection the method should take. See bug 2756. Use
 * arrays instead.
 *
 * @author keshav
 */
@Controller
@RequestMapping("/arrays")
public class ArrayDesignController {

    private static final Log log = LogFactory.getLog( ArrayDesignController.class.getName() );

    /**
     * Instead of showing all the probes for the array, we might only fetch some of them.
     */
    private static final int NUM_PROBES_TO_SHOW = 500;
    private static final boolean AJAX = true;

    @Autowired
    private ArrayDesignMapResultService arrayDesignMapResultService;
    @Autowired
    private ArrayDesignReportService arrayDesignReportService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private CompositeSequenceService compositeSequenceService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private TaskRunningService taskRunningService;
    @Autowired
    private ArrayDesignAnnotationService annotationFileService;

    @Value("${gemma.support.email}")
    private String supportEmail;

    @SuppressWarnings("unused")
    public String addAlternateName( Long arrayDesignId, String alternateName ) {
        ArrayDesign ad = arrayDesignService.loadOrFail( arrayDesignId, EntityNotFoundException::new, "No such platform with id=" + arrayDesignId );

        if ( StringUtils.isBlank( alternateName ) ) {
            return formatAlternateNames( ad );
        }

        AlternateName newName = AlternateName.Factory.newInstance( alternateName );

        ad.getAlternateNames().add( newName );

        arrayDesignService.update( ad );
        return formatAlternateNames( ad );
    }

    @SuppressWarnings("unused")
    public JsonReaderResponse<ArrayDesignValueObject> browse( ListBatchCommand batch, Long[] ids, boolean showMerged,
            boolean showOrphans ) {
        Collection<ArrayDesignValueObject> valueObjects = getArrayDesigns( ids, showMerged, showOrphans );

        if ( !SecurityUtil.isUserAdmin() ) {
            CollectionUtils.filter( valueObjects, vo -> !vo.getTroubled() );
        }
        int count = valueObjects.size();

        arrayDesignReportService.fillInValueObjects( valueObjects );
        arrayDesignReportService.fillEventInformation( valueObjects );
        arrayDesignReportService.fillInSubsumptionInfo( valueObjects );

        return new JsonReaderResponse<>( new ArrayList<>( valueObjects ), count );
    }

    @RequestMapping(value = "/deleteArrayDesign.html", method = RequestMethod.POST)
    public ModelAndView delete( @RequestParam("id") Long id ) {
        ArrayDesign arrayDesign = arrayDesignService.loadOrFail( id,
                EntityNotFoundException::new, "Platform with id=" + id + " not found" );

        // check that no EE depend on the arraydesign we want to remove
        // Do this by checking if there are any bioassays that depend this AD
        Collection<BioAssay> assays = arrayDesignService.getAllAssociatedBioAssays( arrayDesign );
        if ( !assays.isEmpty() ) {
            return new ModelAndView( new RedirectView( "/arrays/showAllArrayDesigns.html", true ) )
                    .addObject( "message", "Array  " + arrayDesign.getName()
                            + " can't be deleted. Dataset has a dependency on this Array." );
        }

        String taskId = taskRunningService.submitTaskCommand( new TaskCommand( arrayDesign.getId() ) );

        return new ModelAndView().addObject( "taskId", taskId );

    }

    @RequestMapping(value = "/downloadAnnotationFile.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public void downloadAnnotationFile( @RequestParam("id") Long arrayDesignId, @RequestParam(value = "fileType", required = false) String fileType, HttpServletResponse response ) throws IOException {
        if ( fileType == null || fileType.equalsIgnoreCase( "allParents" ) ) {
            fileType = ArrayDesignAnnotationService.STANDARD_FILE_SUFFIX;
        } else if ( fileType.equalsIgnoreCase( "noParents" ) ) {
            fileType = ArrayDesignAnnotationService.NO_PARENTS_FILE_SUFFIX;
        } else if ( fileType.equalsIgnoreCase( "bioProcess" ) ) {
            fileType = ArrayDesignAnnotationService.BIO_PROCESS_FILE_SUFFIX;
        } else {
            throw new IllegalArgumentException( "Unknown file type for the 'fileType' query parameter." );
        }
        ArrayDesign arrayDesign = arrayDesignService.load( arrayDesignId );
        if ( arrayDesign == null ) {
            throw new EntityNotFoundException( String.format( "No array design with ID %d was found.", arrayDesignId ) );
        }

        String fileBaseName = ArrayDesignAnnotationServiceImpl.mungeFileName( arrayDesign.getShortName() );
        String fileName = fileBaseName + fileType + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX;
        File f = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + fileName );

        if ( !f.exists() || !f.canRead() ) {
            // Experimental. Ideally make a background process. But usually these files should be available anyway...
            log.info( String.format( "Annotation file %s not found, creating for %s...", f.getPath(), arrayDesign ) );
            try {
                annotationFileService.create( arrayDesign, true, false ); // include GO by default ... but this might be changed.
                //also, don't delete associated files, as this takes a while and since this on-demand generation is just to handle the case of the file being missing, not annotations changing.
            } catch ( Exception e ) {
                log.error( String.format( "Failed to create annotation file %s for %s.", f.getPath(), arrayDesign ), e );
            }
        }

        try ( InputStream is = new FileInputStream( f ) ) {
            response.setContentType( MediaType.APPLICATION_OCTET_STREAM_VALUE );
            response.setHeader( "Content-Disposition", "attachment; filename=" + fileName );
            // response.setContentType( "application/x-gzip" ); // see Bug4206
            response.setContentLength( ( int ) f.length() );
            IOUtils.copy( is, response.getOutputStream() );
        } catch ( FileNotFoundException e ) {
            throw new EntityNotFoundException(
                    String.format( "The annotation file could not be found for %s. Please contact %s for assistance.",
                            arrayDesign.getShortName(), supportEmail ), e );
        }
    }

    @RequestMapping(value = "/filterArrayDesigns.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView filter( @RequestParam("filter") String filter ) {
        StopWatch overallWatch = new StopWatch();
        overallWatch.start();

        // Validate the filtering search criteria.
        if ( StringUtils.isBlank( filter ) ) {
            return new ModelAndView( new RedirectView( "/arrays/showAllArrayDesigns.html", true ) )
                    .addObject( "message", "No search criteria provided" );
        }

        Collection<SearchResult<ArrayDesign>> searchResults;
        try {
            searchResults = searchService.search( SearchSettings.arrayDesignSearch( filter ) )
                    .getByResultObjectType( ArrayDesign.class );
        } catch ( SearchException e ) {
            return new ModelAndView( new RedirectView( "/arrays/showAllArrayDesigns.html", true ) )
                    .addObject( "message", "Invalid search settings: " + e.getMessage() );
        }

        if ( ( searchResults == null ) || ( searchResults.isEmpty() ) ) {
            return new ModelAndView( new RedirectView( "/arrays/showAllArrayDesigns.html", true ) )
                    .addObject( "message", "No search criteria provided" );

        }

        StringBuilder list = new StringBuilder();

        if ( searchResults.size() == 1 ) {
            ArrayDesign arrayDesign = arrayDesignService.loadOrFail( searchResults.iterator().next().getResultId() );
            return new ModelAndView(
                    new RedirectView( "/arrays/showArrayDesign.html?id=" + arrayDesign.getId(), true ) )
                    .addObject( "message",
                            "Matched one : " + arrayDesign.getName() + "(" + arrayDesign.getShortName() + ")" );
        }

        for ( SearchResult<ArrayDesign> ad : searchResults ) {
            list.append( ad.getResultId() ).append( "," );
        }

        overallWatch.stop();
        long overallElapsed = overallWatch.getTime();
        log.info( "Generating the AD list:  (" + list + ") took: " + overallElapsed / 1000 + "s " );

        return new ModelAndView( new RedirectView( "/arrays/showAllArrayDesigns.html?id=" + list, true ) )
                .addObject( "message", searchResults.size() + " Platforms matched your search." );

    }

    @RequestMapping(value = "/generateArrayDesignSummary.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView generateSummary( @RequestParam(value = "id", required = false) Long id ) {
        // if no IDs are specified, then load all expressionExperiments and show the summary (if available)
        GenerateArraySummaryLocalTask job;
        if ( id == null ) {
            job = new GenerateArraySummaryLocalTask( new TaskCommand() );
            String taskId = taskRunningService.submitTask( job );
            return new ModelAndView( new RedirectView( "/arrays/showAllArrayDesigns.html", true ) )
                    .addObject( "taskId", taskId );
        }

        job = new GenerateArraySummaryLocalTask( new TaskCommand( id ) );
        String taskId = taskRunningService.submitTask( job );
        return new ModelAndView( new RedirectView( "/arrays/showAllArrayDesigns.html?id=" + id, true ) )
                .addObject( "taskId", taskId );
    }

    public Collection<ArrayDesignValueObject> getArrayDesigns( Long[] arrayDesignIds, boolean showMergees,
            boolean showOrphans ) {
        List<ArrayDesignValueObject> result = new ArrayList<>();

        // If no IDs are specified, then load all expressionExperiments and show the summary (if available)
        if ( arrayDesignIds == null || arrayDesignIds.length == 0 ) {
            result.addAll( arrayDesignService.loadValueObjectsWithCache( null, null ) );

        } else {// if ids are specified, then display only those arrayDesigns

            Collection<Long> adCol = new LinkedList<>( Arrays.asList( arrayDesignIds ) );
            result.addAll( arrayDesignService.loadValueObjectsWithCache( Filters.by( arrayDesignService.getFilter( "id", Long.class, Filter.Operator.in, adCol ) ), null ) );
        }

        // Filter...
        Collection<ArrayDesignValueObject> toHide = new HashSet<>();
        for ( ArrayDesignValueObject a : result ) {
            if ( !showMergees && a.getIsMergee() && a.getNumberOfExpressionExperiments() == 0 ) {
                toHide.add( a );
            }
            if ( !showOrphans && ( a.getNumberOfExpressionExperiments() == null
                    || a.getNumberOfExpressionExperiments() == 0 ) ) {
                toHide.add( a );
            }
        }
        result.removeAll( toHide );

        result.sort( Comparator.comparing( ArrayDesignValueObject::getId ) );

        return result;
    }

    @SuppressWarnings("unused")
    public Collection<CompositeSequenceMapValueObject> getCsSummaries( EntityDelegator<ArrayDesign> ed ) {
        ArrayDesign arrayDesign = arrayDesignService.load( ed.getId() );
        return this.getDesignSummaries( arrayDesign );
    }

    public Collection<CompositeSequenceMapValueObject> getDesignSummaries( ArrayDesign arrayDesign ) {
        Collection<Object[]> rawSummaries = compositeSequenceService.getRawSummary( arrayDesign, NUM_PROBES_TO_SHOW );
        if ( rawSummaries == null ) {
            return new HashSet<>();
        }
        return arrayDesignMapResultService.getSummaryMapValueObjects( rawSummaries );
    }

    @SuppressWarnings("unused")
    public ArrayDesignValueObjectExt getDetails( Long id ) {

        ArrayDesign arrayDesign = this.getADSafely( id );

        ArrayDesignValueObject vo = arrayDesignService.loadValueObject( arrayDesign );
        if ( vo == null ) {
            throw new EntityNotFoundException(
                    "You do not have appropriate rights to see this platform. This is likely due "
                            + "to the platform being marked as unusable." );
        }
        arrayDesignReportService.fillInValueObjects( Collections.singleton( vo ) );
        arrayDesignReportService.fillInSubsumptionInfo( Collections.singleton( vo ) );

        ArrayDesignValueObjectExt result = new ArrayDesignValueObjectExt( vo );
        this.setExtRefsAndCounts( result, arrayDesign );
        this.setAlternateNames( result, arrayDesign );
        this.setExtRefsAndCounts( result, arrayDesign );
        this.setSummaryInfo( result, id );
        result.setSwitchedExpressionExperimentCount( arrayDesignService.getSwitchedExpressionExperimentCount( arrayDesign ) );

        populateMergeStatus( arrayDesign, result ); // SLOW if we follow down to mergees of mergees etc.

        if ( result.getIsAffymetrixAltCdf() )
            result.setAlternative( new ArrayDesignValueObject( arrayDesign.getAlternativeTo() ) );

        return result;
    }

    /**
     * Loads, checks not null, and thaws the Array Design with given ID;
     */
    private ArrayDesign getADSafely( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ID cannot be null" );
        }
        return arrayDesignService.loadAndThawLiteOrFail( id,
                EntityNotFoundException::new, "No platform with id=" + id + " could be loaded" );
    }

    /**
     * Set alternate names on the given value object.
     */
    private void setAlternateNames( ArrayDesignValueObjectExt result, ArrayDesign arrayDesign ) {
        Collection<String> names = new HashSet<>();
        for ( AlternateName an : arrayDesign.getAlternateNames() ) {
            names.add( an.getName() );
        }
        result.setAlternateNames( names );
    }

    /**
     * Sets external references, design element count and express. experiment count on the given value object.
     */
    private void setExtRefsAndCounts( ArrayDesignValueObjectExt result, ArrayDesign arrayDesign ) {
        Integer numCompositeSequences = arrayDesignService.getCompositeSequenceCount( arrayDesign ).intValue();

        long numExpressionExperiments = arrayDesignService.numExperiments( arrayDesign );

        Set<DatabaseEntryValueObject> externalReferences = new HashSet<>();
        for ( DatabaseEntry en : arrayDesign.getExternalReferences() ) {
            externalReferences.add( new DatabaseEntryValueObject( en ) );
        }
        result.setExternalReferences( externalReferences );
        result.setDesignElementCount( numCompositeSequences );
        result.setExpressionExperimentCount( numExpressionExperiments );
    }

    /**
     * Sets the summary info on the given value object.
     */
    private void setSummaryInfo( ArrayDesignValueObjectExt result, Long id ) {
        ArrayDesignValueObject summary = arrayDesignReportService.getSummaryObject( id );
        if ( summary != null ) {
            result.setNumProbeAlignments( summary.getNumProbeAlignments() );
            result.setNumProbesToGenes( summary.getNumProbesToGenes() );
            result.setNumProbeSequences( summary.getNumProbeSequences() );
        }
    }

    @SuppressWarnings("unused")
    public Map<String, String> getReportHtml( EntityDelegator<ArrayDesign> ed ) {
        assert ed.getId() != null;
        ArrayDesignValueObject summary = arrayDesignReportService.getSummaryObject( ed.getId() );
        Map<String, String> result = new HashMap<>();

        result.put( "id", ed.getId().toString() );
        if ( summary == null )
            result.put( "html", "Not available" );
        else
            result.put( "html", ArrayDesignHtmlUtil.getSummaryHtml( summary ) );
        return result;
    }

    @SuppressWarnings("unused")
    public String getSummaryForArrayDesign( Long id ) {
        ArrayDesignValueObject advo = arrayDesignService.loadValueObjectById( id );
        if ( advo == null ) {
            return "[Not avail.]";
        }
        arrayDesignReportService.fillInValueObjects( Collections.singleton( advo ) );
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

    @SuppressWarnings("unused")
    public Collection<ArrayDesignValueObject> loadArrayDesignsForShowAll( Long[] arrayDesignIds ) {

        Collection<ArrayDesignValueObject> valueObjects = getArrayDesigns( arrayDesignIds, true, true );

        if ( SecurityUtil.isUserAdmin() ) {
            arrayDesignReportService.fillEventInformation( valueObjects );
        } else {
            CollectionUtils.filter( valueObjects, vo -> !vo.getTroubled() );
        }

        arrayDesignReportService.fillInSubsumptionInfo( valueObjects );
        arrayDesignReportService.fillInValueObjects( valueObjects );

        return valueObjects;
    }

    @SuppressWarnings("unused")
    public ArrayDesignValueObject loadArrayDesignsSummary() {

        return arrayDesignReportService.getSummaryObject();
    }

    public String remove( EntityDelegator<ArrayDesign> ed ) {
        ArrayDesign arrayDesign = arrayDesignService.load( ed.getId() );
        if ( arrayDesign == null ) {
            throw new EntityNotFoundException( ed.getId() + " not found" );
        }
        Collection<BioAssay> assays = arrayDesignService.getAllAssociatedBioAssays( arrayDesign );
        if ( !assays.isEmpty() ) {
            throw new IllegalArgumentException( "Cannot remove " + arrayDesign + ", it is used by an expression experiment" );
        }

        RemoveArrayLocalTask job = new RemoveArrayLocalTask( new TaskCommand( arrayDesign.getId() ) );

        return taskRunningService.submitTask( job );

    }

    @RequestMapping(value = "/showAllArrayDesigns.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView showAllArrayDesigns() {
        return new ModelAndView( "arrayDesigns" );
    }

    @RequestMapping(value = { "/showArrayDesign.html", "/" }, params = { "id" }, method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView showArrayDesign( @RequestParam("id") Long id ) {
        return showArrayDesignInternal( arrayDesignService.loadOrFail( id, EntityNotFoundException::new, "No platform was found for ID " + id + "." ) );
    }

    @RequestMapping(value = { "/showArrayDesign.html", "/" }, params = { "name" }, method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView showArrayDesignByName( @RequestParam("name") String name ) {
        ArrayDesign arrayDesign = arrayDesignService.findByShortName( name );
        if ( arrayDesign == null ) {
            arrayDesign = arrayDesignService.findByName( name ).iterator().next();
        }
        if ( arrayDesign == null ) {
            throw new EntityNotFoundException( "No platform was found for the provided name." );
        }
        return showArrayDesignInternal( arrayDesign );
    }

    private ModelAndView showArrayDesignInternal( ArrayDesign arrayDesign ) {
        return new ModelAndView( "arrayDesign.detail" )
                .addObject( "arrayDesignId", arrayDesign.getId() )
                .addObject( "arrayDesignShortName", arrayDesign.getShortName() )
                .addObject( "arrayDesignName", arrayDesign.getName() );
    }

    @RequestMapping(value = "/showCompositeSequenceSummary.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView showCompositeSequences( @RequestParam("id") Long id ) {
        ArrayDesign arrayDesign = arrayDesignService.loadOrFail( id,
                EntityNotFoundException::new, "No platform was found for ID " + id + "." );
        ModelAndView mav = new ModelAndView( "compositeSequences.geneMap" );

        if ( !AJAX ) {
            Collection<CompositeSequenceMapValueObject> compositeSequenceSummary = getDesignSummaries( arrayDesign );
            if ( compositeSequenceSummary == null || compositeSequenceSummary.isEmpty() ) {
                throw new EntityNotFoundException( "No probes found for " + arrayDesign );
            }
            mav.addObject( "sequenceData", compositeSequenceSummary );
            mav.addObject( "numCompositeSequences", compositeSequenceSummary.size() );
        }

        mav.addObject( "arrayDesign", arrayDesign );

        return mav;
    }

    @RequestMapping(value = "/showExpressionExperiments.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView showExpressionExperiments( @RequestParam("id") Long id ) {
        ArrayDesign arrayDesign = arrayDesignService.load( id );
        if ( arrayDesign == null ) {
            // FIXME: treat this as a 404 instead
            return new ModelAndView( new RedirectView( "/arrays/showAllArrayDesigns.html", true ) )
                    .addObject( "message", "Platform with id=" + id + " not found" );
        }
        // seems inefficient? but need security filtering.
        Collection<ExpressionExperiment> ees = arrayDesignService.getExpressionExperiments( arrayDesign );

        String ids = StringUtils.join( EntityUtils.getIds( ees ).toArray(), "," );
        return new ModelAndView(
                new RedirectView( "/expressionExperiment/showAllExpressionExperiments.html?id=" + ids, true ) );
    }

    @SuppressWarnings("unused")
    public String updateReport( EntityDelegator<ArrayDesign> ed ) {
        GenerateArraySummaryLocalTask job = new GenerateArraySummaryLocalTask( new TaskCommand( ed.getId() ) );
        return taskRunningService.submitTask( job );
    }

    @SuppressWarnings("unused")
    public String updateReportById( Long id ) {
        GenerateArraySummaryLocalTask job = new GenerateArraySummaryLocalTask( new TaskCommand( id ) );
        return taskRunningService.submitTask( job );
    }

    private String formatAlternateNames( ArrayDesign ad ) {
        Collection<String> names = new HashSet<>();
        for ( AlternateName an : ad.getAlternateNames() ) {
            names.add( an.getName() );
        }
        return StringUtils.join( names, "; " );
    }

    /**
     * Recursively populate the status. Recursion only goes 'up' - so the subsumer and merger, not the subsumees and
     * mergees.
     */
    private void populateMergeStatus( ArrayDesign arrayDesign, ArrayDesignValueObjectExt result ) {
        assert arrayDesign != null;
        assert result != null;

        Collection<ArrayDesign> subsumees = arrayDesign.getSubsumedArrayDesigns();
        subsumees = arrayDesignService.thawLite( subsumees );
        ArrayDesign subsumer = arrayDesign.getSubsumingArrayDesign();
        Collection<ArrayDesign> mergees = arrayDesign.getMergees();
        mergees = arrayDesignService.thawLite( mergees );
        ArrayDesign merger = arrayDesign.getMergedInto();

        if ( subsumees != null && !subsumees.isEmpty() ) {
            Collection<ArrayDesignValueObject> subsumeesVos = ArrayDesignValueObject.create( subsumees );
            result.setSubsumees( subsumeesVos );
        }
        if ( subsumer != null ) {
            ArrayDesignValueObjectExt subsumerVo = new ArrayDesignValueObjectExt(
                    new ArrayDesignValueObject( subsumer ) );
            result.setSubsumer( subsumerVo );
            // subsumer = arrayDesignService.thawLite( subsumer );
            // populateMergeStatus( subsumer, subsumerVo );
        }
        if ( mergees != null && !mergees.isEmpty() ) {
            Collection<ArrayDesignValueObject> mergeesVos = ArrayDesignValueObject.create( mergees );
            result.setMergees( mergeesVos );
        }
        if ( merger != null ) {
            ArrayDesignValueObjectExt mergerVo = new ArrayDesignValueObjectExt( new ArrayDesignValueObject( merger ) );
            result.setMerger( mergerVo );
            // merger = arrayDesignService.thawLite( merger );
            // populateMergeStatus( merger, mergerVo );
        }

    }

    /**
     * Inner class used for building array design summary
     */
    class GenerateArraySummaryLocalTask extends AbstractTask<TaskCommand> {

        public GenerateArraySummaryLocalTask( TaskCommand command ) {
            super( command );
        }

        @Override
        public TaskResult call() {

            if ( this.taskCommand.getEntityId() == null ) {
                log.info( "Generating summary for all platforms" );
                arrayDesignReportService.generateArrayDesignReport();
                return new TaskResult( taskCommand,
                        new ModelAndView( new RedirectView( "/arrays/showAllArrayDesignStatistics.html", true ) ) );
            }
            ArrayDesignValueObject report = arrayDesignReportService
                    .generateArrayDesignReport( taskCommand.getEntityId() );
            return new TaskResult( taskCommand, report );

        }
    }

    /**
     * Inner class used for deleting array designs
     */
    class RemoveArrayLocalTask extends AbstractTask<TaskCommand> {

        public RemoveArrayLocalTask( TaskCommand command ) {
            super( command );
        }

        @Override
        public TaskResult call() {
            ArrayDesign ad = arrayDesignService.loadOrFail( taskCommand.getEntityId(),
                    EntityNotFoundException::new, "Could not load platform with id=" + taskCommand.getEntityId() );
            arrayDesignService.remove( ad );
            return new TaskResult( taskCommand,
                    new ModelAndView( new RedirectView( "/arrays/showAllArrayDesigns.html", true ) )
                            .addObject( "message", "Array " + ad.getShortName() + " removed from Database." ) );

        }

    }

}

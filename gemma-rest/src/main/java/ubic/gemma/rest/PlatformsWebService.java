/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.FilteredAndPaginatedResponseDataObject;
import ubic.gemma.rest.util.MediaTypeUtils;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import static ubic.gemma.rest.util.Responders.paginate;
import static ubic.gemma.rest.util.Responders.respond;

/**
 * RESTful interface for platforms.
 *
 * @author tesarst
 */
@Service
@Path("/platforms")
public class PlatformsWebService {

    private static final String ERROR_ANNOTATION_FILE_NOT_AVAILABLE = "Annotation file for platform %s does not exist or can not be accessed.";

    private GeneService geneService;
    private ArrayDesignService arrayDesignService;
    private CompositeSequenceService compositeSequenceService;
    private ArrayDesignAnnotationService annotationFileService;
    private PlatformArgService arrayDesignArgService;
    private CompositeSequenceArgService probeArgService;

    /**
     * Required by spring
     */
    public PlatformsWebService() {
    }

    /**
     * Constructor for service autowiring
     */
    @Autowired
    public PlatformsWebService( GeneService geneService, ArrayDesignService arrayDesignService,
            CompositeSequenceService compositeSequenceService, ArrayDesignAnnotationService annotationFileService,
            PlatformArgService arrayDesignArgService, CompositeSequenceArgService probeArgService ) {
        this.geneService = geneService;
        this.arrayDesignService = arrayDesignService;
        this.compositeSequenceService = compositeSequenceService;
        this.annotationFileService = annotationFileService;
        this.arrayDesignArgService = arrayDesignArgService;
        this.probeArgService = probeArgService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all platforms")
    public FilteredAndPaginatedResponseDataObject<ArrayDesignValueObject> getPlatforms( // Params:
            @QueryParam("filter") @DefaultValue("") FilterArg<ArrayDesign> filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg<ArrayDesign> sort // Optional, default +id
    ) {
        Filters filters = arrayDesignArgService.getFilters( filter );
        return paginate( arrayDesignService::loadValueObjects, filters, new String[] { "id" },
                arrayDesignArgService.getSort( sort ), offset.getValue(), limit.getValue() );
    }

    @GET
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Count platforms matching the provided filter")
    public ResponseDataObject<Long> getNumberOfPlatforms(
            @QueryParam("filter") @DefaultValue("") FilterArg<ArrayDesign> filter ) {
        return respond( arrayDesignService.count( arrayDesignArgService.getFilters( filter ) ) );
    }

    /**
     * Retrieves all datasets matching the given identifiers.
     *
     * @param platformsArg a list of identifiers, separated by commas (','). Identifiers can either be the
     *                    ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                    is more efficient.
     *                    <p>
     *                    Only datasets that user has access to will be available.
     *                    </p>
     *                    <p>
     *                    Do not combine different identifiers in one query.
     *                    </p>
     */
    @GET
    @Path("/{platform}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all platforms matching a set of platform identifiers")
    public FilteredAndPaginatedResponseDataObject<ArrayDesignValueObject> getPlatformsByIds( // Params:
            @PathParam("platform") PlatformArrayArg platformsArg, // Optional
            @QueryParam("filter") @DefaultValue("") FilterArg<ArrayDesign> filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg<ArrayDesign> sort // Optional, default +id
    ) {
        Filters filters = arrayDesignArgService.getFilters( filter )
                .and( arrayDesignArgService.getFilters( platformsArg ) );
        return paginate( arrayDesignService::loadValueObjects, filters, new String[] { "id" },
                arrayDesignArgService.getSort( sort ), offset.getValue(), limit.getValue() );
    }

    @GET
    @Path("/blacklisted")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured("GROUP_ADMIN")
    @Operation(summary = "Retrieve all blacklisted platforms", hidden = true)
    public FilteredAndPaginatedResponseDataObject<ArrayDesignValueObject> getBlacklistedPlatforms(
            @QueryParam("filter") @DefaultValue("") FilterArg<ArrayDesign> filter,
            @QueryParam("sort") @DefaultValue("+id") SortArg<ArrayDesign> sort,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset,
            @QueryParam("limit") @DefaultValue("20") LimitArg limit
    ) {
        return paginate( arrayDesignService::loadBlacklistedValueObjects, arrayDesignArgService.getFilters( filter ),
                new String[] { "id" }, arrayDesignArgService.getSort( sort ), offset.getValue(), limit.getValue() );
    }

    /**
     * Retrieves experiments in the given platform.
     *
     * @param platformArg can either be the ArrayDesign ID or its short name (e.g. "GPL1355" ). Retrieval by ID
     *                    is more efficient. Only platforms that user has access to will be available.
     * @param offset      optional parameter (defaults to 0) skips the specified amount of datasets when retrieving them
     *                    from the database.
     * @param limit       optional parameter (defaults to 20) limits the result to specified amount of datasets. Use 0
     */
    @GET
    @Path("/{platform}/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all experiments using a given platform")
    public PaginatedResponseDataObject<ExpressionExperimentValueObject> getPlatformDatasets( // Params:
            @PathParam("platform") PlatformArg<?> platformArg, // Required
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit // Optional, default 20
    ) {
        return paginate( arrayDesignArgService.getExperiments( platformArg, limit.getValue(), offset.getValue() ), new String[] { "id" } );
    }

    /**
     * Retrieves the composite sequences (elements) for the given platform.
     *
     * @param platformArg can either be the ArrayDesign ID or its short name (e.g. "GPL1355" ). Retrieval by ID
     *                    is more efficient. Only platforms that user has access to will be available.
     * @param offset      optional parameter (defaults to 0) skips the specified amount of datasets when retrieving them
     *                    from the database.
     * @param limit       optional parameter (defaults to 20) limits the result to specified amount of datasets. Use 0
     */
    @GET
    @Path("/{platform}/elements")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the probes for a given platform")
    public PaginatedResponseDataObject<CompositeSequenceValueObject> getPlatformElements( // Params:
            @PathParam("platform") PlatformArg<?> platformArg, // Required
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit // Optional, default 20
    ) {
        return paginate( arrayDesignArgService.getElements( platformArg, limit.getValue(), offset.getValue() ), new String[] { "id" } );
    }

    /**
     * Retrieves composite sequences (elements) of the given platform.
     *
     * @param platformArg can either be the ArrayDesign ID or its short name (e.g. "GPL1355" ). Retrieval by ID
     *                    is more efficient. Only platforms that user has access to will be available.
     * @param probesArg   a list of identifiers, separated by commas (','). Identifiers can either be the
     *                    CompositeSequence ID or its name (e.g. AFFX_Rat_beta-actin_M_at).
     *                    <p>
     *                    Only elements on platforms that user has access to will be available.
     *                    </p>
     *                    <p>
     *                    Do not combine different identifiers in one query.
     *                    </p>
     */
    @GET
    @Path("/{platform}/elements/{probes}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the selected probes for a given platform")
    public FilteredAndPaginatedResponseDataObject<CompositeSequenceValueObject> getPlatformElement( // Params:
            @PathParam("platform") PlatformArg<?> platformArg, // Required
            @PathParam("probes") CompositeSequenceArrayArg probesArg, // Required
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit // Optional, default 20
    ) {
        probesArg.setPlatform( arrayDesignArgService.getEntity( platformArg ) );
        Filters filters = Filters.by( probesArg.getPlatformFilter() );
        return paginate( compositeSequenceService::loadValueObjects, filters, new String[] { "id" },
                compositeSequenceService.getSort( "id", Sort.Direction.ASC, Sort.NullMode.LAST ), offset.getValue(), limit.getValue() );
    }

    /**
     * Retrieves the genes on the given platform element.
     *
     * @param platformArg can either be the ArrayDesign ID or its short name (e.g. "GPL1355" ). Retrieval by ID
     *                    is more efficient. Only platforms that user has access to will be available.
     * @param probeArg    the name or ID of the platform element for which the genes should be retrieved. Note that
     *                    names containing
     *                    a forward slash are not accepted. Should you need this restriction temporarily lifted, please
     *                    contact us.
     * @param offset      optional parameter (defaults to 0) skips the specified amount of datasets when retrieving them
     *                    from the database.
     * @param limit       optional parameter (defaults to 20) limits the result to specified amount of datasets. Use 0
     *                    for no limit.
     */
    @GET
    @Path("/{platform}/elements/{probe}/genes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the genes associated to a probe in a given platform")
    public FilteredAndPaginatedResponseDataObject<GeneValueObject> getPlatformElementGenes( // Params:
            @PathParam("platform") PlatformArg<?> platformArg, // Required
            @PathParam("probe") CompositeSequenceArg<?> probeArg, // Required
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit // Optional, default 20
    ) {
        // FIXME: deal with potential null return value of loadValueObject
        return paginate( compositeSequenceService
                .getGenes( probeArgService.getEntityWithPlatform( probeArg, arrayDesignArgService.getEntity( platformArg ) ), offset.getValue(),
                        limit.getValue() )
                .map( geneService::loadValueObject ), probeArgService.getFilters( probeArg ), new String[] { "id" } );
    }

    /**
     * Retrieves the annotation file for the given platform.
     *
     * @param platformArg can either be the ArrayDesign ID or its short name (e.g. "GPL1355" ). Retrieval by ID
     *                    is more efficient. Only platforms that user has access to will be available.
     * @return the content of the annotation file of the given platform.
     */
    @GET
    @Path("/{platform}/annotations")
    @Produces(MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8)
    @Operation(summary = "Retrieve the annotations of a given platform",
            description = "The following columns are available: ElementName, GeneSymbols, GOTerms, GemmaIDs, NCBIids. Older files might still use ProbeName instead of ElementName.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(type = "string", format = "binary"),
                                    examples = { @ExampleObject("classpath:/restapidocs/examples/platform-annotations.tsv") }))
            })
    public Response getPlatformAnnotations( // Params:
            @PathParam("platform") PlatformArg<?> platformArg // Optional, default null
    ) {
        try {
            return outputAnnotationFile( arrayDesignArgService.getEntity( platformArg ) );
        } catch ( IOException e ) {
            throw new InternalServerErrorException( e );
        }
    }

    /**
     * Creates a response with the annotation file for given array design
     *
     * @param arrayDesign the platform to fetch and output the annotation file for.
     * @return a Response object containing the annotation file.
     */
    private Response outputAnnotationFile( ArrayDesign arrayDesign ) throws IOException {
        String fileName = arrayDesign.getShortName().replaceAll( Pattern.quote( "/" ), "_" )
                + ArrayDesignAnnotationService.STANDARD_FILE_SUFFIX
                + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX;
        File file = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + fileName );
        if ( !file.exists() ) {
            try {
                // generate it. This will cause a delay, and potentially a time-out, but better than a 404
                // To speed things up, we don't delete other files
                annotationFileService.create( arrayDesign, true, false ); // include GO by default.
                file = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + fileName );
                if ( !file.canRead() ) throw new IOException( "Annotation file created but cannot read?" );
            } catch ( IOException e ) {
                throw new NotFoundException( String.format( ERROR_ANNOTATION_FILE_NOT_AVAILABLE, arrayDesign.getShortName() ) );
            }
        }
        return Response.ok( new GZIPInputStream( new FileInputStream( file ) ) )
                .header( "Content-Encoding", "gzip" )
                .header( "Content-Disposition", "attachment; filename=" + FilenameUtils.removeExtension( file.getName() ) )
                .build();
    }

}

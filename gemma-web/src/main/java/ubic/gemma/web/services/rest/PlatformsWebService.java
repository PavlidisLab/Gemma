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
package ubic.gemma.web.services.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.web.services.rest.util.MediaTypeUtils;
import ubic.gemma.web.services.rest.util.PaginatedResponseDataObject;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.args.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * RESTful interface for platforms.
 *
 * @author tesarst
 */
@Service
@Path("/platforms")
public class PlatformsWebService {

    private static final Log log = LogFactory.getLog( PlatformsWebService.class.getName() );
    private static final String ERROR_ANNOTATION_FILE_NOT_AVAILABLE = "Annotation file for platform %s does not exist or can not be accessed.";

    private GeneService geneService;
    private ArrayDesignService arrayDesignService;
    private ExpressionExperimentService expressionExperimentService;
    private CompositeSequenceService compositeSequenceService;
    private ArrayDesignAnnotationService annotationFileService;

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
            ExpressionExperimentService expressionExperimentService,
            CompositeSequenceService compositeSequenceService, ArrayDesignAnnotationService annotationFileService ) {
        this.geneService = geneService;
        this.arrayDesignService = arrayDesignService;
        this.expressionExperimentService = expressionExperimentService;
        this.compositeSequenceService = compositeSequenceService;
        this.annotationFileService = annotationFileService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all platforms")
    public PaginatedResponseDataObject<ArrayDesignValueObject> getPlatforms( // Params:
            @QueryParam("filter") @DefaultValue("") FilterArg filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg sort // Optional, default +id
    ) {
        return Responder.paginate( arrayDesignService.loadValueObjectsPreFilter( filter.getObjectFilters( arrayDesignService ), sort.getSort( arrayDesignService ), offset.getValue(), limit.getValue() ) );
    }

    /**
     * Retrieves all datasets matching the given identifiers.
     *
     * @param datasetsArg a list of identifiers, separated by commas (','). Identifiers can either be the
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
    public PaginatedResponseDataObject<ArrayDesignValueObject> getPlatformsByIds( // Params:
            @PathParam("platform") PlatformArrayArg datasetsArg, // Optional
            @QueryParam("filter") @DefaultValue("") FilterArg filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg sort // Optional, default +id
    ) {
        Filters filters = filter.getObjectFilters( arrayDesignService );
        if ( filters == null ) {
            filters = new Filters();
        }

        filters.add( datasetsArg.getObjectFilters( arrayDesignService ) );

        return Responder.paginate( arrayDesignService.loadValueObjectsPreFilter( filters, sort.getSort( arrayDesignService ), offset.getValue(), limit.getValue() ) );
    }

    /**
     * Retrieves experiments in the given platform.
     *  @param platformArg can either be the ArrayDesign ID or its short name (e.g. "GPL1355" ). Retrieval by ID
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
        return Responder.paginate( platformArg
                .getExperiments( arrayDesignService, expressionExperimentService, limit.getValue(), offset.getValue() )
        );
    }

    /**
     * Retrieves the composite sequences (elements) for the given platform.
     *  @param platformArg can either be the ArrayDesign ID or its short name (e.g. "GPL1355" ). Retrieval by ID
     *                    is more efficient. Only platforms that user has access to will be available.
     * @param offset      optional parameter (defaults to 0) skips the specified amount of datasets when retrieving them
     *                    from the database.
     * @param limit       optional parameter (defaults to 20) limits the result to specified amount of datasets. Use 0
     */
    @GET
    @Path("/{platform}/elements")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the composite sequences for a given platform")
    public PaginatedResponseDataObject<CompositeSequenceValueObject> getPlatformElements( // Params:
            @PathParam("platform") PlatformArg<?> platformArg, // Required
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit // Optional, default 20
    ) {
        return Responder.paginate( platformArg
                .getElements( arrayDesignService, compositeSequenceService, limit.getValue(), offset.getValue() ) );
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
    @Operation(summary = "Retrieve the selected composite sequences for a given platform")
    public PaginatedResponseDataObject<CompositeSequenceValueObject> getPlatformElement( // Params:
            @PathParam("platform") PlatformArg<?> platformArg, // Required
            @PathParam("probes") CompositeSequenceArrayArg probesArg, // Required
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit // Optional, default 20
    ) {
        probesArg.setPlatform( platformArg.getEntity( arrayDesignService ) );
        return Responder.paginate( compositeSequenceService.loadValueObjectsPreFilter( Filters.singleFilter( probesArg.getPlatformFilter() ), null, offset.getValue(), limit.getValue() ) );
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
    public PaginatedResponseDataObject<GeneValueObject> getPlatformElementGenes( // Params:
            @PathParam("platform") PlatformArg<?> platformArg, // Required
            @PathParam("probe") CompositeSequenceArg<?> probeArg, // Required
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit // Optional, default 20
    ) {
        probeArg.setPlatform( platformArg.getEntity( arrayDesignService ) );
        return Responder.paginate( compositeSequenceService
                .getGenes( probeArg.getEntity( compositeSequenceService ), offset.getValue(),
                        limit.getValue() )
                .map( geneService::loadValueObject ) );
    }

    /**
     * Retrieves the annotation file for the given platform.
     *
     * @param  platformArg can either be the ArrayDesign ID or its short name (e.g. "GPL1355" ). Retrieval by ID
     *                     is more efficient. Only platforms that user has access to will be available.
     * @return the content of the annotation file of the given platform.
     */
    @GET
    @Path("/{platform}/annotations")
    @Produces(MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8)
    @Operation(summary = "Retrieve the annotations of a given platform", responses = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "string", format = "binary"))) })
    public Response getPlatformAnnotations( // Params:
            @PathParam("platform") PlatformArg<?> platformArg // Optional, default null
    ) {
        return outputAnnotationFile( platformArg.getEntity( arrayDesignService ) );
    }

    /**
     * Creates a response with the annotation file for given array design
     *
     * @param  arrayDesign the platform to fetch and output the annotation file for.
     * @return a Response object containing the annotation file.
     */
    private Response outputAnnotationFile( ArrayDesign arrayDesign ) {
        String fileName = arrayDesign.getShortName().replaceAll( Pattern.quote( "/" ), "_" )
                + ArrayDesignAnnotationService.STANDARD_FILE_SUFFIX
                + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX;
        File file = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + fileName );
        if ( !file.exists() ) {
            try {
                // generate it. This will cause a delay, and potentially a time-out, but better than a 404
                annotationFileService.create( arrayDesign, true );
                file = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + fileName );
                if ( !file.canRead() ) throw new IOException( "Annotation file created but cannot read?" );
            } catch ( IOException e ) {
                throw new NotFoundException( String.format( ERROR_ANNOTATION_FILE_NOT_AVAILABLE, arrayDesign.getShortName() ) );
            }
        }

        return Response.ok( file )
                .header( "Content-Encoding", "gzip" )
                .header( "Content-Disposition", "attachment; filename=" + FilenameUtils.removeExtension( file.getName() ) )
                .build();
    }

}

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

import com.sun.jersey.api.NotFoundException;
import org.hibernate.QueryException;
import org.jfree.util.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.WebService;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;
import ubic.gemma.web.services.rest.util.args.DatasetArg;
import ubic.gemma.web.services.rest.util.args.ExpressionExperimentFilterArg;
import ubic.gemma.web.services.rest.util.args.IntArg;
import ubic.gemma.web.services.rest.util.args.SortArg;
import ubic.gemma.web.util.EntityNotFoundException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * RESTful interface for datasets.
 *
 * @author tesarst
 */
@Service
@Path("/datasets")
public class DatasetsWebService extends WebService {

    private static final String ERROR_MSG_PROP_NOT_FOUND = "Datasets do not contain the given sort property.";
    private static final String ERROR_MSG_PROP_NOT_FOUND_DETAIL = "Property of name '%s' not recognized.";

    private ExpressionExperimentService expressionExperimentService;
    private ArrayDesignService arrayDesignService;
    private BioAssayService bioAssayService;

    /**
     * Required by spring
     */
    public DatasetsWebService() {
    }

    /**
     * Constructor for service autowiring
     */
    @Autowired
    public DatasetsWebService( ExpressionExperimentService expressionExperimentService,
            ArrayDesignService arrayDesignService, BioAssayService bioAssayService ) {
        this.expressionExperimentService = expressionExperimentService;
        this.arrayDesignService = arrayDesignService;
        this.bioAssayService = bioAssayService;
    }

    /**
     * Lists all datasets available in gemma.
     *
     * @param filter optional parameter (defaults to empty string) filters the result by given properties.
     *               <br/>
     *               <p>
     *               Filtering can be done on any property or nested property that the ExpressionExperiment class has (
     *               and is mapped by hibernate ). E.g: 'curationDetails' or 'curationDetails.lastTroubledEvent.date'
     *               </p><p>
     *               Accepted operator keywords are:
     *               <ul>
     *               <li> '=' - equality</li>
     *               <li> '!=' - non-equality</li>
     *               <li> '<' - smaller than</li>
     *               <li> '>' - larger than</li>
     *               <li> '<=' - smaller or equal</li>
     *               <li> '=>' - larger or equal</li>
     *               <li> 'like' - similar string, effectively means 'contains', translates to the sql 'LIKE' operator (given value will be surrounded by % signs)</li>
     *               </ul>
     *               Multiple filters can be chained using 'AND' or 'OR' keywords.<br/>
     *               Leave space between the keywords and the previous/next word! <br/>
     *               E.g: <code>?filter=property1 < value1 AND property2 ~ value2</code>
     *               </p><p>
     *               If chained filters are mixed conjunctions and disjunctions, the query must be in conjunctive normal
     *               form (CNF). Parentheses are not necessary - every AND keyword separates blocks of disjunctions.
     *               </p><p>
     *               Example:<br/>
     *               <code>?filter=p1 = v1 OR p1 != v2 AND p2 <= v2 AND p3 > v3 OR p3 < v4</code><br/>
     *               Above query will translate to: <br/>
     *               <code>(p1 = v1 OR p1 != v2) AND (p2 <= v2) AND (p3 > v3 OR p3 < v4;)</code>
     *               </p><p>
     *               Breaking the CNF results in an error.
     *               </p>
     *               <p>
     *               Filter "curationDetails.troubled" will be ignored if user is not an administrator.
     *               </p>
     *               <br/>
     * @param offset <p>optional parameter (defaults to 0) skips the specified amount of datasets when retrieving them from the database.</p>
     * @param limit  <p>optional parameter (defaults to 20) limits the result to specified amount of datasets. Use 0 for no limit.</p>
     * @param sort   <p>optional parameter (defaults to +id) sets the ordering property and direction.<br/>
     *               Format is [+,-][property name]. E.g. "-accession" will translate to descending ordering by the
     *               Accession property.<br/>
     *               Note that this does not guarantee the order of the returned entities.<br/>
     *               Nested properties are also supported (recursively). E.g. "+curationDetails.lastTroubledEvent.date".<br/></p>
     * @return all datasets in the database, skipping the first [{@code offset}] of datasets, and limiting the amount in the result to
     * the value of the {@code limit} parameter. If the {@code accessionGsmId} parameter is non-null, will limit the result to datasets
     * with specified accession. Note that if the accession GSM id is not valid or no datasets with it are found, a 404 response will be
     * supplied instead.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject all( // Params:
            @QueryParam("filter") @DefaultValue("") ExpressionExperimentFilterArg filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") IntArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") IntArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg sort, // Optional, default +id
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        try {
            return Responder.autoCode( expressionExperimentService
                    .loadValueObjectsPreFilter( offset.getValue(), limit.getValue(), sort.getField(), sort.isAsc(),
                            filter.getObjectFilters() ), sr );
        } catch ( QueryException e ) {

            if ( log.isDebugEnabled() ) {
                e.printStackTrace();
            }

            WellComposedErrorBody error = new WellComposedErrorBody( Response.Status.BAD_REQUEST,
                    ERROR_MSG_PROP_NOT_FOUND );
            WellComposedErrorBody.addExceptionFields( error,
                    new EntityNotFoundException( String.format( ERROR_MSG_PROP_NOT_FOUND_DETAIL, sort.getField() ) ) );

            return Responder.code( error.getStatus(), error, sr );
        }
    }

    /**
     * Retrieves single dataset based on the given identifier.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{datasetArg: [a-zA-Z0-9\\.]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject dataset( // Params:
            @PathParam("datasetArg") DatasetArg<Object> datasetArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        Object response = datasetArg.getValueObject( expressionExperimentService );
        return this.autoCodeResponse( datasetArg, response, sr );
    }

    /**
     * Retrieves platforms for the given experiment
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{datasetArg: [a-zA-Z0-9\\.]+}/platforms")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject datasetPlatforms( // Params:
            @PathParam("datasetArg") DatasetArg<Object> datasetArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        Object response = datasetArg.getPlatforms( expressionExperimentService, arrayDesignService );
        return this.autoCodeResponse( datasetArg, response, sr );
    }

    /**
     * Retrieves the samples for given experiment
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{datasetArg: [a-zA-Z0-9\\.]+}/samples")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public ResponseDataObject datasetSamples( // Params:
            @PathParam("datasetArg") DatasetArg<Object> datasetArg, // Required
            @Context final HttpServletResponse sr // The servlet response, needed for response code setting.
    ) {
        Object response = datasetArg.getSamples( expressionExperimentService, bioAssayService );
        return this.autoCodeResponse( datasetArg, response, sr );
    }


    /* ********************************
     * OLD METHODS
     * TODO REMOVE
     * ********************************/

    @Deprecated
    @GET
    @Path("/findByAccession/includeConstantFactorsStructured/{gsmId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Map<String, Map<String, String>>> getAnnotationsByGSMIncludeTagsStructured(
            @PathParam("gsmId") String gsmId ) {

        Collection<BioAssay> foundBioAssays = this.bioAssayService.findByAccession( gsmId );

        if ( foundBioAssays.isEmpty() )
            throw new NotFoundException( "Sample not found." );

        Collection<Characteristic> characteristics = new HashSet<>();
        if ( foundBioAssays.size() == 1 ) {
            ExpressionExperiment ee = this.expressionExperimentService
                    .findByBioAssay( foundBioAssays.iterator().next() );
            if ( ee != null ) {
                characteristics.addAll( ee.getCharacteristics() );
            }
        }

        return prepareEEAnnotationsStructured( foundBioAssays, characteristics );
    }

    @Deprecated
    @GET
    @Path("/findByAccession/includeConstantFactors/{gsmId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Map<String, String>> getAnnotationsByGSMIncludeTagsUnstructured(
            @PathParam("gsmId") String gsmId ) {

        Collection<BioAssay> foundBioAssays = this.bioAssayService.findByAccession( gsmId );

        if ( foundBioAssays.isEmpty() )
            throw new NotFoundException( "Sample not found." );

        Collection<Characteristic> characteristics = new HashSet<>();
        if ( foundBioAssays.size() == 1 ) {
            ExpressionExperiment ee = this.expressionExperimentService
                    .findByBioAssay( foundBioAssays.iterator().next() );
            if ( ee != null ) {
                characteristics.addAll( ee.getCharacteristics() );
            }
        }

        return prepareEEAnnotationsUnstructured( foundBioAssays, characteristics );
    }

    @Deprecated
    @GET
    @Path("/findByShortName/includeConstantFactorsStructured/{shortName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Map<String, Map<String, String>>> getAnnotationsIncludeTagsStructured(
            @PathParam("shortName") String shortName ) {

        ExpressionExperiment experiment = this.expressionExperimentService.findByShortName( shortName );
        if ( experiment == null )
            throw new NotFoundException( "Dataset not found." );
        Collection<BioAssay> bioAssays = experiment.getBioAssays();
        Collection<Characteristic> chars = experiment.getCharacteristics();

        return prepareEEAnnotationsStructured( bioAssays, chars );
    }

    @Deprecated
    @GET
    @Path("/findByShortName/includeConstantFactors/{shortName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Map<String, String>> getAnnotationsIncludeTagsUnstructured(
            @PathParam("shortName") String shortName ) {

        ExpressionExperiment experiment = this.expressionExperimentService.findByShortName( shortName );
        if ( experiment == null )
            throw new NotFoundException( "Dataset not found." );
        Collection<BioAssay> bioAssays = experiment.getBioAssays();
        Collection<Characteristic> chars = experiment.getCharacteristics();

        return prepareEEAnnotationsUnstructured( bioAssays, chars );
    }

    @Deprecated
    private String[] getTagString( Characteristic characteristic ) {

        String[] arr = { "", "" };
        if ( characteristic == null )
            return arr;
        if ( ( characteristic.getCategory() == null || characteristic.getCategory().isEmpty() ) && (
                characteristic.getValue() == null || characteristic.getValue().isEmpty() ) ) {
            return arr;
        } else if ( characteristic.getCategory() == null || characteristic.getCategory().isEmpty() ) {
            arr[0] = characteristic.getValue();
            arr[1] = characteristic.getValue();
        } else if ( characteristic.getValue() == null || characteristic.getValue().isEmpty() ) {
            arr[0] = characteristic.getCategory();
            arr[1] = "no value";
        } else {
            arr[0] = characteristic.getCategory();
            arr[1] = characteristic.getValue();
        }
        return arr;
    }

    @Deprecated
    private Map<String, Map<String, Map<String, String>>> prepareEEAnnotationsStructured(
            Collection<BioAssay> bioAssays, Collection<Characteristic> characteristics ) {
        Map<String, Map<String, Map<String, String>>> result = new HashMap<>();

        if ( bioAssays.isEmpty() )
            throw new NotFoundException( "BioAssays not found" );
        for ( BioAssay bioAssay : bioAssays ) {

            String accession = bioAssay.getAccession().getAccession();

            Map<String, String> annotations = new HashMap<>();
            Map<String, String> tagAnnotations = new HashMap<>();
            Map<String, Map<String, String>> annotationsCategories = new HashMap<>();

            BioMaterial bioMaterial = bioAssay.getSampleUsed();

            for ( FactorValue factorValue : bioMaterial.getFactorValues() ) {
                if ( !factorValue.getExperimentalFactor().getName()
                        .equals( ExperimentalFactorService.BATCH_FACTOR_NAME ) ) {
                    annotations
                            .put( factorValue.getExperimentalFactor().getName(), factorValue.getDescriptiveString() );
                }
            }

            for ( Characteristic characteristic : characteristics ) {

                String[] tagStringArr = getTagString( characteristic );
                if ( !tagStringArr[0].isEmpty() && !tagStringArr[1].isEmpty() ) {
                    tagAnnotations.put( tagStringArr[0], tagStringArr[1] );
                }
            }

            annotationsCategories.put( "ExperimentFactors", annotations );
            annotationsCategories.put( "ExperimentTags", tagAnnotations );
            result.put( accession, annotationsCategories );
        }

        return result;
    }

    /**
     * Don't introduce structure to separate experimental factors from experiment tags, instead add a prefix to tag
     * categories
     */
    @Deprecated
    private Map<String, Map<String, String>> prepareEEAnnotationsUnstructured( Collection<BioAssay> bioAssays,
            Collection<Characteristic> characteristics ) {
        Map<String, Map<String, String>> result = new HashMap<>();

        if ( bioAssays.isEmpty() )
            throw new NotFoundException( "BioAssays not found" );
        for ( BioAssay bioAssay : bioAssays ) {

            String accession = bioAssay.getAccession().getAccession();
            Map<String, String> annotations = new HashMap<>();
            BioMaterial bioMaterial = bioAssay.getSampleUsed();

            for ( FactorValue factorValue : bioMaterial.getFactorValues() ) {
                if ( !factorValue.getExperimentalFactor().getName()
                        .equals( ExperimentalFactorService.BATCH_FACTOR_NAME ) ) {
                    annotations
                            .put( factorValue.getExperimentalFactor().getName(), factorValue.getDescriptiveString() );
                }
            }

            for ( Characteristic characteristic : characteristics ) {

                String[] tagStringArr = getTagString( characteristic );
                if ( !tagStringArr[0].isEmpty() && !tagStringArr[1].isEmpty() ) {
                    annotations.put( "constant_" + tagStringArr[0], tagStringArr[1] );
                }
            }
            result.put( accession, annotations );
        }

        return result;
    }

}

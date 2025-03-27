package ubic.gemma.rest;

import io.swagger.v3.oas.annotations.Operation;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.DatasetArg;
import ubic.gemma.rest.util.args.DatasetArgService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.rest.util.Responders.respond;

/**
 * Expose dataset subsets to the REST API.
 * @author poirigui
 */
@Service
@Path("/datasets/{dataset}")
public class DatasetSubSetsWebService {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private DatasetArgService datasetArgService;

    /**
     * Retrieve all the "groups" of subsets of a dataset.
     * <p>
     * Each group of subsets is logically organized by a {@link BioAssayDimension} that holds its assays. We don't
     * expose that aspect however, and simply use the ID of the BAD as ID of the group.
     */
    @GET
    @Path("/subSetGroups")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Obtain all the subset groups of a dataset")
    public ResponseDataObject<List<ExpressionExperimentSubSetGroupValueObject>> getAllDatasetsSubSets(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        return respond( expressionExperimentService.getSubSetsByDimension( ee )
                .entrySet()
                .stream()
                .map( e -> {
                    Map<ExperimentalFactor, Map<FactorValue, ExpressionExperimentSubSet>> ssvs = expressionExperimentService.getSubSetsByFactorValue( ee, e.getKey() );
                    return createSubSetGroup( e.getKey(), e.getValue(), ssvs );
                } )
                .collect( Collectors.toList() ) );
    }

    @GET
    @Path("/subSetGroups/{subSetGroup}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Obtain a specific subset group of a dataset")
    public ResponseDataObject<ExpressionExperimentSubSetGroupValueObject> getDatasetsSubSetGroup(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("subSetGroup") Long bioAssayDimensionId
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        // this is preferred, because it does not require any data to be present
        BioAssayDimension bad = expressionExperimentService.getBioAssayDimensionById( ee, bioAssayDimensionId );
        if ( bad == null ) {
            throw new NotFoundException( "No subset group with ID " + bioAssayDimensionId );
        }
        Map<ExperimentalFactor, Map<FactorValue, ExpressionExperimentSubSet>> ssvs = expressionExperimentService.getSubSetsByFactorValue( ee, bad );
        return respond( createSubSetGroup( bad, expressionExperimentService.getSubSets( ee, bad ), ssvs ) );
    }

    private ExpressionExperimentSubSetGroupValueObject createSubSetGroup( BioAssayDimension bad, Collection<ExpressionExperimentSubSet> subsets, Map<ExperimentalFactor, Map<FactorValue, ExpressionExperimentSubSet>> ssvs ) {
        Map<ExpressionExperimentSubSet, Set<FactorValue>> fvs = new HashMap<>();
        ssvs.forEach( ( ef, s2fv ) -> {
            s2fv.forEach( ( fv, s ) -> {
                fvs.computeIfAbsent( s, k -> new HashSet<>() ).add( fv );
            } );
        } );
        List<ExpressionExperimentSubsetWithFactorValuesObject> ssvos = subsets.stream()
                // TODO order the subsets by how they appear in the BioAssayDimension
                .sorted( Comparator.comparing( ExpressionExperimentSubSet::getName ) )
                .map( subset -> new ExpressionExperimentSubsetWithFactorValuesObject( subset, fvs.get( subset ) ) )
                .collect( Collectors.toList() );
        return new ExpressionExperimentSubSetGroupValueObject( bad, ssvos );
    }

    @GET
    @Path("/subSets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Obtain all subsets of a dataset")
    public ResponseDataObject<List<ExpressionExperimentSubsetValueObject>> getDatasetsSubSets(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        return respond( datasetArgService.getSubSets( datasetArg ).stream()
                .map( ExpressionExperimentSubsetValueObject::new )
                .collect( Collectors.toList() ) );
    }

    @GET
    @Path("/subSets/{subSet}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Obtain a specific subset of a dataset")
    public ResponseDataObject<ExpressionExperimentSubsetValueObject> getDatasetsSubSetById(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("subSet") Long subSetId
    ) {
        return respond( new ExpressionExperimentSubsetValueObject( datasetArgService.getSubSet( datasetArg, subSetId ) ) );
    }

    @GET
    @Path("/subSets/{subSet}/samples")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Obtain the samples of a specific subset of a dataset")
    public ResponseDataObject<List<BioAssayValueObject>> getDatasetsSubSetSamples(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("subSet") Long subSetId
    ) {
        return respond( datasetArgService.getSubSetSamples( datasetArg, subSetId ) );
    }

    /**
     * A group of subsets, logically organized by a {@link BioAssayDimension}.
     * @author poirigui
     */
    @Getter
    public static class ExpressionExperimentSubSetGroupValueObject {

        private final Long id;

        private final String name;

        private final List<ExpressionExperimentSubsetWithFactorValuesObject> subSets;

        public ExpressionExperimentSubSetGroupValueObject( BioAssayDimension bioAssayDimension, List<ExpressionExperimentSubsetWithFactorValuesObject> subSets ) {
            this.id = bioAssayDimension.getId();
            // FIXME: make the name generation more robust, it's only tailored to how we name single-cell subsets
            this.name = StringUtils.removeEnd( StringUtils.getCommonPrefix( subSets.stream().map( ExpressionExperimentSubsetValueObject::getName ).toArray( String[]::new ) ), " - " );
            this.subSets = subSets;
        }
    }

    @Getter
    public static class ExpressionExperimentSubsetWithFactorValuesObject extends ExpressionExperimentSubsetValueObject {

        private final List<FactorValueBasicValueObject> factorValues;

        public ExpressionExperimentSubsetWithFactorValuesObject( ExpressionExperimentSubSet subset, Set<FactorValue> factorValues ) {
            super( subset );
            this.factorValues = factorValues.stream()
                    .map( FactorValueBasicValueObject::new )
                    .collect( Collectors.toList() );
        }
    }
}

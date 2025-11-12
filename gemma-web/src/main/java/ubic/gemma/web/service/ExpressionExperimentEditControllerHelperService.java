package ubic.gemma.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentEditController;
import ubic.gemma.web.controller.util.EntityNotFoundException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExpressionExperimentEditControllerHelperService {

    private static final List<String>
            STANDARD_QUANTITATION_TYPES = Arrays.stream( StandardQuantitationType.values() ).map( Enum::name ).sorted().collect( Collectors.toList() ),
            SCALE_TYPES = Arrays.stream( ScaleType.values() ).map( Enum::name ).sorted().collect( Collectors.toList() ),
            GENERAL_QUANTITATION_TYPES = Arrays.stream( GeneralType.values() ).map( Enum::name ).sorted().collect( Collectors.toList() ),
            REPRESENTATIONS = Arrays.stream( PrimitiveType.values() ).map( Enum::name ).sorted().collect( Collectors.toList() );

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Transactional(readOnly = true)
    public Map<String, ?> getFormObjectAndReferenceDataAndKeywordsById( Long id ) {
        ExpressionExperiment ee = expressionExperimentService.loadAndThawLiteOrFail( id, EntityNotFoundException::new,
                "No experiment with ID " + id );
        Map<String, Object> referenceData = new HashMap<>();
        referenceData.put( "expressionExperiment", getFormObject( ee ) );
        referenceData.putAll( getReferenceDataAndKeywords( ee ) );
        return referenceData;
    }

    @Transactional(readOnly = true)
    public Map<String, ?> getFormObjectAndReferenceDataAndKeywords( ExpressionExperiment ee ) {
        Map<String, Object> referenceData = new HashMap<>();
        referenceData.put( "expressionExperiment", getFormObject( ee ) );
        referenceData.putAll( getReferenceDataAndKeywords( ee ) );
        return referenceData;
    }

    @Transactional(readOnly = true)
    public Map<String, ?> getReferenceDataAndKeywords( ExpressionExperiment ee ) {
        Map<String, Object> referenceData = new HashMap<>( getReferenceData() );
        referenceData.put( "keywords", getKeywords( ee ) );
        return referenceData;
    }

    private Map<String, ?> getReferenceData() {
        Map<String, Object> referenceData = new HashMap<>();
        referenceData.put( "standardQuantitationTypes", new ArrayList<>( STANDARD_QUANTITATION_TYPES ) );
        referenceData.put( "scaleTypes", new ArrayList<>( SCALE_TYPES ) );
        referenceData.put( "generalQuantitationTypes", new ArrayList<>( GENERAL_QUANTITATION_TYPES ) );
        referenceData.put( "representations", new ArrayList<>( REPRESENTATIONS ) );
        referenceData.put( "cellTypeAssignmentProtocols", singleCellExpressionExperimentService.getCellTypeAssignmentProtocols() );
        return referenceData;
    }

    private String getKeywords( ExpressionExperiment ee ) {
        return expressionExperimentService.getAnnotations( ee ).stream()
                .map( AnnotationValueObject::getTermName )
                .collect( Collectors.joining( "," ) );
    }

    @Transactional(readOnly = true)
    public ExpressionExperimentEditController.ExpressionExperimentEditForm getFormObject( ExpressionExperiment ee ) {
        ExpressionExperimentEditController.ExpressionExperimentEditForm obj = new ExpressionExperimentEditController.ExpressionExperimentEditForm();
        populateForm( obj, ee );
        LinkedHashMap<Class<? extends DataVector>, List<ExpressionExperimentEditController.QuantitationTypeEditForm>> qtf = getQuantitationTypesByVectorType( ee );
        List<ExpressionExperimentEditController.QuantitationTypeEditForm> qtfL = qtf.values().stream()
                .flatMap( Collection::stream )
                .collect( Collectors.toList() );
        obj.setQuantitationTypes( qtfL );
        obj.setQuantitationTypesByVectorType( qtf );
        return obj;
    }

    @Transactional(readOnly = true)
    public void populateForm( ExpressionExperimentEditController.ExpressionExperimentEditForm form, ExpressionExperiment expressionExperiment ) {
        form.setId( expressionExperiment.getId() );
        form.setShortName( expressionExperiment.getShortName() );
        form.setName( expressionExperiment.getName() );
        form.setDescription( expressionExperiment.getDescription() );
        form.setBioAssays( convert2ValueObjects( expressionExperiment.getBioAssays() ) );
        SingleCellExpressionExperimentService.SingleCellDimensionInitializationConfig initconfig = SingleCellExpressionExperimentService.SingleCellDimensionInitializationConfig.builder()
                .includeCtas( true )
                .includeClcs( true )
                .includeProtocol( true )
                .includeCharacteristics( true )
                .build();
        Map<SingleCellDimension, Set<QuantitationType>> dim2qts = singleCellExpressionExperimentService.getSingleCellQuantitationTypesBySingleCellDimensionWithoutCellIds( expressionExperiment,
                // minimal config, we only care about the mapping keys
                SingleCellExpressionExperimentService.SingleCellDimensionInitializationConfig.builder().build() );
        List<SingleCellDimension> scds = singleCellExpressionExperimentService.getSingleCellDimensionsWithoutCellIds( expressionExperiment, initconfig );
        form.setSingleCellDimensions( scds.stream().map( scd -> new ExpressionExperimentEditController.SingleCellDimensionEditForm( scd, dim2qts.getOrDefault( scd, Collections.emptySet() ) ) ).collect( Collectors.toList() ) );
    }

    private Collection<BioAssayValueObject> convert2ValueObjects( Collection<BioAssay> bioAssays ) {
        Collection<BioAssayValueObject> result = new HashSet<>();
        for ( BioAssay bioAssay : bioAssays ) {
            result.add( new BioAssayValueObject( bioAssay, false ) );
        }
        return result;
    }

    /**
     * Organize all the QTs for the given experiment by the vector type they apply to.
     */
    private LinkedHashMap<Class<? extends DataVector>, List<ExpressionExperimentEditController.QuantitationTypeEditForm>> getQuantitationTypesByVectorType( ExpressionExperiment ee ) {
        // sort the mapping
        return expressionExperimentService.getQuantitationTypesByVectorType( ee ).entrySet().stream()
                .sorted( Map.Entry.comparingByKey( Comparator.comparing( Class::getSimpleName, Comparator.nullsLast( Comparator.naturalOrder() ) ) ) )
                .collect( Collectors.toMap( Map.Entry::getKey,
                        v -> v.getValue().stream().sorted( Comparator.comparing( QuantitationType::getName ) ).map( ExpressionExperimentEditController.QuantitationTypeEditForm::new ).collect( Collectors.toList() ),
                        ( a, b ) -> b,
                        LinkedHashMap::new ) );
    }
}

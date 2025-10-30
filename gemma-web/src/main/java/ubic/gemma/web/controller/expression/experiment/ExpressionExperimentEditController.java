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
package ubic.gemma.web.controller.expression.experiment;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.core.analysis.preprocess.PreprocessingException;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.model.common.auditAndSecurity.eventType.BioMaterialMappingUpdate;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.persistence.util.IdentifiableUtils;
import ubic.gemma.web.controller.util.EntityNotFoundException;
import ubic.gemma.web.controller.util.MessageUtil;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Handle advanced editing of expression experiments.
 *
 * @author keshav
 */
@Controller
@CommonsLog
public class ExpressionExperimentEditController {

    private static final List<String>
            STANDARD_QUANTITATION_TYPES = Arrays.stream( StandardQuantitationType.values() ).map( Enum::name ).sorted().collect( Collectors.toList() ),
            SCALE_TYPES = Arrays.stream( ScaleType.values() ).map( Enum::name ).sorted().collect( Collectors.toList() ),
            GENERAL_QUANTITATION_TYPES = Arrays.stream( GeneralType.values() ).map( Enum::name ).sorted().collect( Collectors.toList() ),
            REPRESENTATIONS = Arrays.stream( PrimitiveType.values() ).map( Enum::name ).sorted().collect( Collectors.toList() );

    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;
    @Autowired
    private PreprocessorService preprocessorService;
    @Autowired
    private BioAssayService bioAssayService;
    @Autowired
    private BioMaterialService bioMaterialService;
    @Autowired
    private AuditTrailService auditTrailService;
    @Autowired
    private Persister persisterHelper;
    @Autowired
    protected MessageSource messageSource;
    @Autowired
    protected MessageUtil messageUtil;

    @Autowired
    private TaskExecutor taskExecutor;

    @Data
    public static class ExpressionExperimentEditForm {
        private Long id;
        private String shortName;
        private String name;
        private String description;
        @Nullable
        private List<QuantitationTypeEditForm> quantitationTypes;
        /**
         * {@link #quantitationTypes}, organized by the type of data vector they apply to and in the same order.
         */
        private Map<Class<? extends DataVector>, List<QuantitationTypeEditForm>> quantitationTypesByVectorType;
        private List<SingleCellDimensionEditForm> singleCellDimensions;
        private Collection<BioAssayValueObject> bioAssays;
        @Nullable
        private String assayToMaterialMap;
        /**
         * Return to the experiment page after a successful update.
         * <p>
         * The default is to stay on the edit page.
         */
        private boolean returnToExperiment;
    }

    @Data
    @NoArgsConstructor
    public static class SingleCellDimensionEditForm {
        private Long id;
        private List<CellTypeAssignmentEditForm> cellTypeAssignments;
        private List<CellLevelCharacteristicsEditForm> cellLevelCharacteristics;

        public SingleCellDimensionEditForm( SingleCellDimension scd ) {
            this.id = scd.getId();
            this.cellTypeAssignments = scd.getCellTypeAssignments().stream().map( CellTypeAssignmentEditForm::new ).collect( Collectors.toList() );
            this.cellLevelCharacteristics = scd.getCellLevelCharacteristics().stream().map( CellLevelCharacteristicsEditForm::new ).collect( Collectors.toList() );
        }
    }

    @Data
    @NoArgsConstructor
    public static class CellTypeAssignmentEditForm {
        private Long id;
        private String name;
        private String description;
        private Long protocolId;
        private boolean isPreferred;
        private List<String> values;

        public CellTypeAssignmentEditForm( CellTypeAssignment cta ) {
            setId( cta.getId() );
            setName( cta.getName() );
            setDescription( cta.getDescription() );
            if ( cta.getProtocol() != null ) {
                setProtocolId( cta.getProtocol().getId() );
            }
            setIsPreferred( cta.isPreferred() );
            setValues( cta.getCellTypes().stream().map( Characteristic::getValue ).sorted().collect( Collectors.toList() ) );
        }

        public boolean getIsPreferred() {
            return isPreferred;
        }

        public void setIsPreferred( boolean isPreferred ) {
            this.isPreferred = isPreferred;
        }
    }

    @Data
    @NoArgsConstructor
    public static class CellLevelCharacteristicsEditForm {
        private Long id;
        private String name;
        private String description;
        private String category;
        private List<String> values;

        public CellLevelCharacteristicsEditForm( CellLevelCharacteristics clc ) {
            this.id = clc.getId();
            this.name = clc.getName();
            this.description = clc.getDescription();
            clc.getCharacteristics().stream().findFirst().ifPresent( c -> setCategory( c.getCategory() ) );
            this.values = clc.getCharacteristics().stream().map( Characteristic::getValue ).sorted().collect( Collectors.toList() );
        }
    }

    @Data
    @NoArgsConstructor
    public static class QuantitationTypeEditForm {
        private Long id;
        private String name;
        private String description;
        private String generalType;
        private String type;
        private String scale;
        /**
         * This is disabled by default in the UI because we don't want users to mess with the representation accidentally.
         */
        @Nullable
        private String representation;
        @Nullable
        private Boolean isPreferred;
        @Nullable
        @Deprecated
        private Boolean isMaskedPreferred;
        @Nullable
        private Boolean isSingleCellPreferred;
        // all required, Boolean is used for name consistency
        private boolean isBackground;
        private boolean isBackgroundSubtracted;
        private boolean isNormalized;
        private boolean isBatchCorrected;
        private boolean isRatio;
        private boolean isRecomputedFromRawData;

        public QuantitationTypeEditForm( QuantitationType qt ) {
            setId( qt.getId() );
            setName( qt.getName() );
            setDescription( qt.getDescription() );
            setGeneralType( qt.getGeneralType().name() );
            setType( qt.getType().name() );
            setScale( qt.getScale().name() );
            setRepresentation( qt.getRepresentation().name() );
            setIsPreferred( qt.getIsPreferred() );
            //noinspection deprecation
            setIsMaskedPreferred( qt.getIsMaskedPreferred() );
            setIsSingleCellPreferred( qt.getIsSingleCellPreferred() );
            setIsBackground( qt.getIsBackground() );
            setIsBackgroundSubtracted( qt.getIsBackgroundSubtracted() );
            setIsNormalized( qt.getIsNormalized() );
            setIsBatchCorrected( qt.getIsBatchCorrected() );
            setIsRatio( qt.getIsRatio() );
            setIsRecomputedFromRawData( qt.getIsRecomputedFromRawData() );
        }

        public boolean getIsBackground() {
            return isBackground;
        }

        public void setIsBackground( boolean isBackground ) {
            this.isBackground = isBackground;
        }

        public boolean getIsBackgroundSubtracted() {
            return isBackgroundSubtracted;
        }

        public void setIsBackgroundSubtracted( boolean isBackgroundSubtracted ) {
            this.isBackgroundSubtracted = isBackgroundSubtracted;
        }

        public boolean getIsNormalized() {
            return isNormalized;
        }

        public void setIsNormalized( boolean isNormalized ) {
            this.isNormalized = isNormalized;
        }

        public boolean getIsBatchCorrected() {
            return isBatchCorrected;
        }

        public void setIsBatchCorrected( boolean isBatchCorrected ) {
            this.isBatchCorrected = isBatchCorrected;
        }

        public boolean getIsRatio() {
            return isRatio;
        }

        public void setIsRatio( boolean isRatio ) {
            this.isRatio = isRatio;
        }

        public boolean getIsRecomputedFromRawData() {
            return isRecomputedFromRawData;
        }

        public void setIsRecomputedFromRawData( boolean isRecomputedFromRawData ) {
            this.isRecomputedFromRawData = isRecomputedFromRawData;
        }
    }

    @RequestMapping(value = "/expressionExperiment/editExpressionExperiment.html", method = RequestMethod.GET)
    public ModelAndView getExpressionExperimentEditPage( @RequestParam("id") Long id ) {
        ExpressionExperiment ee = expressionExperimentService.loadAndThawLiteOrFail( id, EntityNotFoundException::new,
                "No experiment with ID " + id );
        return new ModelAndView( "expressionExperiment.edit" )
                .addObject( "expressionExperiment", getFormObject( ee ) )
                .addObject( "keywords", getKeywords( ee ) )
                .addAllObjects( getReferenceData() );
    }

    @RequestMapping(value = "/expressionExperiment/editExpressionExperiment.html", method = RequestMethod.POST)
    public ModelAndView updateExpressionExperiment( @RequestParam("id") Long id,
            @ModelAttribute("expressionExperiment") ExpressionExperimentEditForm form, BindingResult bindingResult,
            HttpServletResponse response ) {
        ExpressionExperiment expressionExperiment = expressionExperimentService.loadAndThawLiteOrFail( id,
                EntityNotFoundException::new, String.format( "No experiment with ID %d", id ) );

        // the backend only submits quantitationTypes and assayToMaterialMap, so we need to populate the remaining fields
        populateForm( form, expressionExperiment );

        // FIXME: the update can alter properties affecting hashCode(), so an hash set is unsuitable here
        Set<QuantitationType> preferredSingleCellQuantitationTypes = new TreeSet<>( Comparator.comparing( QuantitationType::getId ) );
        Set<QuantitationType> preferredQuantitationTypes = new TreeSet<>( Comparator.comparing( QuantitationType::getId ) );
        Map<Long, Class<? extends DataVector>> qtbv = new LinkedHashMap<>();
        if ( form.getQuantitationTypes() != null ) {
            for ( Entry<Class<? extends DataVector>, Set<QuantitationType>> entry : expressionExperimentService
                    .getQuantitationTypesByVectorType( expressionExperiment ).entrySet() ) {
                Class<? extends DataVector> key = entry.getKey();
                Set<QuantitationType> v = entry.getValue();
                for ( QuantitationType qt : v ) {
                    qtbv.put( qt.getId(), key );
                    if ( qt.getIsPreferred() ) {
                        preferredQuantitationTypes.add( qt );
                    }
                    if ( qt.getIsSingleCellPreferred() ) {
                        preferredSingleCellQuantitationTypes.add( qt );
                    }
                }
            }
            Map<Class<? extends DataVector>, List<QuantitationTypeEditForm>> qtf = new LinkedHashMap<>();
            for ( QuantitationTypeEditForm qt : form.getQuantitationTypes() ) {
                qtf.computeIfAbsent( qtbv.get( qt.getId() ), k -> new ArrayList<>() )
                        .add( qt );
            }
            form.setQuantitationTypesByVectorType( qtf );
        }

        ValidationUtils.invokeValidator( new ExpressionExperimentEditFormValidator( expressionExperiment, qtbv ), form, bindingResult );
        if ( bindingResult.hasErrors() ) {
            response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
            return new ModelAndView( "expressionExperiment.edit" )
                    .addObject( "expressionExperiment", form )
                    .addAllObjects( getReferenceData() );
        }

        // fetch the previous CTA in case the preferred single-cell QT changes
        Optional<CellTypeAssignment> previousCta = singleCellExpressionExperimentService.getPreferredCellTypeAssignmentWithoutIndices( expressionExperiment );

        boolean reprocess = false;
        boolean recomputeSingleCellSparsityMetrics = false;
        boolean recreateCellTypeFactor = false;
        if ( form.getQuantitationTypes() != null ) {
            Map<QuantitationType, QuantitationTypeUpdateStatus> status = updateQuantitationTypes( expressionExperiment, form.getQuantitationTypes() );
            for ( Entry<QuantitationType, QuantitationTypeUpdateStatus> entry : status.entrySet() ) {
                if ( entry.getValue() != QuantitationTypeUpdateStatus.SIGNIFICANT ) {
                    continue;
                }
                if ( entry.getKey().getIsPreferred() ) {
                    this.messageUtil.saveMessage( "Preferred raw quantitation type has been significantly changed, reprocessing will be performed." );
                    reprocess = true;
                } else if ( preferredQuantitationTypes.contains( entry.getKey() )
                        && expressionExperiment.getQuantitationTypes().stream().noneMatch( QuantitationType::getIsPreferred ) ) {
                    // FIXME: remove processed vectors when there are no preferred QTs
                    this.messageUtil.saveMessage( "There is no preferred quantitation type, however existing processed data will be kept." );
                }
                if ( entry.getKey().getIsSingleCellPreferred() ) {
                    recomputeSingleCellSparsityMetrics = true;
                    // recreate the cell type factor only if the new preferred QT has a preferred CTA that differs from
                    // the previous preferred CTA; the previous CTA might be missing.
                    Optional<CellTypeAssignment> newCta = singleCellExpressionExperimentService.getPreferredCellTypeAssignmentWithoutIndices( expressionExperiment, entry.getKey() );
                    recreateCellTypeFactor = newCta.isPresent() && !newCta.equals( previousCta );
                    this.messageUtil.saveMessage( String.format( "Preferred single-cell quantitation type has been significantly changed, single-cell sparsity metrics will be recomputed%s.",
                            recreateCellTypeFactor ? " and the cell type factor will be re-created based on " + newCta.get() : "" ) );
                } else if ( preferredSingleCellQuantitationTypes.contains( entry.getKey() )
                        && expressionExperiment.getQuantitationTypes().stream().noneMatch( QuantitationType::getIsSingleCellPreferred ) ) {
                    // sparsity metrics will be cleared if there are no other preferred SC QTs
                    this.messageUtil.saveMessage( "There is no preferred single-cell quantitation type, single-cell sparsity metrics will be cleared." );
                    recomputeSingleCellSparsityMetrics = true;
                }
            }
        }

        if ( form.getAssayToMaterialMap() != null ) {
            if ( updateBioMaterialMap( expressionExperiment, form.getAssayToMaterialMap() ) ) {
                this.messageUtil.saveMessage( "Assay to sample associations have been changed; reprocessing will be performed." );
                reprocess = true;
            }
        }

        if ( recreateCellTypeFactor ) {
            singleCellExpressionExperimentService.recreateCellTypeFactor( expressionExperiment );
        }

        if ( recomputeSingleCellSparsityMetrics ) {
            // this does nothing if there is no single-cell data (beside clearing the metrics fields), maybe it
            // should be moved to the pre-processor service at some point
            taskExecutor.execute( () -> singleCellExpressionExperimentService.updateSparsityMetrics( expressionExperiment ) );
        }

        if ( reprocess ) {
            taskExecutor.execute( () -> {
                try {
                    ExpressionExperiment thawedEe = expressionExperimentService.thaw( expressionExperiment );
                    preprocessorService.process( thawedEe );
                } catch ( PreprocessingException e ) {
                    log.error( "There was an error while updating the experiment after "
                            + "making changes to the quantitation types and/or biomaterial map.", e );
                }
            } );
        }

        return new ModelAndView( "expressionExperiment.edit" )
                .addObject( "expressionExperiment", form )
                .addObject( "keywords", getKeywords( expressionExperiment ) )
                .addAllObjects( getReferenceData() );
    }

    private ExpressionExperimentEditForm getFormObject( ExpressionExperiment ee ) {
        ExpressionExperimentEditForm obj = new ExpressionExperimentEditForm();
        populateForm( obj, ee );
        LinkedHashMap<Class<? extends DataVector>, List<QuantitationTypeEditForm>> qtf = getQuantitationTypesByVectorType( ee );
        List<QuantitationTypeEditForm> qtfL = qtf.values().stream()
                .flatMap( Collection::stream )
                .collect( Collectors.toList() );
        obj.setQuantitationTypes( qtfL );
        obj.setQuantitationTypesByVectorType( qtf );
        return obj;
    }

    private void populateForm( ExpressionExperimentEditForm form, ExpressionExperiment expressionExperiment ) {
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
        List<SingleCellDimension> scds = singleCellExpressionExperimentService.getSingleCellDimensionsWithoutCellIds( expressionExperiment, initconfig );
        form.setSingleCellDimensions( scds.stream().map( SingleCellDimensionEditForm::new ).collect( Collectors.toList() ) );
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
    private LinkedHashMap<Class<? extends DataVector>, List<QuantitationTypeEditForm>> getQuantitationTypesByVectorType( ExpressionExperiment ee ) {
        // sort the mapping
        return expressionExperimentService.getQuantitationTypesByVectorType( ee ).entrySet().stream()
                .sorted( Map.Entry.comparingByKey( Comparator.comparing( Class::getSimpleName, Comparator.nullsLast( Comparator.naturalOrder() ) ) ) )
                .collect( Collectors.toMap( Entry::getKey,
                        v -> v.getValue().stream().sorted( Comparator.comparing( QuantitationType::getName ) ).map( QuantitationTypeEditForm::new ).collect( Collectors.toList() ),
                        ( a, b ) -> b,
                        LinkedHashMap::new ) );
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

    private enum QuantitationTypeUpdateStatus {
        /**
         * No changes were done.
         */
        NONE,
        /**
         * Only cosmetic changes were done, no need for reprocessing.
         */
        COSMETIC,
        /**
         * Significant changes were done that might affect the processed vectors and analyses downstream, reprocessing
         * is needed.
         */
        SIGNIFICANT
    }

    /**
     * Update the quantitation types.
     */
    private Map<QuantitationType, QuantitationTypeUpdateStatus> updateQuantitationTypes( ExpressionExperiment expressionExperiment, List<QuantitationTypeEditForm> updatedQuantitationTypes ) {
        fixDenormalizedQts( expressionExperiment );

        Map<Long, QuantitationType> qtMap = IdentifiableUtils.getIdMap( expressionExperiment.getQuantitationTypes() );

        Map<QuantitationType, QuantitationTypeUpdateStatus> result = new HashMap<>();

        for ( ExpressionExperimentEditController.QuantitationTypeEditForm editForm : updatedQuantitationTypes ) {
            QuantitationType qt = qtMap.get( editForm.getId() );

            if ( qt == null ) {
                throw new EntityNotFoundException( "No QuantitationType with ID " + editForm.getId() );
            }

            BeanWrapper qtBw = new BeanWrapperImpl( qt );

            boolean dirty = false;
            // a change in any of these fields will trigger reprocessing, that include switching the preferred set of vectors
            boolean veryDirty = false;

            dirty |= setPropertyValue( qtBw, "name", StringUtils.strip( editForm.getName() ) );
            dirty |= setPropertyValue( qtBw, "description", StringUtils.stripToNull( editForm.getDescription() ) );

            if ( editForm.getIsSingleCellPreferred() != null ) {
                veryDirty |= setPropertyValue( qtBw, "isSingleCellPreferred", editForm.getIsSingleCellPreferred() );
            }

            if ( editForm.getIsPreferred() != null ) {
                veryDirty |= setPropertyValue( qtBw, "isPreferred", editForm.getIsPreferred() );
            }

            if ( editForm.getIsMaskedPreferred() != null ) {
                dirty |= setPropertyValue( qtBw, "isMaskedPreferred", editForm.getIsMaskedPreferred() );
            }

            veryDirty |= setPropertyValue( qtBw, "generalType", GeneralType.valueOf( editForm.getGeneralType() ) );
            veryDirty |= setPropertyValue( qtBw, "type", StandardQuantitationType.valueOf( editForm.getType() ) );
            veryDirty |= setPropertyValue( qtBw, "scale", ScaleType.valueOf( editForm.getScale() ) );
            if ( editForm.getRepresentation() != null ) {
                veryDirty |= setPropertyValue( qtBw, "representation", PrimitiveType.valueOf( editForm.getRepresentation() ) );
            }
            veryDirty |= setPropertyValue( qtBw, "isRatio", editForm.getIsRatio() );
            veryDirty |= setPropertyValue( qtBw, "isNormalized", editForm.getIsNormalized() );
            veryDirty |= setPropertyValue( qtBw, "isBatchCorrected", editForm.getIsBatchCorrected() );
            veryDirty |= setPropertyValue( qtBw, "isBackground", editForm.getIsBackground() );
            veryDirty |= setPropertyValue( qtBw, "isBackgroundSubtracted", editForm.getIsBackgroundSubtracted() );
            veryDirty |= setPropertyValue( qtBw, "isRecomputedFromRawData", editForm.getIsRecomputedFromRawData() );

            // only reprocess if the preferred QT has been significantly changed, including if the preferred QT has
            // changed
            if ( veryDirty ) {
                expressionExperimentService.updateQuantitationType( expressionExperiment, qt );
                result.put( qt, QuantitationTypeUpdateStatus.SIGNIFICANT );
            } else if ( dirty ) {
                expressionExperimentService.updateQuantitationType( expressionExperiment, qt );
                result.put( qt, QuantitationTypeUpdateStatus.COSMETIC );
            } else {
                result.put( qt, QuantitationTypeUpdateStatus.NONE );
            }
        }

        return result;
    }

    /**
     * Fix any incorrectly denormalized QTs so that {@link ExpressionExperiment#getQuantitationTypes()} reflects all the
     * QTs of the experiment.
     */
    private void fixDenormalizedQts( ExpressionExperiment expressionExperiment ) {
        // this will fix incorrect denormalization of QTs
        boolean updateEe = false;
        Collection<QuantitationType> allQts = expressionExperimentService.getQuantitationTypes( expressionExperiment );
        for ( QuantitationType qt : allQts ) {
            if ( !expressionExperiment.getQuantitationTypes().contains( qt ) ) {
                log.warn( qt + " is not associated to " + expressionExperiment + ", but it belongs to at least one of its vector, adding..." );
                expressionExperiment.getQuantitationTypes().add( qt );
                updateEe = true;
            }
        }
        if ( updateEe ) {
            expressionExperimentService.update( expressionExperiment );
        }
    }

    /**
     * Change the relationship between assays and biomaterials.
     *
     * @return true if there were changes
     */
    private boolean updateBioMaterialMap( ExpressionExperiment expressionExperiment, String assayToMaterialMap ) {
        // parse JSON-serialized map
        JSONObject bioAssay2BioMaterialMap = new JSONObject( assayToMaterialMap );

        Map<BioAssay, BioMaterial> deleteAssociations = new HashMap<>();
        boolean anyChanges = false;

        int newBioMaterialCount = 0;

        // Map<Long, BioAssay> baMap = EntityUtils.getIdMap( expressionExperiment.getBioAssays() );

        for ( String bioAssayKey : bioAssay2BioMaterialMap.keySet() ) {
            // check if the bioAssayId is a nullElement
            // if it is, skip over this entry
            if ( bioAssayKey.equalsIgnoreCase( "nullElement" ) ) {
                continue;
            }

            Long bioAssayId = Long.parseLong( bioAssayKey );

            Long newMaterialId;
            Object value = bioAssay2BioMaterialMap.get( bioAssayKey );
            if ( value instanceof String ) {
                newMaterialId = Long.parseLong( bioAssay2BioMaterialMap.getString( bioAssayKey ) );
            } else {
                newMaterialId = bioAssay2BioMaterialMap.getLong( bioAssayKey );
            }

            BioAssay bioAssay = bioAssayService.loadOrFail( bioAssayId, EntityNotFoundException::new, "No Bioassay with ID " + bioAssayId ); // maybe we need to do

            if ( !expressionExperiment.getBioAssays().contains( bioAssay ) ) {
                throw new IllegalArgumentException( "Bioassay with id=" + bioAssayId + " was not associated with the experiment" );
            }

            BioMaterial currentBioMaterial = bioAssay.getSampleUsed();

            if ( newMaterialId.equals( currentBioMaterial.getId() ) ) {
                // / no change
                continue;
            }

            BioMaterial newMaterial;
            if ( newMaterialId < 0 ) { // This kludge signifies that it is a 'brand new' biomaterial.
                newMaterial = bioMaterialService.copy( currentBioMaterial );
                newMaterial.setName( "Modeled after " + currentBioMaterial.getName() );
                newMaterial.getFactorValues().clear();
                newMaterial = ( BioMaterial ) persisterHelper.persist( newMaterial );
                newBioMaterialCount++;
            } else {
                // FIXME can we just use this from the experiment, probably no need to fetch it again.
                newMaterial = bioMaterialService.loadOrFail( newMaterialId, EntityNotFoundException::new,
                        "BioMaterial with id=" + newMaterialId + " could not be loaded" );
            }
            anyChanges = true;
            ExpressionExperimentEditController.log.info( "Associating " + bioAssay + " with " + newMaterial );
            bioAssayService.addBioMaterialAssociation( bioAssay, newMaterial );
        }

        if ( anyChanges ) {
            /*
             * FIXME Decide if we need to remove the biomaterial -> factor value associations, it could be completely
             * fouled up.
             */
            ExpressionExperimentEditController.log.info( "There were changes to the BioMaterial -> BioAssay map" );
            // remove unnecessary biomaterial associations
            auditTrailService.addUpdateEvent( expressionExperiment, BioMaterialMappingUpdate.class, newBioMaterialCount + " biomaterials" );
            Collection<BioAssay> deleteKeys = deleteAssociations.keySet();
            for ( BioAssay assay : deleteKeys ) {
                /*
                 * BUG: if this fails, we end up with a useless extra biomaterial associated with the bioassay.
                 */
                bioAssayService.removeBioMaterialAssociation( assay, deleteAssociations.get( assay ) );
            }
        } else {
            ExpressionExperimentEditController.log.info( "There were no changes to the BioMaterial -> BioAssay map" );

        }

        return anyChanges;
    }

    private <T> boolean setPropertyValue( BeanWrapper qt, String property, T newVal ) {
        if ( !Objects.equals( qt.getPropertyValue( property ), newVal ) ) {
            qt.setPropertyValue( property, newVal );
            return true;
        }
        return false;
    }

    private String getKeywords( ExpressionExperiment ee ) {
        return expressionExperimentService.getAnnotations( ee ).stream()
                .map( AnnotationValueObject::getTermName )
                .collect( Collectors.joining( "," ) );
    }

    private static class ExpressionExperimentEditFormValidator implements Validator {

        private final Map<Long, Class<? extends DataVector>> vectorTypes;
        private final Set<Long> bioAssays;
        private final Set<Long> bioMaterials;

        private ExpressionExperimentEditFormValidator( ExpressionExperiment ee, Map<Long, Class<? extends DataVector>> vectorTypes ) {
            this.vectorTypes = vectorTypes;
            this.bioAssays = ee.getBioAssays().stream().map( BioAssay::getId ).collect( Collectors.toSet() );
            this.bioMaterials = ee.getBioAssays().stream().map( BioAssay::getSampleUsed ).map( BioMaterial::getId ).collect( Collectors.toSet() );
        }

        @Override
        public boolean supports( Class<?> clazz ) {
            return ExpressionExperimentEditForm.class.isAssignableFrom( clazz );
        }

        @Override
        public void validate( Object target, Errors errors ) {
            ExpressionExperimentEditForm form = ( ExpressionExperimentEditForm ) target;
            validateQuantitationTypes( form, errors );
            validateAssayToMaterialMap( form, errors );
        }

        private void validateQuantitationTypes( ExpressionExperimentEditForm form, Errors errors ) {
            if ( form.getQuantitationTypes() == null )
                return;
            int i = 0;
            Set<String> usedNames = new HashSet<>();
            int numPreferred = 0, numMaskedPreferred = 0, numSingleCellPreferred = 0;
            for ( QuantitationTypeEditForm qt : form.getQuantitationTypes() ) {
                errors.pushNestedPath( "quantitationTypes[" + i + "]" );
                // Note: there's no visible UI field for the ID, so we bind errors to the overall QT
                if ( qt.getId() == null ) {
                    errors.rejectValue( null, "required", "Quantitation type ID is required." );
                    errors.popNestedPath();
                    continue;
                }
                if ( !vectorTypes.containsKey( qt.getId() ) ) {
                    errors.rejectValue( null, "invalid", new String[] { String.valueOf( qt.getId() ) },
                            String.format( "Quantitation type with ID %d does not belong to the experiment.", qt.getId() ) );
                    errors.popNestedPath();
                    continue;
                }
                if ( StringUtils.isBlank( qt.getName() ) ) {
                    errors.rejectValue( "name", "required", "Name is required." );
                }
                if ( !usedNames.add( StringUtils.strip( qt.getName() ) ) ) {
                    // this is not enforced in the database, but it's good practice when naming QTs.
                    errors.rejectValue( "name", "unique", "Name must be unique for each quantitation type." );
                }
                Class<? extends DataVector> vectorType = vectorTypes.get( qt.getId() );
                if ( qt.getIsSingleCellPreferred() != null && qt.getIsSingleCellPreferred() ) {
                    if ( vectorType == null || !SingleCellExpressionDataVector.class.isAssignableFrom( vectorType ) ) {
                        errors.rejectValue( "isSingleCellPreferred", "invalid", "This quantitation type is not applicable to single-cell data vectors." );
                    }
                    numSingleCellPreferred++;
                }
                if ( qt.getIsPreferred() != null && qt.getIsPreferred() ) {
                    if ( vectorType == null || !RawExpressionDataVector.class.isAssignableFrom( vectorType ) ) {
                        errors.rejectValue( "isPreferred", "invalid", "This quantitation type is not applicable to raw data vectors." );
                    }
                    numPreferred++;
                }
                if ( qt.getIsMaskedPreferred() != null && qt.getIsMaskedPreferred() ) {
                    if ( vectorType == null || !ProcessedExpressionDataVector.class.isAssignableFrom( vectorType ) ) {
                        errors.rejectValue( "isMaskedPreferred", "invalid", "This quantitation type is not applicable to processed data vectors." );
                    }
                    numMaskedPreferred++;
                }
                validateRequiredEnumField( "generalType", GeneralType.class, errors );
                validateRequiredEnumField( "type", StandardQuantitationType.class, errors );
                validateRequiredEnumField( "scale", ScaleType.class, errors );
                if ( qt.getRepresentation() != null ) {
                    validateRequiredEnumField( "representation", PrimitiveType.class, errors );
                }
                errors.popNestedPath();
                i++;
            }
            // also include QTs that are not modified when counting the number of preferred QTs
            if ( numSingleCellPreferred > 1 ) {
                errors.rejectValue( "quantitationTypes", "", "There must be at most one preferred single-cell quantitation type." );
            }
            if ( numPreferred > 1 ) {
                errors.rejectValue( "quantitationTypes", "", "There must be at most one preferred raw quantitation type." );
            }
            if ( numMaskedPreferred > 1 ) {
                errors.rejectValue( "quantitationTypes", "", "There must be at most one preferred processed quantitation type." );
            }
        }

        private <T extends Enum<T>> void validateRequiredEnumField( String field, Class<T> enumClass, Errors errors ) {
            String val = ( String ) errors.getFieldValue( field );
            if ( val == null ) {
                errors.rejectValue( field, "required", "Value is required." );
                return;
            }
            try {
                Enum.valueOf( enumClass, val );
            } catch ( IllegalArgumentException e ) {
                errors.rejectValue( field, "invalid", "Invalid value." );
            }
        }

        private void validateAssayToMaterialMap( ExpressionExperimentEditForm form, Errors errors ) {
            if ( form.getAssayToMaterialMap() == null ) {
                return;
            }
            errors.pushNestedPath( "assayToMaterialMap" );
            try {
                JSONObject obj = new JSONObject( form.getAssayToMaterialMap() );
                for ( String key : obj.keySet() ) {
                    try {
                        long bioAssayId = Long.parseLong( key );
                        if ( !bioAssays.contains( bioAssayId ) ) {
                            errors.rejectValue( null, "invalid",
                                    String.format( "Assay with ID %d does not belong to the experiment.", bioAssayId ) );
                        }
                    } catch ( NumberFormatException e ) {
                        errors.rejectValue( null, "invalid", "Invalid assay ID." );
                    }
                    try {
                        long bioMaterialId = Long.parseLong( obj.getString( key ) );
                        if ( !bioMaterials.contains( bioMaterialId ) ) {
                            errors.rejectValue( null, "invalid",
                                    String.format( "Biomaterial with ID %d does not belong to the experiment.", bioMaterialId ) );
                        }
                    } catch ( NumberFormatException e ) {
                        errors.rejectValue( null, "invalid", "Invalid biomaterial ID." );
                    }
                }
            } catch ( JSONException e ) {
                errors.rejectValue( "assayToMaterialMap", "invalid", "Invalid JSON format." );
            } finally {
                errors.popNestedPath();
            }
        }
    }
}

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
import ubic.gemma.core.analysis.service.ExpressionDataDeleterService;
import ubic.gemma.model.common.auditAndSecurity.eventType.BioMaterialMappingUpdate;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValueBasicValueObject;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.persistence.util.IdentifiableUtils;
import ubic.gemma.web.controller.util.EntityNotFoundException;
import ubic.gemma.web.controller.util.MessageUtil;
import ubic.gemma.web.service.ExpressionExperimentEditControllerHelperService;

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
    private ExpressionExperimentEditControllerHelperService expressionExperimentEditControllerHelperService;
    @Autowired
    private ExpressionDataDeleterService expressionDataDeleterService;

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
        @Nullable
        private List<SingleCellDimensionEditForm> singleCellDimensions;
        /**
         * This field is populated by the backend, so it is always non-null.
         */
        private Collection<BioAssayValueObject> bioAssays;
        @Nullable
        private Long preferredCellTypeAssignmentId;
        @Nullable
        private List<CharacteristicValueObject> preferredCellTypeAssignmentValues;
        /**
         * A list of values from the current cell type factor (to compare with the current preferred CTA).
         */
        @Nullable
        private List<FactorValueBasicValueObject> cellTypeFactorValues;
        /**
         * Whether the preferred cell type assignment is compatible with the existing cell type factor.
         */
        private boolean isPreferredCellTypeAssignmentCompatibleWithCellTypeFactor;
        @Nullable
        private Set<CharacteristicValueObject> incompatibleCellTypeAssignmentValues;
        @Nullable
        private Set<FactorValueBasicValueObject> unmatchedCellTypeFactorValues;
        @Nullable
        private String assayToMaterialMap;
        /**
         * Field used to confirm destructive actions.
         */
        private String confirmation;
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
        /**
         * This field is populated by the backend, so it is always non-null.
         */
        private List<QuantitationTypeValueObject> quantitationTypes;
        @Nullable
        private List<CellTypeAssignmentEditForm> cellTypeAssignments;
        @Nullable
        private List<CellLevelCharacteristicsEditForm> cellLevelCharacteristics;

        /**
         *
         * @param quantitationTypes a set of quantitation types associated to this single-cell dimension; can be null
         * @param preferredCtaIds   a mapping of CTA IDs to whether they are preferred or not; can be null in which case
         *                          the isPreferred is filled from the database
         */
        public SingleCellDimensionEditForm( SingleCellDimension scd, @Nullable Set<QuantitationType> quantitationTypes, @Nullable Map<Long, Boolean> preferredCtaIds ) {
            this.id = scd.getId();
            if ( quantitationTypes != null ) {
                this.quantitationTypes = quantitationTypes.stream()
                        .sorted( Comparator.comparing( QuantitationType::getName ).thenComparing( QuantitationType::getId ) )
                        .map( QuantitationTypeValueObject::new )
                        .collect( Collectors.toList() );
            } else {
                this.quantitationTypes = Collections.emptyList();
            }
            this.cellTypeAssignments = scd.getCellTypeAssignments().stream().map( CellTypeAssignmentEditForm::new ).collect( Collectors.toList() );
            if ( preferredCtaIds != null ) {
                for ( CellTypeAssignmentEditForm cellTypeAssignmentEditForm : this.cellTypeAssignments ) {
                    if ( preferredCtaIds.containsKey( cellTypeAssignmentEditForm.getId() ) ) {
                        cellTypeAssignmentEditForm.setIsPreferred( preferredCtaIds.get( cellTypeAssignmentEditForm.getId() ) );
                    }
                }
            }
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

        // used by the frontend
        @SuppressWarnings("unused")
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
        // all required
        private boolean isBackground;
        private boolean isBackgroundSubtracted;
        private boolean isNormalized;
        private boolean isBatchCorrected;
        private boolean isRatio;
        private boolean isRecomputedFromRawData;
        /**
         * Disabled by default in the UI.
         */
        @Nullable
        private Boolean isAggregated;

        /**
         * Populated by the backend.
         */
        private Class<? extends DataVector> vectorType;

        public QuantitationTypeEditForm( QuantitationType qt, Class<? extends DataVector> vectorType ) {
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
            setIsAggregated( qt.getIsAggregated() );
            setVectorType( vectorType );
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

        @Nullable
        public Boolean getIsAggregated() {
            return isAggregated;
        }

        public void setIsAggregated( @Nullable Boolean isAggregated ) {
            this.isAggregated = isAggregated;
        }
    }

    @RequestMapping(value = "/expressionExperiment/editExpressionExperiment.html", method = RequestMethod.GET)
    public ModelAndView getExpressionExperimentEditPage( @RequestParam("id") Long id ) {
        return new ModelAndView( "expressionExperiment.edit" )
                .addAllObjects( expressionExperimentEditControllerHelperService.getFormObjectAndReferenceDataAndKeywordsById( id ) );
    }

    @RequestMapping(value = "/expressionExperiment/editExpressionExperiment.html", method = RequestMethod.POST)
    public ModelAndView updateExpressionExperiment( @RequestParam("id") Long id,
            @ModelAttribute("expressionExperiment") ExpressionExperimentEditForm form, BindingResult bindingResult,
            HttpServletResponse response ) {
        ExpressionExperiment expressionExperiment = expressionExperimentService.loadAndThawLiteOrFail( id, EntityNotFoundException::new );

        // the frontend only submits quantitationTypes and assayToMaterialMap, so we need to populate the remaining fields
        expressionExperimentEditControllerHelperService.populateForm( form, expressionExperiment );

        // FIXME: the update can alter properties affecting hashCode(), so an hash set is unsuitable here
        Set<QuantitationType> preferredSingleCellQuantitationTypes = new TreeSet<>( Comparator.comparing( QuantitationType::getId ) );
        Set<QuantitationType> preferredQuantitationTypes = new TreeSet<>( Comparator.comparing( QuantitationType::getId ) );
        Map<Long, Class<? extends DataVector>> qtbv = new LinkedHashMap<>();
        if ( form.getQuantitationTypes() != null ) {
            Map<Class<? extends DataVector>, Set<QuantitationType>> qtbvt = expressionExperimentService
                    .getQuantitationTypesByVectorType( expressionExperiment );
            Map<Long, QuantitationType> qtById = qtbvt.values().stream()
                    .flatMap( Set::stream )
                    .collect( Collectors.toMap( QuantitationType::getId, qt -> qt ) );
            for ( Entry<Class<? extends DataVector>, Set<QuantitationType>> entry : qtbvt.entrySet() ) {
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
                Class<? extends DataVector> vectorType = qtbv.get( qt.getId() );
                // these fields are not editable by the user, so they need to be populated
                if ( qt.getIsAggregated() == null ) {
                    qt.setIsAggregated( qtById.get( qt.getId() ).getIsAggregated() );
                }
                if ( qt.getIsMaskedPreferred() == null ) {
                    qt.setIsMaskedPreferred( qtById.get( qt.getId() ).getIsMaskedPreferred() );
                }
                if ( qt.getRepresentation() == null ) {
                    qt.setRepresentation( qtById.get( qt.getId() ).getRepresentation().toString() );
                }
                qt.setVectorType( vectorType );
                qtf.computeIfAbsent( vectorType, k -> new ArrayList<>() )
                        .add( qt );
            }
            form.setQuantitationTypesByVectorType( qtf );
        }

        ValidationUtils.invokeValidator( new ExpressionExperimentEditFormValidator( expressionExperiment, qtbv ), form, bindingResult );
        if ( bindingResult.hasErrors() ) {
            expressionExperimentEditControllerHelperService.populateCellTypeMisalignment( form, expressionExperiment );
            response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
            return new ModelAndView( "expressionExperiment.edit" )
                    .addObject( "expressionExperiment", form )
                    .addAllObjects( expressionExperimentEditControllerHelperService.getReferenceDataAndKeywords( expressionExperiment ) );
        }

        // fetch the previous CTA in case the preferred single-cell QT changes
        Optional<CellTypeAssignment> previousCta = singleCellExpressionExperimentService.getPreferredCellTypeAssignmentWithoutIndices( expressionExperiment );

        boolean reprocess = false;
        boolean recomputeSingleCellSparsityMetrics = false;
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
                    // check if it recommended to re-create the cell type factor since we changed the preferred single-cell QT
                    Optional<CellTypeAssignment> newCta = singleCellExpressionExperimentService.getPreferredCellTypeAssignmentWithoutIndices( expressionExperiment, entry.getKey() );
                    this.messageUtil.saveMessage( "Preferred single-cell quantitation type has been significantly changed, single-cell sparsity metrics will be recomputed." );
                    if ( newCta.isPresent() && !newCta.equals( previousCta ) ) {
                        this.messageUtil.saveMessage( String.format( "The preferred cell type assignment has changed to %s, you should re-create the cell type factor.",
                                newCta.get() ) );
                    }
                } else if ( preferredSingleCellQuantitationTypes.contains( entry.getKey() )
                        && expressionExperiment.getQuantitationTypes().stream().noneMatch( QuantitationType::getIsSingleCellPreferred ) ) {
                    // sparsity metrics will be cleared if there are no other preferred SC QTs
                    this.messageUtil.saveMessage( "There is no preferred single-cell quantitation type, single-cell sparsity metrics will be cleared." );
                    recomputeSingleCellSparsityMetrics = true;
                }
            }
        }

        if ( form.getSingleCellDimensions() != null ) {
            updateSingleCellDimensions( expressionExperiment, form.getSingleCellDimensions() );
        }

        if ( form.getAssayToMaterialMap() != null ) {
            if ( updateBioMaterialMap( expressionExperiment, form.getAssayToMaterialMap() ) ) {
                this.messageUtil.saveMessage( "Assay to sample associations have been changed; reprocessing will be performed." );
                reprocess = true;
            }
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

        // this needs to be filled after the updates
        // TODO: reorganize this code better to merge getFormObject() with the user's changes
        expressionExperimentEditControllerHelperService.populateCellTypeMisalignment( form, expressionExperiment );

        return new ModelAndView( "expressionExperiment.edit" )
                .addObject( "expressionExperiment", form )
                .addAllObjects( expressionExperimentEditControllerHelperService.getReferenceDataAndKeywords( expressionExperiment ) );
    }

    /**
     * Re-create the cell type factor from the preferred cell type assignment.
     *
     * @param confirmation must be equal to {@code RECREATE CTF FROM CTA {preferredCtaId} [IGNORE COMPATIBLE]} to
     *                     confirm that the user
     */
    @RequestMapping(method = RequestMethod.POST, value = "/expressionExperiment/editExpressionExperiment.html", params = { "recreateCellTypeFactor" })
    public ModelAndView recreateCellTypeFactor( @RequestParam("id") Long id, @RequestParam("confirmation") String confirmation ) {
        ExpressionExperiment ee = expressionExperimentService.loadOrFail( id, EntityNotFoundException::new );
        CellTypeAssignment preferredCta = singleCellExpressionExperimentService.getPreferredCellTypeAssignmentWithoutIndices( ee )
                .orElseThrow( () -> new IllegalArgumentException( "No preferred cell type assignment found for " + ee.getShortName() + "." ) );
        if ( !confirmation.equals( "RECREATE CTF FROM CTA " + preferredCta.getId() ) ) {
            throw new IllegalArgumentException( "No confirmation was provided for re-creating the cell type factor." );
        }
        ExperimentalFactor previousCellTypeFactor = singleCellExpressionExperimentService.getCellTypeFactor( ee ).orElse( null );
        ExperimentalFactor newCtf = singleCellExpressionExperimentService.createCellTypeFactor( ee, true, false );
        if ( newCtf != null && newCtf.equals( previousCellTypeFactor ) ) {
            messageUtil.saveMessage( "The current cell type factor was kept since it's compatible with the preferred cell type assignment." );
        } else if ( newCtf != null ) {
            messageUtil.saveMessage( "The cell type factor was " + ( previousCellTypeFactor != null ? "replaced" : "created" ) + "." );
        } else {
            // should never happen since we passed removeExistingIfNecessary
            messageUtil.saveMessage( "No cell type factor was created." );
        }
        return new ModelAndView( "expressionExperiment.edit" )
                .addAllObjects( expressionExperimentEditControllerHelperService.getFormObjectAndReferenceDataAndKeywordsById( id ) );
    }

    /**
     * @param confirmation must be equal to {@code "DELETE QT " + qtId} to confirm that the user
     */
    @RequestMapping(method = RequestMethod.POST, value = "/expressionExperiment/editExpressionExperiment.html", params = { "deleteQuantitationType" })
    public ModelAndView deleteQuantitationType( @RequestParam("id") Long id, @RequestParam("deleteQuantitationType") Long qtId, @RequestParam("confirmation") String confirmation ) {
        ExpressionExperiment ee = expressionExperimentService.loadAndThawLiteOrFail( id, EntityNotFoundException::new );
        Map<Class<? extends DataVector>, Set<QuantitationType>> qtByVt = expressionExperimentService.getQuantitationTypesByVectorType( ee );
        QuantitationType qt = null;
        Class<? extends DataVector> vectorType = null;
        for ( Entry<Class<? extends DataVector>, Set<QuantitationType>> entry : qtByVt.entrySet() ) {
            for ( QuantitationType qt2 : entry.getValue() ) {
                if ( qt2.getId().equals( qtId ) ) {
                    qt = qt2;
                    vectorType = entry.getKey();
                    break;
                }
            }
        }
        if ( qt == null ) {
            throw new EntityNotFoundException( "No quantitation type with ID " + qtId + " found for " + ee.getShortName() + "." );
        }
        if ( !confirmation.equals( "DELETE QT " + qt.getId() ) ) {
            throw new IllegalArgumentException( "No confirmation was provided for deleting the quantitation type with ID " + qt.getId() + "." );
        }
        if ( RawExpressionDataVector.class.isAssignableFrom( vectorType ) ) {
            expressionDataDeleterService.deleteRawData( ee, qt );
        } else if ( SingleCellExpressionDataVector.class.isAssignableFrom( vectorType ) ) {
            expressionDataDeleterService.deleteSingleCellData( ee, qt );
        } else if ( ProcessedExpressionDataVector.class.isAssignableFrom( vectorType ) ) {
            expressionDataDeleterService.deleteProcessedData( ee );
        } else {
            throw new IllegalArgumentException( "Deleting quantitation types of type " + vectorType.getSimpleName() + " is not supported yet." );
        }
        messageUtil.saveMessage( "Deleted " + qt + "." );
        return new ModelAndView( "expressionExperiment.edit" )
                .addAllObjects( expressionExperimentEditControllerHelperService.getFormObjectAndReferenceDataAndKeywordsById( id ) );
    }

    @RequestMapping(method = RequestMethod.POST, value = "/expressionExperiment/editExpressionExperiment.html", params = { "deleteCellTypeAssignment" })
    public ModelAndView deleteCellTypeAssignment( @RequestParam("id") Long id, @RequestParam("deleteCellTypeAssignment") Long ctaId, @RequestParam("confirmation") String confirmation ) {
        ExpressionExperiment ee = expressionExperimentService.loadAndThawLiteOrFail( id, EntityNotFoundException::new );
        if ( !confirmation.equals( "DELETE CTA " + ctaId ) ) {
            throw new IllegalArgumentException( "No confirmation was provided for deleting the cell type assignment with ID " + ctaId + "." );
        }
        singleCellExpressionExperimentService.removeCellTypeAssignmentById( ee, ctaId );
        messageUtil.saveMessage( "Deleted cell type assignment with ID " + ctaId + "." );
        return new ModelAndView( "expressionExperiment.edit" )
                .addAllObjects( expressionExperimentEditControllerHelperService.getFormObjectAndReferenceDataAndKeywordsById( id ) );
    }

    @RequestMapping(method = RequestMethod.POST, value = "/expressionExperiment/editExpressionExperiment.html", params = { "deleteCellLevelCharacteristics" })
    public ModelAndView deleteCellLevelCharacteristics( @RequestParam("id") Long id, @RequestParam("deleteCellLevelCharacteristics") Long clcId, @RequestParam("confirmation") String confirmation ) {
        ExpressionExperiment ee = expressionExperimentService.loadAndThawLiteOrFail( id, EntityNotFoundException::new );
        if ( !confirmation.equals( "DELETE CLC " + clcId ) ) {
            throw new IllegalArgumentException( "No confirmation was provided for deleting the cell-level characteristics with ID " + clcId + "." );
        }
        singleCellExpressionExperimentService.removeCellLevelCharacteristicsById( ee, clcId );
        messageUtil.saveMessage( "Deleted cell-level characteristics with ID " + clcId + "." );
        return new ModelAndView( "expressionExperiment.edit" )
                .addAllObjects( expressionExperimentEditControllerHelperService.getFormObjectAndReferenceDataAndKeywordsById( id ) );
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

            // may be null if there is no associated vectors, careful!
            Class<? extends DataVector> vectorType = editForm.getVectorType();
            QuantitationType previousPreferredQt;
            if ( SingleCellExpressionDataVector.class.isAssignableFrom( vectorType ) ) {
                previousPreferredQt = expressionExperiment.getQuantitationTypes().stream()
                        .filter( QuantitationType::getIsSingleCellPreferred )
                        .findFirst()
                        .orElse( null );
            } else if ( RawExpressionDataVector.class.isAssignableFrom( vectorType ) ) {
                previousPreferredQt = expressionExperiment.getQuantitationTypes().stream()
                        .filter( QuantitationType::getIsPreferred )
                        .findFirst()
                        .orElse( null );
            } else if ( ProcessedExpressionDataVector.class.isAssignableFrom( vectorType ) ) {
                previousPreferredQt = expressionExperiment.getQuantitationTypes().stream()
                        .filter( QuantitationType::getIsMaskedPreferred )
                        .findFirst()
                        .orElse( null );
            } else {
                throw new UnsupportedOperationException( "Unsupported data vector type: " + vectorType );
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
            if ( editForm.getIsAggregated() != null ) {
                dirty |= setPropertyValue( qtBw, "isAggregated", editForm.getIsAggregated() );
            }

            // only reprocess if the preferred QT has been significantly changed, including if the preferred QT has
            // changed
            if ( veryDirty ) {
                expressionExperimentService.updateQuantitationType( expressionExperiment, qt, previousPreferredQt );
                result.put( qt, QuantitationTypeUpdateStatus.SIGNIFICANT );
            } else if ( dirty ) {
                expressionExperimentService.updateQuantitationType( expressionExperiment, qt, previousPreferredQt );
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

    private void updateSingleCellDimensions( ExpressionExperiment expressionExperiment, List<SingleCellDimensionEditForm> singleCellDimensions ) {
        for ( SingleCellDimensionEditForm form : singleCellDimensions ) {
            // TODO: avoid loading cell IDs, but that won't work with Hibernate's Session.update()
            SingleCellDimension scd = singleCellExpressionExperimentService.getSingleCellDimensionByIdWithoutCellIds( expressionExperiment,
                    form.getId(), SingleCellExpressionExperimentService.SingleCellDimensionInitializationConfig.builder()
                            .includeCtas( true ) // we need those to tell if the preferred CTA changed, or was cleared, or nothing was changed
                            .includeCharacteristics( true ) // needed to check if the CTA is aligned with the CTF
                            .build() );
            if ( scd == null ) {
                throw new EntityNotFoundException( "No SingleCellDimension with ID " + form.getId() + " in " + expressionExperiment.getShortName() + "." );
            }
            if ( form.cellTypeAssignments != null ) {
                Map<Long, CellTypeAssignment> ctaById = IdentifiableUtils.getIdMap( scd.getCellTypeAssignments() );
                CellTypeAssignment preferredCta = scd.getCellTypeAssignments().stream().filter( CellTypeAssignment::isPreferred ).findFirst().orElse( null );
                CellTypeAssignment newPreferredCta = form.cellTypeAssignments.stream()
                        .filter( CellTypeAssignmentEditForm::getIsPreferred )
                        .map( CellTypeAssignmentEditForm::getId )
                        .map( ctaById::get )
                        .map( Objects::requireNonNull )
                        .findFirst()
                        .orElse( null );
                if ( !Objects.equals( preferredCta, newPreferredCta ) ) {
                    SingleCellExpressionExperimentService.PreferredCellTypeAssignmentChangeOutcome outcome;
                    if ( newPreferredCta != null ) {
                        outcome = singleCellExpressionExperimentService.changePreferredCellTypeAssignment( expressionExperiment, scd, newPreferredCta, false, false );
                        messageUtil.saveMessage( "The preferred cell type assignment was changed to " + newPreferredCta.getName() + "." );
                    } else {
                        outcome = singleCellExpressionExperimentService.clearPreferredCellTypeAssignment( expressionExperiment, scd );
                        messageUtil.saveMessage( "The preferred cell type assignment was cleared." );
                    }
                    switch ( outcome ) {
                        case UNCHANGED:
                            // this is important to report because the user might be expecting a change
                            messageUtil.saveMessage( "The preferred cell type assignment was left unchanged." );
                            break;
                        case CELL_TYPE_FACTOR_RECREATED:
                            messageUtil.saveMessage( "The cell type factor was re-created." );
                            break;
                        case CELL_TYPE_FACTOR_REMOVED:
                            messageUtil.saveMessage( "The cell type factor was removed." );
                            break;
                        case CELL_TYPE_FACTOR_UNCHANGED:
                            break;
                        case CELL_TYPE_FACTOR_UNCHANGED_BUT_MISALIGNED:
                            messageUtil.saveMessage( "The cell type factor was left unchanged, but it is now misaligned with the new preferred cell type assignment. You should re-create it." );
                            break;
                        default:
                            log.warn( "Unsupported outcome " + outcome + " when changing the preferred CTA in " + scd + "." );
                            break;
                    }
                } else {
                    // no change, including the case where both are null
                    log.debug( "No change to the preferred CTA in " + scd + "." );
                }
            }
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
            ExpressionExperimentEditController.log.debug( "There were no changes to the BioMaterial -> BioAssay map" );

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
            validateSingleCellDimensions( form, errors );
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
                    errors.rejectValue( null, "errors.QuantitationType.id.required" );
                    errors.popNestedPath();
                    continue;
                }
                if ( !vectorTypes.containsKey( qt.getId() ) ) {
                    errors.rejectValue( null, "errors.QuantitationType.doesNotBelongToExperiment",
                            new String[] { String.valueOf( qt.getId() ) }, null );
                    errors.popNestedPath();
                    continue;
                }
                if ( StringUtils.isBlank( qt.getName() ) ) {
                    errors.rejectValue( "name", "errors.QuantitationType.name.required" );
                }
                if ( !usedNames.add( StringUtils.strip( qt.getName() ) ) ) {
                    // this is not enforced in the database, but it's good practice when naming QTs.
                    errors.rejectValue( "name", "errors.QuantitationType.name.unique" );
                }
                Class<? extends DataVector> vectorType = vectorTypes.get( qt.getId() );
                if ( qt.getIsSingleCellPreferred() != null && qt.getIsSingleCellPreferred() ) {
                    if ( vectorType == null || !SingleCellExpressionDataVector.class.isAssignableFrom( vectorType ) ) {
                        errors.rejectValue( "isSingleCellPreferred", "errors.QuantitationType.isSingleCellPreferred.notApplicableToVectorType" );
                    }
                    numSingleCellPreferred++;
                }
                if ( qt.getIsPreferred() != null && qt.getIsPreferred() ) {
                    if ( vectorType == null || !RawExpressionDataVector.class.isAssignableFrom( vectorType ) ) {
                        errors.rejectValue( "isPreferred", "errors.QuantitationType.isPreferred.notApplicableToVectorType" );
                    }
                    numPreferred++;
                }
                if ( qt.getIsMaskedPreferred() != null && qt.getIsMaskedPreferred() ) {
                    if ( vectorType == null || !ProcessedExpressionDataVector.class.isAssignableFrom( vectorType ) ) {
                        errors.rejectValue( "isMaskedPreferred", "errors.QuantitationType.isMaskedPreferred.notApplicableToVectorType" );
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
                errors.rejectValue( "quantitationTypes", "errors.QuantitationType.atMostOnePreferredSingleCellQuantitationType" );
            }
            if ( numPreferred > 1 ) {
                errors.rejectValue( "quantitationTypes", "errors.QuantitationType.atMostOnePreferredRawQuantitationType" );
            }
            if ( numMaskedPreferred > 1 ) {
                errors.rejectValue( "quantitationTypes", "errors.QuantitationType.atMostOnePreferredProcessedQuantitationType" );
            }
        }

        private <T extends Enum<T>> void validateRequiredEnumField( String field, Class<T> enumClass, Errors errors ) {
            String val = ( String ) errors.getFieldValue( field );
            if ( val == null ) {
                errors.rejectValue( field, "errors.Enum.required" );
                return;
            }
            try {
                Enum.valueOf( enumClass, val );
            } catch ( IllegalArgumentException e ) {
                errors.rejectValue( field, "errors.Enum.invalid" );
            }
        }

        private void validateSingleCellDimensions( ExpressionExperimentEditForm form, Errors errors ) {
            if ( form.getSingleCellDimensions() == null ) {
                return;
            }
            for ( int i = 0; i < form.getSingleCellDimensions().size(); i++ ) {
                errors.pushNestedPath( "singleCellDimensions[" + i + "]" );
                // check if all CTAs are un
                validateSingleCellDimension( form.getSingleCellDimensions().get( i ), errors );
                errors.popNestedPath();
            }
        }

        /**
         * Only the "preferred" state is editable for now.
         */
        private void validateSingleCellDimension( SingleCellDimensionEditForm singleCellDimensionEditForm, Errors errors ) {
            if ( singleCellDimensionEditForm.cellTypeAssignments == null ) {
                return;
            }
            long numberOfPreferredCtas = singleCellDimensionEditForm.cellTypeAssignments.stream()
                    .filter( CellTypeAssignmentEditForm::getIsPreferred )
                    .count();
            if ( numberOfPreferredCtas > 1 ) {
                errors.rejectValue( "cellTypeAssignments", "errors.CellTypeAssignment.atMostOnePreferred" );
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
                            errors.rejectValue( null, "errors.assayToMaterialMap.assayDoesNotBelongToExperiment",
                                    new String[] { String.valueOf( bioAssayId ) }, null );
                        }
                    } catch ( NumberFormatException e ) {
                        errors.rejectValue( null, "errors.assayToMaterialMap.invalidAssayId" );
                    }
                    try {
                        long bioMaterialId = Long.parseLong( obj.getString( key ) );
                        if ( !bioMaterials.contains( bioMaterialId ) ) {
                            errors.rejectValue( null, "errors.assayToMaterialMap.sampleDoesNotBelongToExperiment",
                                    new String[] { String.valueOf( bioMaterialId ) }, null );
                        }
                    } catch ( NumberFormatException e ) {
                        errors.rejectValue( null, "errors.assayToMaterialMap.invalidSampleId" );
                    }
                }
            } catch ( JSONException e ) {
                log.warn( "Invalid JSON format in assayToMaterialMap", e );
                errors.rejectValue( null, "errors.assayToMaterialMap.invalidJsonFormat" );
            } finally {
                errors.popNestedPath();
            }
        }
    }
}

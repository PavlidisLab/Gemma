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

import gemma.gsec.util.SecurityUtil;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.core.analysis.preprocess.PreprocessingException;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.BioMaterialMappingUpdate;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.util.EntityNotFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Handle editing of expression experiments.
 *
 * @author keshav
 */
public class ExpressionExperimentFormController extends BaseFormController {

    private AuditTrailService auditTrailService;
    private BioAssayService bioAssayService = null;
    private BioMaterialService bioMaterialService = null;
    private ExpressionExperimentService expressionExperimentService = null;
    private ExternalDatabaseService externalDatabaseService = null;
    private Persister persisterHelper = null;

    private PreprocessorService preprocessorService;
    private QuantitationTypeService quantitationTypeService;

    @SuppressWarnings("deprecation")
    public ExpressionExperimentFormController() {
        /*
         * if true, reuses the same command object across the edit-submit-process (get-post-process).
         */
        this.setSessionForm( true );
        this.setCommandClass( ExpressionExperimentEditValueObject.class );
    }

    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        BaseFormController.log.debug( "entering processFormSubmission" );

        Long id = ( ( ExpressionExperimentValueObject ) command ).getId();

        if ( request.getParameter( "cancel" ) != null ) {
            if ( id != null ) {
                return new ModelAndView( new RedirectView(
                        "http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath()
                                + "/expressionExperiment/showExpressionExperiment.html?id=" + id ) );
            }

            BaseFormController.log.warn( "Cannot find details view due to null id.  Redirecting to overview" );
            return new ModelAndView( new RedirectView(
                    "http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath()
                            + "/expressionExperiment/showAllExpressionExperiments.html" ) );
        }

        ModelAndView mav = super.processFormSubmission( request, response, command, errors );

        ExpressionExperiment ee = expressionExperimentService.loadOrFail( id );

        Set<Entry<QuantitationType, Long>> s = expressionExperimentService.getQuantitationTypeCount( ee )
                .entrySet();
        mav.addObject( "qtCountSet", s );

        // add count of designElementDataVectors
        mav.addObject( "designElementDataVectorCount",
                expressionExperimentService.getDesignElementDataVectorCount( ee ) );
        return mav;
    }

    /**
     * @param auditTrailService the auditTrailService to set
     */
    public void setAuditTrailService( AuditTrailService auditTrailService ) {
        this.auditTrailService = auditTrailService;
    }

    /**
     * @param bioAssayService the bioAssayService to set
     */
    public void setBioAssayService( BioAssayService bioAssayService ) {
        this.bioAssayService = bioAssayService;
    }

    /**
     * @param bioMaterialService the bioMaterialService to set
     */
    public void setBioMaterialService( BioMaterialService bioMaterialService ) {
        this.bioMaterialService = bioMaterialService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
    }

    /**
     * @param persisterHelper the persisterHelper to set
     */
    public void setPersisterHelper( Persister persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    public void setPreprocessorService( PreprocessorService preprocessorService ) {
        this.preprocessorService = preprocessorService;
    }

    public void setQuantitationTypeService( QuantitationTypeService quantitationTypeService ) {
        this.quantitationTypeService = quantitationTypeService;
    }

    @Override
    protected Object formBackingObject( HttpServletRequest request ) {
        if ( !SecurityUtil.isUserLoggedIn() ) {
            throw new AccessDeniedException( "User does not have access to experiment management" );
        }

        Long id;
        try {
            id = Long.parseLong( request.getParameter( "id" ) );
        } catch ( NumberFormatException e ) {
            this.saveMessage( request, "Id was not a number " + request.getParameter( "id" ) );
            throw new IllegalArgumentException( "Id was not a number " + request.getParameter( "id" ) );
        }

        BaseFormController.log.debug( id );
        ExpressionExperimentEditValueObject obj;

        ExpressionExperiment ee = expressionExperimentService.loadAndThawLiteOrFail( id,
                EntityNotFoundException::new, String.format( "No experiment with ID %d", id ) );

        List<QuantitationTypeValueObject> qts = new ArrayList<>(
                quantitationTypeService.loadValueObjects( expressionExperimentService.getQuantitationTypes( ee ) ) );

        ExpressionExperimentValueObject vo = expressionExperimentService.loadValueObject( ee );

        if ( vo == null ) {
            throw new EntityNotFoundException( String.format( "Could load experiment VO with ID %d", id ) );
        }

        obj = new ExpressionExperimentEditValueObject( vo );

        obj.setQuantitationTypes( qts );
        obj.setBioAssays( BioAssayValueObject.convert2ValueObjects( ee.getBioAssays() ) );

        this.saveMessage( request, "Editing dataset" );

        return obj;
    }

    private static final List<String>
            STANDARD_QUANTITATION_TYPES = Arrays.stream( StandardQuantitationType.values() ).map( Enum::name ).sorted().collect( Collectors.toList() ),
            SCALE_TYPES = Arrays.stream( ScaleType.values() ).map( Enum::name ).sorted().collect( Collectors.toList() ),
            GENERAL_QUANTITATION_TYPES = Arrays.stream( GeneralType.values() ).map( Enum::name ).sorted().collect( Collectors.toList() ),
            REPRESENTATIONS = Arrays.stream( PrimitiveType.values() ).map( Enum::name ).sorted().collect( Collectors.toList() );

    @Override
    protected Map<Object, Object> referenceData( HttpServletRequest request ) {
        Map<Object, Object> referenceData = new HashMap<>();
        Collection<ExternalDatabase> edCol = externalDatabaseService.loadAll();

        Collection<ExternalDatabase> keepers = new HashSet<>();
        for ( ExternalDatabase database : edCol ) {
            if ( database.getType() == null )
                continue;
            if ( database.getType().equals( DatabaseType.EXPRESSION ) ) {
                keepers.add( database );
            }
        }

        referenceData.put( "externalDatabases", keepers );

        referenceData.put( "standardQuantitationTypes", new ArrayList<>( STANDARD_QUANTITATION_TYPES ) );
        referenceData.put( "scaleTypes", new ArrayList<>( SCALE_TYPES ) );
        referenceData.put( "generalQuantitationTypes", new ArrayList<>( GENERAL_QUANTITATION_TYPES ) );
        referenceData.put( "representations", new ArrayList<>( REPRESENTATIONS ) );
        return referenceData;
    }

    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) {

        ExpressionExperimentEditValueObject eeCommand = ( ExpressionExperimentEditValueObject ) command;
        ExpressionExperiment expressionExperiment = expressionExperimentService.loadAndThawLiteOrFail( eeCommand.getId(),
                EntityNotFoundException::new, String.format( "No experiment with ID %d", eeCommand.getId() ) );

        /*
         * Much more complicated
         */
        boolean changedQT = this.updateQuantTypes( request, expressionExperiment, eeCommand.getQuantitationTypes() );
        boolean changedBMM = this.updateBioMaterialMap( request, expressionExperiment );

        if ( changedQT || changedBMM ) {
            try {
                preprocessorService.process( expressionExperiment );
            } catch ( PreprocessingException e ) {
                throw new RuntimeException( "There was an error while updating the experiment after "
                        + "making changes to the quantitation types and/or biomaterial map.", e );
            }
        }

        return new ModelAndView( new RedirectView(
                "http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath()
                        + "/expressionExperiment/showExpressionExperiment.html?id=" + eeCommand.getId() ) );
    }

    private void audit( ExpressionExperiment ee, Class<? extends AuditEventType> eventType, String note ) {
        auditTrailService.addUpdateEvent( ee, eventType, note );
    }

    private String scrub( String s ) {
        return StringEscapeUtils.escapeHtml4( s );
    }

    /**
     * Change the relationship between bioassays and biomaterials.
     *
     * @param request              request
     * @param expressionExperiment ee
     * @return true if there were changes
     */
    private boolean updateBioMaterialMap( HttpServletRequest request, ExpressionExperiment expressionExperiment ) {
        // parse JSON-serialized map
        JSONObject bioAssay2BioMaterialMap = new JSONObject( request.getParameter( "assayToMaterialMap" ) );

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
            BaseFormController.log.info( "Associating " + bioAssay + " with " + newMaterial );
            bioAssayService.addBioMaterialAssociation( bioAssay, newMaterial );
        }

        if ( anyChanges ) {
            /*
             * FIXME Decide if we need to remove the biomaterial -> factor value associations, it could be completely
             * fouled up.
             */
            BaseFormController.log.info( "There were changes to the BioMaterial -> BioAssay map" );
            this.audit( expressionExperiment, BioMaterialMappingUpdate.class,
                    newBioMaterialCount + " biomaterials" ); // remove unnecessary biomaterial associations
            Collection<BioAssay> deleteKeys = deleteAssociations.keySet();
            for ( BioAssay assay : deleteKeys ) {
                /*
                 * BUG: if this fails, we end up with a useless extra biomaterial associated with the bioassay.
                 */
                bioAssayService.removeBioMaterialAssociation( assay, deleteAssociations.get( assay ) );
            }
        } else {
            BaseFormController.log.info( "There were no changes to the BioMaterial -> BioAssay map" );

        }

        return anyChanges;
    }

    /**
     * Check old vs. new quantitation types, and update any affected data vectors.
     *
     * @param request                  request
     * @param expressionExperiment     ee
     * @param updatedQuantitationTypes updated QTs
     * @return whether any changes were made
     */
    private boolean updateQuantTypes( HttpServletRequest request, ExpressionExperiment expressionExperiment,
            Collection<QuantitationTypeValueObject> updatedQuantitationTypes ) {

        Collection<QuantitationType> oldQuantitationTypes = expressionExperimentService
                .getQuantitationTypes( expressionExperiment );

        boolean anyChanged = false;
        for ( QuantitationType qType : oldQuantitationTypes ) {
            assert qType != null;
            Long id = qType.getId();
            boolean dirty = false;
            QuantitationType revisedType = QuantitationType.Factory.newInstance();
            for ( QuantitationTypeValueObject newQtype : updatedQuantitationTypes ) {
                if ( newQtype.getId().equals( id ) ) {

                    String oldName = qType.getName();
                    String oldDescription = qType.getDescription();
                    GeneralType gentype = qType.getGeneralType();
                    boolean isBkg = qType.getIsBackground();
                    boolean isBkgSub = qType.getIsBackgroundSubtracted();
                    boolean isNormalized = qType.getIsNormalized();
                    PrimitiveType rep = qType.getRepresentation();
                    ScaleType scale = qType.getScale();
                    StandardQuantitationType type = qType.getType();
                    boolean isPreferred = qType.getIsPreferred();
                    boolean isMaskedPreferred = qType.getIsMaskedPreferred();
                    boolean isRatio = qType.getIsRatio();
                    boolean isRecomputedFromRawDAta = qType.getIsRecomputedFromRawData();
                    boolean isBatchCorrected = qType.getIsBatchCorrected();

                    String newName = newQtype.getName();
                    String newDescription = newQtype.getDescription();
                    GeneralType newgentype = GeneralType.valueOf( newQtype.getGeneralType() );
                    boolean newisBkg = newQtype.getIsBackground();
                    boolean newisBkgSub = newQtype.getIsBackgroundSubtracted();
                    boolean newisNormalized = newQtype.getIsNormalized();
                    PrimitiveType newrep = PrimitiveType.valueOf( newQtype.getRepresentation() );
                    ScaleType newscale = ScaleType.valueOf( newQtype.getScale() );
                    StandardQuantitationType newType = StandardQuantitationType.valueOf( newQtype.getType() );
                    boolean newisPreferred = newQtype.getIsPreferred();
                    boolean newIsmaskedPreferred = newQtype.getIsMaskedPreferred();
                    boolean newisRatio = newQtype.getIsRatio();

                    boolean newIsBatchCorrected = newQtype.getIsBatchCorrected();
                    boolean newIsRecomputedFromRawData = newQtype.getIsRecomputedFromRawData();

                    // make it a copy.
                    revisedType.setIsBackgroundSubtracted( newisBkgSub );
                    revisedType.setIsBackground( newisBkg );
                    revisedType.setIsPreferred( newisPreferred );
                    revisedType.setIsMaskedPreferred( newIsmaskedPreferred );
                    revisedType.setIsRatio( newisRatio );
                    revisedType.setRepresentation( newrep );
                    revisedType.setType( newType );
                    revisedType.setScale( newscale );
                    revisedType.setGeneralType( newgentype );
                    revisedType.setDescription( this.scrub( newDescription ) );
                    revisedType.setName( this.scrub( newName ) );
                    revisedType.setIsNormalized( newisNormalized );
                    revisedType.setIsBatchCorrected( newIsBatchCorrected );
                    revisedType.setIsRecomputedFromRawData( newIsRecomputedFromRawData );

                    qType.setIsBackgroundSubtracted( newisBkgSub );
                    qType.setIsBackground( newisBkg );
                    qType.setIsPreferred( newisPreferred );
                    qType.setIsMaskedPreferred( newIsmaskedPreferred );
                    qType.setIsRatio( newisRatio );
                    qType.setRepresentation( newrep );
                    qType.setType( newType );
                    qType.setScale( newscale );
                    qType.setGeneralType( newgentype );
                    qType.setDescription( this.scrub( newDescription ) );
                    qType.setName( this.scrub( newName ) );
                    qType.setIsNormalized( newisNormalized );
                    qType.setIsBatchCorrected( newIsBatchCorrected );
                    qType.setIsRecomputedFromRawData( newIsRecomputedFromRawData );

                    if ( newName != null && ( oldName == null || !oldName.equals( newName ) ) ) {
                        dirty = true;
                    }
                    if ( newDescription != null && ( oldDescription == null || !oldDescription
                            .equals( newDescription ) ) ) {
                        dirty = true;
                    }
                    if ( !gentype.equals( newgentype ) ) {
                        dirty = true;
                    }
                    if ( !scale.equals( newscale ) ) {
                        dirty = true;
                    }

                    if ( !type.equals( newType ) ) {
                        dirty = true;
                    }
                    if ( !rep.equals( newrep ) ) {
                        dirty = true;
                    }
                    if ( isPreferred != newisPreferred ) {
                        // special case - make sure there is only one preferred per platform?
                        dirty = true;
                    }
                    if ( isMaskedPreferred != newIsmaskedPreferred ) {
                        // special case - make sure there is only one preferred per platform?
                        dirty = true;
                    }

                    if ( isBkg != newisBkg ) {
                        dirty = true;
                    }
                    if ( isBkgSub != newisBkgSub ) {
                        dirty = true;
                    }
                    if ( isNormalized != newisNormalized ) {
                        dirty = true;
                    }

                    if ( isRatio != newisRatio ) {
                        dirty = true;
                    }

                    if ( isBatchCorrected != newIsBatchCorrected ) {
                        dirty = true;
                    }

                    if ( isRecomputedFromRawDAta != newIsRecomputedFromRawData ) {
                        dirty = true;
                    }

                    break;
                }
            }
            if ( dirty ) {
                // update the quantitation type
                quantitationTypeService.update( qType );

            }
            anyChanged = dirty;
        }
        return anyChanged;
    }
}

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
import lombok.extern.apachecommons.CommonsLog;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.core.analysis.preprocess.PreprocessingException;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
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
import ubic.gemma.web.util.EntityNotFoundException;
import ubic.gemma.web.util.MessageUtil;

import javax.servlet.http.HttpServletRequest;
import java.text.NumberFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * Handle editing of expression experiments.
 *
 * @author keshav
 */
@CommonsLog
@Controller
public class ExpressionExperimentEditController {

    private static final List<String>
            STANDARD_QUANTITATION_TYPES = Arrays.stream( StandardQuantitationType.values() ).map( Enum::name ).sorted().collect( Collectors.toList() ),
            SCALE_TYPES = Arrays.stream( ScaleType.values() ).map( Enum::name ).sorted().collect( Collectors.toList() ),
            GENERAL_QUANTITATION_TYPES = Arrays.stream( GeneralType.values() ).map( Enum::name ).sorted().collect( Collectors.toList() ),
            REPRESENTATIONS = Arrays.stream( PrimitiveType.values() ).map( Enum::name ).sorted().collect( Collectors.toList() );

    @Autowired
    private AuditTrailService auditTrailService;
    @Autowired
    private BioAssayService bioAssayService;
    @Autowired
    private BioMaterialService bioMaterialService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ExternalDatabaseService externalDatabaseService;
    @Autowired
    private Persister persisterHelper;
    @Autowired
    private PreprocessorService preprocessorService;
    @Autowired
    private QuantitationTypeService quantitationTypeService;
    @Autowired
    private MessageUtil messageUtil;

    /**
     * Set up a custom property editor for converting form inputs to real objects. Override this to add additional
     * custom editors (call super.initBinder() in your implementation)
     */
    @InitBinder
    protected void initBinder( WebDataBinder binder ) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        binder.registerCustomEditor( Integer.class, null, new CustomNumberEditor( Integer.class, nf, true ) );
        binder.registerCustomEditor( Long.class, null, new CustomNumberEditor( Long.class, nf, true ) );
        binder.registerCustomEditor( byte[].class, new ByteArrayMultipartFileEditor() );
    }

    @RequestMapping(value = "/expressionExperiment/editExpressionExperiment.html", method = RequestMethod.GET)
    public ModelAndView getExpressionExperiment( @RequestParam("id") Long id, HttpServletRequest request ) {
        ExpressionExperimentValueObject command = this.formBackingObject( id, request );

        ExpressionExperimentEditController.log.debug( "entering processFormSubmission" );

        if ( request.getParameter( "cancel" ) != null ) {
            if ( id != null ) {
                return new ModelAndView( new RedirectView( "/expressionExperiment/showExpressionExperiment.html?id=" + id, true ) );
            }

            ExpressionExperimentEditController.log.warn( "Cannot find details view due to null id.  Redirecting to overview" );
            return new ModelAndView( new RedirectView( "/expressionExperiment/showAllExpressionExperiments.html", true ) );
        }

        ModelAndView mav = new ModelAndView( "expressionExperiment.edit" );

        ExpressionExperiment ee = expressionExperimentService.loadOrFail( id );

        Set<Entry<QuantitationType, Long>> s = expressionExperimentService.getQuantitationTypeCount( ee )
                .entrySet();
        mav.addObject( "qtCountSet", s );

        // add count of designElementDataVectors
        mav.addObject( "designElementDataVectorCount",
                expressionExperimentService.getDesignElementDataVectorCount( ee ) );

        Collection<ExternalDatabase> edCol = externalDatabaseService.loadAll();

        Collection<ExternalDatabase> keepers = new HashSet<>();
        for ( ExternalDatabase database : edCol ) {
            if ( database.getType() == null )
                continue;
            if ( database.getType().equals( DatabaseType.EXPRESSION ) ) {
                keepers.add( database );
            }
        }

        mav.addObject( "expressionExperiment", command );
        mav.addObject( "externalDatabases", keepers );
        mav.addObject( "standardQuantitationTypes", new ArrayList<>( STANDARD_QUANTITATION_TYPES ) );
        mav.addObject( "scaleTypes", new ArrayList<>( SCALE_TYPES ) );
        mav.addObject( "generalQuantitationTypes", new ArrayList<>( GENERAL_QUANTITATION_TYPES ) );
        mav.addObject( "representations", new ArrayList<>( REPRESENTATIONS ) );

        return mav;
    }

    @RequestMapping(value = "/expressionExperiment/editExpressionExperiment.html", method = RequestMethod.POST)
    public ModelAndView updateExpressionExperiment( ExpressionExperimentEditValueObject eeCommand, HttpServletRequest request ) {
        ExpressionExperiment expressionExperiment = expressionExperimentService.loadAndThawLiteOrFail( eeCommand.getId(),
                EntityNotFoundException::new, String.format( "No experiment with ID %d", eeCommand.getId() ) );

        /*
         * Much more complicated
         */
        boolean changedQT = this.updateQuantTypes( expressionExperiment, eeCommand.getQuantitationTypes() );
        boolean changedBMM = this.updateBioMaterialMap( request, expressionExperiment );

        if ( changedQT || changedBMM ) {
            try {
                preprocessorService.process( expressionExperiment );
            } catch ( PreprocessingException e ) {
                throw new RuntimeException( "There was an error while updating the experiment after "
                        + "making changes to the quantitation types and/or biomaterial map.", e );
            }
        }

        return new ModelAndView( new RedirectView( "/expressionExperiment/showExpressionExperiment.html?id=" + eeCommand.getId(), true ) );
    }

    private ExpressionExperimentEditValueObject formBackingObject( Long id, HttpServletRequest request ) {
        if ( !SecurityUtil.isUserLoggedIn() ) {
            throw new AccessDeniedException( "User does not have access to experiment management" );
        }

        ExpressionExperimentEditController.log.debug( id );

        ExpressionExperiment ee = expressionExperimentService.loadAndThawLiteOrFail( id,
                EntityNotFoundException::new, String.format( "No experiment with ID %d", id ) );

        List<QuantitationTypeValueObject> qts = new ArrayList<>(
                quantitationTypeService.loadValueObjects( expressionExperimentService.getQuantitationTypes( ee ) ) );

        ExpressionExperimentValueObject vo = expressionExperimentService.loadValueObject( ee );

        if ( vo == null ) {
            throw new EntityNotFoundException( String.format( "Could load experiment VO with ID %d", id ) );
        }

        ExpressionExperimentEditValueObject obj = new ExpressionExperimentEditValueObject( vo );

        obj.setQuantitationTypes( qts );
        obj.setBioAssays( BioAssayValueObject.convert2ValueObjects( ee.getBioAssays() ) );

        messageUtil.saveMessage( request, "Editing dataset" );

        return obj;
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

    /**
     * Check old vs. new quantitation types, and update any affected data vectors.
     *
     * @param expressionExperiment     ee
     * @param updatedQuantitationTypes updated QTs
     * @return whether any changes were made
     */
    private boolean updateQuantTypes( ExpressionExperiment expressionExperiment,
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
                    revisedType.setDescription( escapeHtml4( newDescription ) );
                    revisedType.setName( escapeHtml4( newName ) );
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
                    qType.setDescription( escapeHtml4( newDescription ) );
                    qType.setName( escapeHtml4( newName ) );
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

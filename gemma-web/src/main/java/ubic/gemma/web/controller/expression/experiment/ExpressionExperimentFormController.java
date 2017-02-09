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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.tools.ant.filters.StringInputStream;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import com.sdicons.json.model.JSONInteger;
import com.sdicons.json.model.JSONObject;
import com.sdicons.json.model.JSONString;
import com.sdicons.json.model.JSONValue;
import com.sdicons.json.parser.JSONParser;

import antlr.ANTLRException;
import ubic.gemma.analysis.preprocess.PreprocessingException;
import ubic.gemma.analysis.preprocess.PreprocessorService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.BioMaterialMappingUpdate;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.Persister;
import ubic.gemma.web.controller.BaseFormController;

/**
 * Handle editing of expression experiments.
 * 
 * @author keshav
 * @version $Id$
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
        setSessionForm( true );
        setCommandClass( ExpressionExperimentEditValueObject.class );
    }

    /**
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     */
    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        ExpressionExperimentEditValueObject eeCommand = ( ExpressionExperimentEditValueObject ) command;
        ExpressionExperiment expressionExperiment = expressionExperimentService.load( eeCommand.getId() );

        if ( expressionExperiment == null ) {
            throw new IllegalArgumentException( "Could not load experiment" );
        }

        expressionExperiment = expressionExperimentService.thawLite( expressionExperiment );

        /**
         * Much more complicated
         */
        boolean changedQT = updateQuantTypes( request, expressionExperiment, eeCommand.getQuantitationTypes() );
        boolean changedBMM = updateBioMaterialMap( request, expressionExperiment );

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

    /**
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     */
    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        log.debug( "entering processFormSubmission" );

        Long id = ( ( ExpressionExperimentValueObject ) command ).getId();

        if ( request.getParameter( "cancel" ) != null ) {
            if ( id != null ) {
                return new ModelAndView( new RedirectView(
                        "http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath()
                                + "/expressionExperiment/showExpressionExperiment.html?id=" + id ) );
            }

            log.warn( "Cannot find details view due to null id.  Redirecting to overview" );
            return new ModelAndView(
                    new RedirectView( "http://" + request.getServerName() + ":" + request.getServerPort()
                            + request.getContextPath() + "/expressionExperiment/showAllExpressionExperiments.html" ) );
        }

        ModelAndView mav = super.processFormSubmission( request, response, command, errors );

        Set<Entry<QuantitationType, Integer>> s = expressionExperimentService.getQuantitationTypeCountById( id )
                .entrySet();
        mav.addObject( "qtCountSet", s );

        // add count of designElementDataVectors
        mav.addObject( "designElementDataVectorCount",
                new Long( expressionExperimentService.getDesignElementDataVectorCountById( id ) ) );
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

    /**
     * @param expressionExperimentService
     */
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

    /**
     * @param request
     * @return Object
     * @throws ServletException
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) {
        Long id = null;
        try {
            id = Long.parseLong( request.getParameter( "id" ) );
        } catch ( NumberFormatException e ) {
            saveMessage( request, "Id was not a number " + id );
            throw new IllegalArgumentException( "Id was not a number " + id );
        }

        List<QuantitationTypeValueObject> qts = new ArrayList<>();
        log.debug( id );
        ExpressionExperimentEditValueObject obj;

        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Could not load experiment with id=" + id );
        }

        ee = expressionExperimentService.thawLite( ee );

        qts.addAll( QuantitationTypeValueObject
                .convert2ValueObjects( expressionExperimentService.getQuantitationTypes( ee ) ) );

        obj = new ExpressionExperimentEditValueObject( expressionExperimentService.loadValueObject( id ) );

        obj.setQuantitationTypes( qts );
        obj.setBioAssays( BioAssayValueObject.convert2ValueObjects( ee.getBioAssays() ) );

        this.saveMessage( request, "Editing dataset" );

        return obj;
    }

    /**
     * @param request
     * @return Map
     */
    @Override
    protected Map referenceData( HttpServletRequest request ) {
        Map<Object, Object> referenceData = new HashMap<Object, Object>();
        Collection<ExternalDatabase> edCol = externalDatabaseService.loadAll();

        Collection<ExternalDatabase> keepers = new HashSet<ExternalDatabase>();
        for ( ExternalDatabase database : edCol ) {
            if ( database.getType() == null ) continue;
            if ( database.getType().equals( DatabaseType.EXPRESSION ) ) {
                keepers.add( database );
            }
        }

        referenceData.put( "externalDatabases", keepers );

        referenceData.put( "standardQuantitationTypes", new ArrayList<String>( StandardQuantitationType.literals() ) );
        referenceData.put( "scaleTypes", new ArrayList<String>( ScaleType.literals() ) );
        referenceData.put( "generalQuantitationTypes", new ArrayList<String>( GeneralType.literals() ) );
        referenceData.put( "representations", new ArrayList<String>( PrimitiveType.literals() ) );
        return referenceData;
    }

    /**
     * @param arrayDesign
     */
    private void audit( ExpressionExperiment ee, AuditEventType eventType, String note ) {
        auditTrailService.addUpdateEvent( ee, eventType, note );
    }

    private String scrub( String s ) {
        return StringEscapeUtils.escapeHtml4( s );
    }

    /**
     * Change the relationship between bioassays and biomaterials.
     * 
     * @param request
     * @param expressionExperiment
     * @return true if there were changes
     */
    private boolean updateBioMaterialMap( HttpServletRequest request, ExpressionExperiment expressionExperiment ) {
        // parse JSON-serialized map
        String jsonSerialization = request.getParameter( "assayToMaterialMap" );
        // convert back to a map

        Map<String, JSONValue> bioAssayMap = null;
        try (StringInputStream aStream = new StringInputStream( jsonSerialization );) {
            JSONParser parser = new JSONParser( aStream );
            bioAssayMap = ( ( JSONObject ) parser.nextValue() ).getValue();
        } catch ( IOException | ANTLRException e ) {
            throw new RuntimeException( e );
        }

        Map<BioAssay, BioMaterial> deleteAssociations = new HashMap<>();
        Set<Entry<String, JSONValue>> bioAssays = bioAssayMap.entrySet();

        boolean anyChanges = false;

        int newBioMaterialCount = 0;

        // Map<Long, BioAssay> baMap = EntityUtils.getIdMap( expressionExperiment.getBioAssays() );

        for ( Entry<String, JSONValue> entry : bioAssays ) {
            // check if the bioAssayId is a nullElement
            // if it is, skip over this entry
            if ( entry.getKey().equalsIgnoreCase( "nullElement" ) ) {
                continue;
            }
            Long bioAssayId = Long.parseLong( entry.getKey() );

            JSONValue value = entry.getValue();

            Long newMaterialId = null;
            if ( value.isString() ) {
                newMaterialId = Long.parseLong( ( ( JSONString ) value ).getValue() );
            } else {
                newMaterialId = ( ( JSONInteger ) value ).getValue().longValue();
            }

            BioAssay bioAssay = bioAssayService.load( bioAssayId ); // maybe we need to do
            // this load to avoid stale data?

            if ( bioAssay == null ) {
                throw new IllegalArgumentException(
                        "Bioassay with id=" + bioAssayId + " was not associated with the experiment" );
            }

            BioMaterial currentBioMaterial = bioAssay.getSampleUsed();

            if ( newMaterialId.equals( currentBioMaterial.getId() ) ) {
                // / no change
                continue;
            }

            BioMaterial newMaterial;
            if ( newMaterialId < 0 ) { // This kludge signifies that it is a 'brand new' biomaterial.
                BioMaterial oldBioMaterial = currentBioMaterial;
                newMaterial = bioMaterialService.copy( oldBioMaterial );
                newMaterial.setName( "Modeled after " + oldBioMaterial.getName() );
                newMaterial.getFactorValues().clear();
                newMaterial = ( BioMaterial ) persisterHelper.persist( newMaterial );
                newBioMaterialCount++;
            } else {
                // FIXME can we just use this from the experiment, probably no need to fetch it again.
                newMaterial = bioMaterialService.load( newMaterialId );
                if ( newMaterial == null ) {
                    throw new IllegalArgumentException(
                            "BioMaterial with id=" + newMaterialId + " could not be loaded" );
                }
            }
            anyChanges = true;
            log.info( "Associating " + bioAssay + " with " + newMaterial );
            bioAssayService.addBioMaterialAssociation( bioAssay, newMaterial );
        }

        if ( anyChanges ) {
            /*
             * FIXME Decide if we need to delete the biomaterial -> factor value associations, it could be completely
             * fouled up.
             */
            log.info( "There were changes to the BioMaterial -> BioAssay map" );
            audit( expressionExperiment, BioMaterialMappingUpdate.Factory.newInstance(),
                    newBioMaterialCount + " biomaterials" ); // remove unnecessary biomaterial associations
            Collection<BioAssay> deleteKeys = deleteAssociations.keySet();
            for ( BioAssay assay : deleteKeys ) {
                /*
                 * BUG: if this fails, we end up with a useless extra biomaterial associated with the bioassay.
                 */
                bioAssayService.removeBioMaterialAssociation( assay, deleteAssociations.get( assay ) );
            }
        } else {
            log.info( "There were no changes to the BioMaterial -> BioAssay map" );

        }

        return anyChanges;
    }

    /**
     * Check old vs. new quantitation types, and update any affected data vectors.
     * 
     * @param request
     * @param expressionExperiment
     * @param updatedQuantitationTypes
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
                    GeneralType newgentype = GeneralType.fromString( newQtype.getGeneralType() );
                    boolean newisBkg = newQtype.getIsBackground();
                    boolean newisBkgSub = newQtype.getIsBackgroundSubtracted();
                    boolean newisNormalized = newQtype.getIsNormalized();
                    PrimitiveType newrep = PrimitiveType.fromString( newQtype.getRepresentation() );
                    ScaleType newscale = ScaleType.fromString( newQtype.getScale() );
                    StandardQuantitationType newType = StandardQuantitationType.fromString( newQtype.getType() );
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
                    revisedType.setDescription( scrub( newDescription ) );
                    revisedType.setName( scrub( newName ) );
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
                    qType.setDescription( scrub( newDescription ) );
                    qType.setName( scrub( newName ) );
                    qType.setIsNormalized( newisNormalized );
                    qType.setIsBatchCorrected( newIsBatchCorrected );
                    qType.setIsRecomputedFromRawData( newIsRecomputedFromRawData );

                    if ( newName != null && ( oldName == null || !oldName.equals( newName ) ) ) {
                        dirty = true;
                    }
                    if ( newDescription != null
                            && ( oldDescription == null || !oldDescription.equals( newDescription ) ) ) {
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

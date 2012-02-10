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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.tools.ant.filters.StringInputStream;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.ContactService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.BioMaterialMappingUpdate;
import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.Persister;
import ubic.gemma.web.controller.BaseFormController;
import antlr.RecognitionException;
import antlr.TokenStreamException;

import com.sdicons.json.model.JSONArray;
import com.sdicons.json.model.JSONInteger;
import com.sdicons.json.model.JSONObject;
import com.sdicons.json.model.JSONString;
import com.sdicons.json.model.JSONValue;
import com.sdicons.json.parser.JSONParser;

/**
 * Handle editing of expression experiments.
 * 
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentFormController extends BaseFormController {

    ExpressionExperimentService expressionExperimentService = null;
    ContactService contactService = null;
    BioAssayService bioAssayService = null;
    BioMaterialService bioMaterialService = null;
    BibliographicReferenceService bibliographicReferenceService = null;
    Persister persisterHelper = null;
    QuantitationTypeService quantitationTypeService;
    AuditTrailService auditTrailService;

    private ExternalDatabaseService externalDatabaseService = null;

    public ExpressionExperimentFormController() {
        /*
         * if true, reuses the same command object across the edit-submit-process (get-post-process).
         */
        setSessionForm( true );
        setCommandClass( ExpressionExperiment.class );
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

        ExpressionExperimentEditCommand eeCommand = ( ExpressionExperimentEditCommand ) command;
        ExpressionExperiment expressionExperiment = expressionExperimentService.load( eeCommand.getId() );

        if ( expressionExperiment == null ) {
            throw new IllegalArgumentException( "Could not load experiment" );
        }

        expressionExperiment = expressionExperimentService.thawLite( expressionExperiment );

        /**
         * Much more complicated
         */
        updateQuantTypes( request, expressionExperiment, eeCommand.getQuantitationTypes() );

        updateBioMaterialMap( request, expressionExperiment );

        return new ModelAndView( new RedirectView( "http://" + request.getServerName() + ":" + request.getServerPort()
                + request.getContextPath() + "/expressionExperiment/showExpressionExperiment.html?id="
                + eeCommand.getId() ) );
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
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {

        log.debug( "entering processFormSubmission" );

        Long id = ( ( ExpressionExperimentEditCommand ) command ).getId();

        if ( request.getParameter( "cancel" ) != null ) {
            if ( id != null ) {
                return new ModelAndView( new RedirectView( "http://" + request.getServerName() + ":"
                        + request.getServerPort() + request.getContextPath()
                        + "/expressionExperiment/showExpressionExperiment.html?id=" + id ) );
            }

            log.warn( "Cannot find details view due to null id.  Redirecting to overview" );
            return new ModelAndView( new RedirectView( "http://" + request.getServerName() + ":"
                    + request.getServerPort() + request.getContextPath()
                    + "/expressionExperiment/showAllExpressionExperiments.html" ) );
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
     * @param bibliographicReferenceService the bibliographicReferenceService to set
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
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
     * @param contactService
     */
    public void setContactService( ContactService contactService ) {
        this.contactService = contactService;
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

        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        List<QuantitationType> qts = new ArrayList<QuantitationType>();
        log.debug( id );
        ExpressionExperimentEditCommand obj;
        if ( id != null ) {
            ee = expressionExperimentService.load( id );
            ee = expressionExperimentService.thawLite( ee );
            qts.addAll( expressionExperimentService.getQuantitationTypes( ee ) );
            obj = new ExpressionExperimentEditCommand( ee, qts );
        } else {
            obj = new ExpressionExperimentEditCommand( ee, qts );
        }

        if ( ee.getId() != null ) {
            this.saveMessage( request, "Editing dataset" );
        }

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
        return StringEscapeUtils.escapeHtml( s );
    }

    /**
     * Change the relationship between bioassays and biomaterials.
     * 
     * @param request
     * @param expressionExperiment
     */
    private void updateBioMaterialMap( HttpServletRequest request, ExpressionExperiment expressionExperiment ) {
        // parse JSON-serialized map
        String jsonSerialization = request.getParameter( "assayToMaterialMap" );
        // convert back to a map
        JSONParser parser = new JSONParser( new StringInputStream( jsonSerialization ) );

        Map<String, JSONValue> bioAssayMap = null;
        try {
            bioAssayMap = ( ( JSONObject ) parser.nextValue() ).getValue();
        } catch ( TokenStreamException e ) {
            throw new RuntimeException( e );
        } catch ( RecognitionException e ) {
            throw new RuntimeException( e );
        }

        Map<BioAssay, BioMaterial> deleteAssociations = new HashMap<BioAssay, BioMaterial>();
        // set the bioMaterial - bioAssay associations if they are different
        Set<Entry<String, JSONValue>> bioAssays = bioAssayMap.entrySet();

        boolean anyChanges = false;

        int newBioMaterialCount = 0;

        for ( Entry<String, JSONValue> entry : bioAssays ) {
            // check if the bioAssayId is a nullElement
            // if it is, skip over this entry
            if ( entry.getKey().equalsIgnoreCase( "nullElement" ) ) {
                continue;
            }
            Long bioAssayId = Long.parseLong( entry.getKey() );

            Collection<JSONValue> bioMaterialValues = ( ( JSONArray ) entry.getValue() ).getValue();
            Collection<Long> newBioMaterials = new ArrayList<Long>();
            for ( JSONValue value : bioMaterialValues ) {
                if ( value.isString() ) {
                    Long newMaterial = Long.parseLong( ( ( JSONString ) value ).getValue() );
                    newBioMaterials.add( newMaterial );
                } else {
                    Long newMaterial = ( ( JSONInteger ) value ).getValue().longValue();
                    newBioMaterials.add( newMaterial );
                }
            }

            newBioMaterialCount = newBioMaterials.size();

            BioAssay bioAssay = bioAssayService.load( bioAssayId );
            bioAssayService.thaw( bioAssay );
            Collection<BioMaterial> bMats = bioAssay.getSamplesUsed();
            Collection<Long> oldBioMaterials = new ArrayList<Long>();
            for ( BioMaterial material : bMats ) {
                oldBioMaterials.add( material.getId() );
            }

            // try to find the bioMaterials in the list of current samples
            // if it is not in the current samples, add it
            // if it is in the current sample list, skip to next entry
            // if the current sample does not exist in the new bioMaterial list, remove it
            for ( Long newBioMaterialId : newBioMaterials ) {
                if ( oldBioMaterials.contains( newBioMaterialId ) ) {
                    continue;
                }
                BioMaterial newMaterial;
                if ( newBioMaterialId < 0 ) { // This kludge signifies that it is a 'brand new' biomaterial.
                    // model the new biomaterial after the old one for the bioassay (we're taking a guess here.)
                    if ( bMats.size() > 1 ) {
                        // log.warn("");
                    }

                    BioMaterial oldBioMaterial = bMats.iterator().next();
                    newMaterial = bioMaterialService.copy( oldBioMaterial );
                    newMaterial.setName( "Modeled after " + oldBioMaterial.getName() );
                    newMaterial.getFactorValues().clear();
                    newMaterial = ( BioMaterial ) persisterHelper.persist( newMaterial );
                } else {
                    newMaterial = bioMaterialService.load( newBioMaterialId );
                }
                anyChanges = true;
                bioAssayService.addBioMaterialAssociation( bioAssay, newMaterial );

            }

            // put all unnecessary associations in a collection
            // they are not deleted immediately to let all new associations be added first
            // before any deletions are made. This makes sure that
            // no bioMaterials are removed unnecessarily
            for ( Long oldBioMaterialId : oldBioMaterials ) {
                if ( newBioMaterials.contains( oldBioMaterialId ) ) {
                    continue;
                }
                BioMaterial oldMaterial = bioMaterialService.load( oldBioMaterialId );
                deleteAssociations.put( bioAssay, oldMaterial );

            }

        }

        if ( anyChanges ) {
            /*
             * TODO Decide if we need to delete the biomaterial -> factor value associations, it could be completely
             * fouled up.
             */
            log.info( "There were changes to the BioMaterial -> BioAssay map" );
        } else {
            log.info( "There were NO changes to the BioMaterial -> BioAssay map" );
        }

        // remove unnecessary biomaterial associations
        Collection<BioAssay> deleteKeys = deleteAssociations.keySet();
        for ( BioAssay assay : deleteKeys ) {
            /*
             * BUG: if this fails, we end up with a useless extra biomaterial associated with the bioassay.
             */
            bioAssayService.removeBioMaterialAssociation( assay, deleteAssociations.get( assay ) );
        }

        audit( expressionExperiment, BioMaterialMappingUpdate.Factory.newInstance(), newBioMaterialCount
                + " biomaterials" );
    }

    /**
     * Check old vs. new quantitation types, and update any affected data vectors.
     * 
     * @param request
     * @param expressionExperiment
     * @param updatedQuantitationTypes
     */
    private void updateQuantTypes( HttpServletRequest request, ExpressionExperiment expressionExperiment,
            Collection<QuantitationType> updatedQuantitationTypes ) {

        Collection<QuantitationType> oldQuantitationTypes = expressionExperimentService
                .getQuantitationTypes( expressionExperiment );

        for ( QuantitationType qType : oldQuantitationTypes ) {
            assert qType != null;
            Long id = qType.getId();
            boolean dirty = false;
            QuantitationType revisedType = QuantitationType.Factory.newInstance();
            for ( QuantitationType newQtype : updatedQuantitationTypes ) {
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

                    String newName = newQtype.getName();
                    String newDescription = newQtype.getDescription();
                    GeneralType newgentype = newQtype.getGeneralType();
                    boolean newisBkg = newQtype.getIsBackground();
                    boolean newisBkgSub = newQtype.getIsBackgroundSubtracted();
                    boolean newisNormalized = newQtype.getIsNormalized();
                    PrimitiveType newrep = newQtype.getRepresentation();
                    ScaleType newscale = newQtype.getScale();
                    StandardQuantitationType newType = newQtype.getType();
                    boolean newisPreferred = newQtype.getIsPreferred();
                    boolean newIsmaskedPreferred = newQtype.getIsMaskedPreferred();
                    boolean newisRatio = newQtype.getIsRatio();

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

                    break;
                }
            }
            if ( dirty ) {
                // update the quantitation type
                quantitationTypeService.update( qType );
            }
        }
    }

}

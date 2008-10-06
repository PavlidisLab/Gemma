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
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.filters.StringInputStream;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.xml.sax.SAXException;

import ubic.gemma.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.model.common.auditAndSecurity.ContactService;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.model.common.description.DatabaseEntry;
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
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.PersisterHelper;
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
 * @spring.bean id="expressionExperimentFormController"
 * @spring.property name = "commandName" value="expressionExperiment"
 * @spring.property name="commandClass"
 *                  value="ubic.gemma.web.controller.expression.experiment.ExpressionExperimentEditCommand"
 * @spring.property name = "formView" value="expressionExperiment.edit"
 * @spring.property name = "successView" value="redirect:/expressionExperiment/showAllExpressionExperiments.html"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "bioAssayService" ref="bioAssayService"
 * @spring.property name = "bioMaterialService" ref="bioMaterialService"
 * @spring.property name = "contactService" ref="contactService"
 * @spring.property name = "externalDatabaseService" ref="externalDatabaseService"
 * @spring.property name = "bibliographicReferenceService" ref="bibliographicReferenceService"
 * @spring.property name = "persisterHelper" ref="persisterHelper"
 * @spring.property name = "validator" ref="expressionExperimentValidator"
 * @spring.property name = "quantitationTypeService" ref="quantitationTypeService"
 * @spring.property name = "designElementDataVectorService" ref="designElementDataVectorService"
 */
public class ExpressionExperimentFormController extends BaseFormController {
    private static Log log = LogFactory.getLog( ExpressionExperimentFormController.class.getName() );

    ExpressionExperimentService expressionExperimentService = null;
    ContactService contactService = null;
    BioAssayService bioAssayService = null;
    BioMaterialService bioMaterialService = null;
    BibliographicReferenceService bibliographicReferenceService = null;
    PersisterHelper persisterHelper = null;
    QuantitationTypeService quantitationTypeService;
    DesignElementDataVectorService designElementDataVectorService;

    private ExternalDatabaseService externalDatabaseService = null;

    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
    }

    /**
     * @param persisterHelper the persisterHelper to set
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * @param bibliographicReferenceService the bibliographicReferenceService to set
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

    public ExpressionExperimentFormController() {
        /*
         * if true, reuses the same command object across the edit-submit-process (get-post-process).
         */
        setSessionForm( true );
        setCommandClass( ExpressionExperiment.class );
    }

    /**
     * @param request
     * @return Object
     * @throws ServletException
     */
    @SuppressWarnings("unchecked")
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

        Set s = expressionExperimentService.getQuantitationTypeCountById( id ).entrySet();
        mav.addObject( "qtCountSet", s );

        // add count of designElementDataVectors
        mav.addObject( "designElementDataVectorCount", new Long( expressionExperimentService
                .getDesignElementDataVectorCountById( id ) ) );
        return mav;
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

        log.debug( "entering onSubmit" );

        ExpressionExperimentEditCommand eeCommand = ( ExpressionExperimentEditCommand ) command;

        if ( eeCommand == null || eeCommand.getId() == null ) {
            errors.addError( new ObjectError( command.toString(), null, null,
                    "Expression experiment was null or had null id" ) );
            return processFormSubmission( request, response, command, errors );
        }

        ExpressionExperiment expressionExperiment = eeCommand.toExpressionExperiment();
        // create bibliographicReference if necessary

        updatePubMed( request, expressionExperiment );

        /**
         * Takes care of the basics.
         */
        expressionExperimentService.update( expressionExperiment );

        /**
         * Much more complicated
         */
        updateQuantTypes( request, expressionExperiment, eeCommand.getQuantitationTypes() );

        updateBioMaterialMap( request );

        updateAccession( request, expressionExperiment );

        // saveMessage( request, "object.saved", new Object[] { expressionExperiment.getClass().getSimpleName(),
        // expressionExperiment.getId() }, "Saved" );

        return new ModelAndView( new RedirectView( "http://" + request.getServerName() + ":" + request.getServerPort()
                + request.getContextPath() + "/expressionExperiment/showExpressionExperiment.html?id="
                + eeCommand.getId() ) );
    }

    /**
     * @param request
     * @param expressionExperiment
     */
    private void updateAccession( HttpServletRequest request, ExpressionExperiment expressionExperiment ) {
        String accession = request.getParameter( "expressionExperiment.accession.accession" );

        if ( accession == null ) {
            // do nothing
        } else if ( expressionExperiment.getAccession() != null ) {
            /* database entry */
            expressionExperiment.getAccession().setAccession( accession );

            /* external database */
            ExternalDatabase ed = ( expressionExperiment.getAccession().getExternalDatabase() );
            ed = externalDatabaseService.findOrCreate( ed );
            expressionExperiment.getAccession().setExternalDatabase( ed );
        }
    }

    /**
     * @param request
     * @throws TokenStreamException
     * @throws RecognitionException
     */
    private void updateBioMaterialMap( HttpServletRequest request ) {
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

            BioAssay bioAssay = bioAssayService.load( bioAssayId );
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
                } else {
                    BioMaterial newMaterial;
                    if ( newBioMaterialId < 0 ) { // This kludge signifies that it is a 'brand new' biomaterial.
                        // model the new biomaterial after the old one for the bioassay (we're taking a guess here.)
                        if ( bMats.size() > 1 ) {
                            // log.warn("");
                        }

                        BioMaterial oldBioMaterial = bMats.iterator().next();
                        // newMaterial = BioMaterial.Factory.newInstance();
                        newMaterial = bioMaterialService.copy( oldBioMaterial );
                        // newMaterial.setDescription( oldBioMaterial.getDescription() + " [Created by Gemma]" );
                        // newMaterial.setMaterialType( oldBioMaterial.getMaterialType() );
                        // newMaterial.setCharacteristics( oldBioMaterial.getCharacteristics() );
                        // newMaterial.setTreatments( oldBioMaterial.getTreatments() );
                        // newMaterial.setSourceTaxon( oldBioMaterial.getSourceTaxon() );
                        // newMaterial.setFactorValues( oldBioMaterial.getFactorValues() );
                        newMaterial.setName( "Modeled after " + oldBioMaterial.getName() );
                        newMaterial = ( BioMaterial ) persisterHelper.persist( newMaterial );
                    } else {
                        newMaterial = bioMaterialService.load( newBioMaterialId );
                    }

                    bioAssayService.addBioMaterialAssociation( bioAssay, newMaterial );

                }
            }

            // put all unnecessary associations in a collection
            // they are not deleted immediately to let all new associations be added first
            // before any deletions are made. This makes sure that
            // no bioMaterials are removed unnecessarily
            for ( Long oldBioMaterialId : oldBioMaterials ) {
                if ( newBioMaterials.contains( oldBioMaterialId ) ) {
                    continue;
                } else {
                    BioMaterial oldMaterial = bioMaterialService.load( oldBioMaterialId );
                    deleteAssociations.put( bioAssay, oldMaterial );
                }
            }

        }

        // remove unnecessary biomaterial associations
        Collection<BioAssay> deleteKeys = deleteAssociations.keySet();
        for ( BioAssay assay : deleteKeys ) {
            bioAssayService.removeBioMaterialAssociation( assay, deleteAssociations.get( assay ) );
        }
    }

    /**
     * Check old vs. new quantitation types, and update any affected data vectors.
     * 
     * @param request
     * @param expressionExperiment
     * @param updatedQuantitationTypes
     */
    @SuppressWarnings("unchecked")
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
                    revisedType.setDescription( newDescription );
                    revisedType.setName( newName );
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
                    qType.setDescription( newDescription );
                    qType.setName( newName );
                    qType.setIsNormalized( newisNormalized );

                    if ( !oldName.equals( newName ) ) {
                        dirty = true;
                    }
                    if ( !oldDescription.equals( newDescription ) ) {
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

    /**
     * @param request
     * @param command
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    private void updatePubMed( HttpServletRequest request, ExpressionExperiment command ) throws IOException,
            SAXException, ParserConfigurationException {
        String pubMedId = request.getParameter( "expressionExperiment.PubMedId" );
        if ( StringUtils.isBlank( pubMedId ) ) {
            return;
        }
        // first, search for the pubMedId in the database
        // if it is in the database, then just point the EE to that
        // if it doesn't, then grab the BibliographicReference from PubMed and persist. Then point EE to the new
        // entry.
        BibliographicReference publication = bibliographicReferenceService.findByExternalId( pubMedId );
        if ( publication != null ) {
            command.setPrimaryPublication( publication );
        } else {
            // search for pubmedId
            PubMedSearch pms = new PubMedSearch();
            Collection<String> searchTerms = new ArrayList<String>();
            searchTerms.add( pubMedId );
            Collection<BibliographicReference> publications = pms.searchAndRetrieveIdByHTTP( searchTerms );
            // check to see if there are publications found
            // if there are none, or more than one, add an error message and do nothing
            if ( publications.size() == 0 ) {
                this.saveMessage( request, "Cannot find PubMed ID " + pubMedId );
            } else if ( publications.size() > 1 ) {
                this.saveMessage( request, "PubMed ID " + pubMedId + "" );
            } else {
                publication = publications.iterator().next();

                DatabaseEntry pubAccession = DatabaseEntry.Factory.newInstance();
                pubAccession.setAccession( pubMedId );
                ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
                ed.setName( "PubMed" );
                pubAccession.setExternalDatabase( ed );

                publication.setPubAccession( pubAccession );

                // persist new publication
                publication = ( BibliographicReference ) persisterHelper.persist( publication );
                // publication = bibliographicReferenceService.findOrCreate( publication );
                // assign to expressionExperiment
                command.setPrimaryPublication( publication );
            }
        }

    }

    /**
     * @param request
     * @return Map
     */
    @SuppressWarnings("unchecked")
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
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param contactService
     */
    public void setContactService( ContactService contactService ) {
        this.contactService = contactService;
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

    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }

    public void setQuantitationTypeService( QuantitationTypeService quantitationTypeService ) {
        this.quantitationTypeService = quantitationTypeService;
    }
}

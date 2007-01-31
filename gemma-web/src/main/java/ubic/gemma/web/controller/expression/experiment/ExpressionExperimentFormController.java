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
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseDao;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.web.controller.BaseFormController;

import com.sdicons.json.model.JSONArray;
import com.sdicons.json.model.JSONInteger;
import com.sdicons.json.model.JSONObject;
import com.sdicons.json.model.JSONString;
import com.sdicons.json.model.JSONValue;
import com.sdicons.json.parser.JSONParser;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="expressionExperimentFormController"
 * @spring.property name = "commandName" value="expressionExperiment"
 * @spring.property name = "formView" value="expressionExperiment.edit"
 * @spring.property name = "successView" value="redirect:/expressionExperiment/showAllExpressionExperiments.html"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "bioAssayService" ref="bioAssayService"
 * @spring.property name = "bioMaterialService" ref="bioMaterialService"
 * @spring.property name = "contactService" ref="contactService"
 * @spring.property name = "externalDatabaseDao" ref="externalDatabaseDao"
 * @spring.property name = "bibliographicReferenceService" ref="bibliographicReferenceService"
 * @spring.property name = "persisterHelper" ref="persisterHelper"
 * @spring.property name = "validator" ref="expressionExperimentValidator"
 */
public class ExpressionExperimentFormController extends BaseFormController {
    private static Log log = LogFactory.getLog( ExpressionExperimentFormController.class.getName() );

    ExpressionExperimentService expressionExperimentService = null;
    ContactService contactService = null;
    BioAssayService bioAssayService = null;
    BioMaterialService bioMaterialService = null;
    BibliographicReferenceService bibliographicReferenceService = null;
    PersisterHelper persisterHelper = null;

    private Long id = null;

    private ExternalDatabaseDao externalDatabaseDao = null;

    // FIXME Use ExternalDatabaseService instead of ExternalDatabaseDao. Methods have been put in model for service
    // but when I use them I get NonUniqueObjectException. This seems to be documented here:
    // http://saloon.javaranch.com/cgi-bin/ubb/ultimatebb.cgi?ubb=get_topic&f=78&t=000475.
    // It works if you call the dao layer directly from your controller (I know we are not supposed to do this, but it
    // works).
    // Will fix this later.



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
    @Override
    protected Object formBackingObject( HttpServletRequest request ) {

        id = Long.parseLong( request.getParameter( "id" ) );

        ExpressionExperiment ee = null;

        log.debug( id );

        if ( !"".equals( id ) )
            ee = expressionExperimentService.findById( id );

        else
            ee = ExpressionExperiment.Factory.newInstance();

        saveMessage( request, "object.editing", new Object[] { ee.getClass().getSimpleName(), ee.getId() }, "Editing" );

        return ee;
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

        id = ( ( ExpressionExperiment ) command ).getId();

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
    @SuppressWarnings("unused")
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        log.debug( "entering onSubmit" );

        ExpressionExperiment ee = ( ExpressionExperiment ) command;

        if (ee == null || ee.getId() == null ) {
            errors.addError( new ObjectError( command.toString(), null, null, "Expression experiment was null or had null id" ) );
            return processFormSubmission( request, response, command, errors );
        }

        expressionExperimentService.update( ( ExpressionExperiment ) command ); 

        // parse JSON-serialized map
        String jsonSerialization = request.getParameter( "assayToMaterialMap" );
        // convert back to a map
        JSONParser parser = new JSONParser(new StringInputStream(jsonSerialization));

        Map<String, JSONValue> bioAssayMap = ((JSONObject) parser.nextValue()).getValue();
 
        Map<BioAssay,BioMaterial> deleteAssociations = new HashMap<BioAssay,BioMaterial>();
        // set the bioMaterial - bioAssay associations if they are different
        Set<Entry<String,JSONValue>> bioAssays = bioAssayMap.entrySet();
        for ( Entry<String, JSONValue> entry : bioAssays ) {
            // check if the bioAssayId is a nullElement
            // if it is, skip over this entry
            if (entry.getKey().equalsIgnoreCase( "nullElement" )) {
                continue;
            }
            Long bioAssayId = Long.parseLong(entry.getKey());

            Collection<JSONValue> bioMaterialValues =  ((JSONArray)entry.getValue()).getValue();
            Collection<Long> newBioMaterials = new ArrayList<Long>();
            for ( JSONValue value : bioMaterialValues ) {
                if (value.isString()) {
                    Long newMaterial = Long.parseLong(( (JSONString)value).getValue());
                    newBioMaterials.add( newMaterial );
                }
                else {
                    Long newMaterial = ((JSONInteger)value).getValue().longValue();
                    newBioMaterials.add( newMaterial );               
                }
            }
            
            BioAssay bioAssay = bioAssayService.findById( bioAssayId );
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
                if (oldBioMaterials.contains( newBioMaterialId )) {
                    continue;
                }
                else {
                    BioMaterial newMaterial = bioMaterialService.findById( newBioMaterialId );
                    bioAssayService.addBioMaterialAssociation( bioAssay, newMaterial );
                }
            }
            
            
            // put all unnecessary associations in a collection
            // they are not deleted immediately to let all new associations be added first
            // before any deletions are made. This makes sure that
            // no bioMaterials are removed unnecessarily
            for ( Long oldBioMaterialId : oldBioMaterials ) {
                if (newBioMaterials.contains( oldBioMaterialId )) {
                    continue;
                }
                else {
                    BioMaterial oldMaterial = bioMaterialService.findById( oldBioMaterialId );
                    deleteAssociations.put( bioAssay,oldMaterial );
                }
            }

        }
        
        // remove unnecessary biomaterial associations
        Collection<BioAssay> deleteKeys = deleteAssociations.keySet();
        for ( BioAssay assay : deleteKeys ) {
            bioAssayService.removeBioMaterialAssociation( assay, deleteAssociations.get( assay ) );
        }
        String accession = request.getParameter( "expressionExperiment.accession.accession" );

        
        if ( accession == null ) {
            // do nothing
        } else {
            /* database entry */
            ( ( ExpressionExperiment ) command ).getAccession().setAccession( accession );

            /* external database */
            ExternalDatabase ed = ( ( ( ExpressionExperiment ) command ).getAccession().getExternalDatabase() );
            ed = externalDatabaseDao.findOrCreate( ed );
            ( ( ExpressionExperiment ) command ).getAccession().setExternalDatabase( ed );
        }

        // create bibliographicReference if necessary
        String pubMedId = request.getParameter( "expressionExperiment.PubMedId" );
        updatePubMed( request, (ExpressionExperiment)command, pubMedId );

        saveMessage( request, "object.saved", new Object[] { ee.getClass().getSimpleName(), ee.getId() }, "Saved" );

        
        return new ModelAndView( new RedirectView( "http://" + request.getServerName() + ":"
                + request.getServerPort() + request.getContextPath()
                + "/expressionExperiment/showExpressionExperiment.html?id=" + id ) );
    }

    /**
     * 
     * @param request
     * @param command
     * @param pubMedId
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    private void updatePubMed( HttpServletRequest request, ExpressionExperiment command, String pubMedId ) throws IOException, SAXException, ParserConfigurationException {
        if (StringUtils.isBlank( pubMedId )) {
            // do nothing
        }
        else {
            // first, search for the pubMedId in the database
            // if it is in the database, then just point the EE to that
            // if it doesn't, then grab the BibliographicReference from PubMed and persist. Then point EE to the new entry.
            BibliographicReference publication = bibliographicReferenceService.findByExternalId( pubMedId );
            if (publication != null) {
                ( ( ExpressionExperiment ) command ).setPrimaryPublication( publication );
            }
            else {
                // search for pubmedId
                PubMedSearch pms = new PubMedSearch();
                Collection<String> searchTerms = new ArrayList<String>();
                searchTerms.add( pubMedId );
                Collection<BibliographicReference> publications = pms.searchAndRetrieveIdByHTTP( searchTerms );
                // check to see if there are publications found
                // if there are none, or more than one, add an error message and do nothing
                if (publications.size() == 0) {
                    this.saveMessage( request, "Cannot find PubMed ID " + pubMedId );
                }
                else if (publications.size() > 1) {
                    this.saveMessage( request, "PubMed ID " + pubMedId + "");        
                }
                else {
                    publication = publications.iterator().next();         
                    
                    DatabaseEntry pubAccession = DatabaseEntry.Factory.newInstance();
                    pubAccession.setAccession( pubMedId );
                    ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
                    ed.setName( "PubMed" );
                    pubAccession.setExternalDatabase( ed );
                    
                    publication.setPubAccession( pubAccession );
                    
                    // persist new publication
                    publication = (BibliographicReference) persisterHelper.persist( publication );
                    //publication = bibliographicReferenceService.findOrCreate( publication );
                    // assign to expressionExperiment
                    ( ( ExpressionExperiment ) command ).setPrimaryPublication( publication );
                }
            }
        }
    }

    /**
     * @param request
     * @return Map
     */
    @Override
    @SuppressWarnings( { "unused", "unchecked" })
    protected Map referenceData( HttpServletRequest request ) {
        Collection<ExternalDatabase> edCol = externalDatabaseDao.loadAll();
        Map<String, Collection<ExternalDatabase>> edMap = new HashMap<String, Collection<ExternalDatabase>>();
        edMap.put( "externalDatabases", edCol );
        return edMap;
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
     * @param externalDatabaseDao
     */
    public void setExternalDatabaseDao( ExternalDatabaseDao externalDatabaseDao ) {
        this.externalDatabaseDao = externalDatabaseDao;
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
}

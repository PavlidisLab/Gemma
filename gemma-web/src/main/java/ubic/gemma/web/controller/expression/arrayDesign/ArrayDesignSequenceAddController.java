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
package ubic.gemma.web.controller.expression.arrayDesign;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceProcessingService;
import ubic.gemma.loader.genome.taxon.SupportedTaxa;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.controller.common.auditAndSecurity.FileUpload;
import ubic.gemma.web.propertyeditor.ArrayDesignPropertyEditor;
import ubic.gemma.web.propertyeditor.TaxonPropertyEditor;
import ubic.gemma.web.util.upload.CommonsMultipartFile;
import ubic.gemma.web.util.upload.FileUploadUtil;

/**
 * Controller for associating sequences with an existing arrayDesign.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="arrayDesignSequenceAddController"
 * @spring.property name="commandName" value="arrayDesignSequenceAddCommand"
 * @spring.property name="commandClass"
 *                  value="ubic.gemma.web.controller.expression.arrayDesign.ArrayDesignSequenceAddCommand"
 * @spring.property name="formView" value="arrayDesignSequenceAdd"
 * @spring.property name="successView" value="redirect:/arrayDesign/associateSequences.html"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="validator" ref="arrayDesignSequenceAddValidator"
 * @spring.property name="bioSequenceService" ref="bioSequenceService"
 * @spring.property name="taxonService" ref="taxonService"
 */
public class ArrayDesignSequenceAddController extends BaseFormController {

    TaxonService taxonService;

    ArrayDesignService arrayDesignService;

    BioSequenceService bioSequenceService;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(java.lang.Object,
     *      org.springframework.validation.BindException)
     */
    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {
        ArrayDesignSequenceAddCommand commandObject = ( ArrayDesignSequenceAddCommand ) command;
        FileUpload fileUpload = commandObject.getSequenceFile();

        ArrayDesign arrayDesign = commandObject.getArrayDesign();

        // should be done by validaiton.
        if ( arrayDesign == null ) {
            Object[] args = new Object[] { getText( "arrayDesignSequenceAddCommand.arrayDesign", request.getLocale() ) };
            errors.rejectValue( "arrayDesign", "errors.required", args, "Array Design is required" );
            return showForm( request, response, errors );
        }

        // validate type is okay

        SequenceType sequenceType = commandObject.getSequenceType();
        if ( sequenceType == null ) {
            errors.rejectValue( "sequenceType", "sequenceType.missing", "Sequence Type is missing" );
            return showForm( request, response, errors );
        }

        Taxon taxon = commandObject.getTaxon();
        // validate

        // validate array design has design elements.
        if ( sequenceType == SequenceType.AFFY_PROBE ) {
            if ( arrayDesignService.getReporterCount( arrayDesign ) == 0 ) {
                errors
                        .rejectValue( "arrayDesign", "arrayDesign.noreporters",
                                "Array design did not have any reporters" );
                return showForm( request, response, errors );
            }
        } else {
            if ( arrayDesignService.getCompositeSequenceCount( arrayDesign ) == 0 ) {
                errors.rejectValue( "arrayDesign", "arrayDesign.nocompositesequences",
                        "Array design did not have any compositesequences" );
                return showForm( request, response, errors );
            }
        }

        // TODO - possible additional validation to make sure array design is in right state for this processing.

        // validate a file was entered
        if ( fileUpload.getFile().length == 0 ) {
            Object[] args = new Object[] { getText( "arrayDesignSequenceAddCommand.file", request.getLocale() ) };
            errors.rejectValue( "file", "errors.required", args, "File" );

            return showForm( request, response, errors );
        }

        CommonsMultipartFile sequenceFile = FileUploadUtil.uploadFile( request, fileUpload, "sequenceFile.file" );

        // process it
        ArrayDesignSequenceProcessingService processor = new ArrayDesignSequenceProcessingService();

        Collection<BioSequence> bioSequences = processor.processArrayDesign( arrayDesign,
                sequenceFile.getInputStream(), sequenceType, taxon );

        Map<String, Object> model = new HashMap<String, Object>();
        model.put( "numSequencesProcessed", bioSequences.size() );

        // need to persist the bioSequences

        // then update the designelements.

        return new ModelAndView( this.getSuccessView(), model );

    }

    /**
     * @param arrayDesignService the arrayDesignService to set
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param bioSequenceService the bioSequenceService to set
     */
    public void setBioSequenceService( BioSequenceService bioSequenceService ) {
        this.bioSequenceService = bioSequenceService;
    }

    /**
     * @param taxonService the taxonService to set
     */
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        ArrayDesignSequenceAddCommand adsac = new ArrayDesignSequenceAddCommand();
        return adsac;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.BaseFormController#initBinder(javax.servlet.http.HttpServletRequest,
     *      org.springframework.web.bind.ServletRequestDataBinder)
     */
    @Override
    protected void initBinder( HttpServletRequest request, ServletRequestDataBinder binder ) {
        super.initBinder( request, binder );
        binder.registerCustomEditor( ArrayDesign.class, new ArrayDesignPropertyEditor( this.arrayDesignService ) );
        binder.registerCustomEditor( Taxon.class, new TaxonPropertyEditor( this.taxonService ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest,
     *      java.lang.Object, org.springframework.validation.Errors)
     */
    @SuppressWarnings( { "unchecked", "unused" })
    @Override
    protected Map referenceData( HttpServletRequest request ) throws Exception {

        Map<String, List<? extends Object>> mapping = new HashMap<String, List<? extends Object>>();

        List<ArrayDesign> arrayDesignNames = new ArrayList<ArrayDesign>();

        for ( ArrayDesign arrayDesign : ( Collection<ArrayDesign> ) arrayDesignService.loadAll() ) {
            arrayDesignNames.add( arrayDesign );
        }

        List<Taxon> taxonNames = new ArrayList<Taxon>();

        for ( Taxon taxon : ( Collection<Taxon> ) taxonService.loadAll() ) {
            if ( !SupportedTaxa.contains( taxon ) ) {
                continue;
            }
            taxonNames.add( taxon );
        }

        Collections.sort( arrayDesignNames, new Comparator() {
            public int compare( Object o1, Object o2 ) {
                return ( ( ArrayDesign ) o1 ).getName().compareTo( ( ( ArrayDesign ) o2 ).getName() );
            }
        } );
        Collections.sort( taxonNames, new Comparator() {
            public int compare( Object o1, Object o2 ) {
                return ( ( Taxon ) o1 ).getScientificName().compareTo( ( ( Taxon ) o2 ).getScientificName() );
            }
        } );

        mapping.put( "arrayDesigns", arrayDesignNames );

        mapping.put( "sequenceTypes", new ArrayList<String>( SequenceType.literals() ) );

        mapping.put( "taxa", taxonNames );

        return mapping;

    }

}

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

import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceProcessor;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.controller.common.auditAndSecurity.FileUpload;
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
 */
public class ArrayDesignSequenceAddController extends BaseFormController {

    public class ArrayDesignPropertyEditor extends PropertyEditorSupport {
        public String getAsText() {
            return ( ( ArrayDesign ) this.getValue() ).getName();
        }

        public void setAsText( String text ) throws IllegalArgumentException {
            if ( log.isDebugEnabled() ) log.debug( "Transforming " + text + " to an array design..." );
            Object ad = arrayDesignService.findArrayDesignByName( text );
            if ( ad == null ) {
                throw new IllegalArgumentException( "There is no array design with name=" + text );
            }
            this.setValue( ad );
        }
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
        binder.registerCustomEditor( ArrayDesign.class, new ArrayDesignPropertyEditor() );
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

        Map<String, List<String>> mapping = new HashMap<String, List<String>>();

        List<String> arrayDesignNames = new ArrayList<String>();

        for ( ArrayDesign arrayDesign : ( Collection<ArrayDesign> ) arrayDesignService.getAllArrayDesigns() ) {
            arrayDesignNames.add( arrayDesign.getName() );
        }

        Collections.sort( arrayDesignNames );

        mapping.put( "arrayDesigns", arrayDesignNames );

        mapping.put( "sequenceTypes", new ArrayList<String>( SequenceType.literals() ) );

        return mapping;

    }

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
        ArrayDesignSequenceProcessor processor = new ArrayDesignSequenceProcessor();

        processor.processArrayDesign( arrayDesign, sequenceFile.getInputStream(), sequenceType );

        // FIXME put the number of processed sequences in the request so we can display it

        return new ModelAndView( this.getSuccessView() );

    }

    ArrayDesignService arrayDesignService;

    BioSequenceService bioSequenceService;

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

}

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

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.web.controller.util.MessageUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Controller for editing basic information about array designs.
 *
 * @author keshav
 */
@CommonsLog
public class ArrayDesignFormController extends SimpleFormController {

    @Setter
    private ArrayDesignService arrayDesignService;
    @Setter
    private MessageUtil messageUtil;

    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {
        ArrayDesignValueObject ad = ( ArrayDesignValueObject ) command;

        ArrayDesign existing = arrayDesignService.load( ad.getId() );

        if ( existing == null ) {
            errors.addError(
                    new ObjectError( command.toString(), null, null, "No such platform with id=" + ad.getId() ) );
            return processFormSubmission( request, response, command, errors );
        }

        // existing = arrayDesignService.thawLite( existing );
        existing.setDescription( ad.getDescription() );
        existing.setName( ad.getName() );
        existing.setShortName( ad.getShortName() );
        String technologyType = ad.getTechnologyType();
        if ( StringUtils.isNotBlank( technologyType ) ) {
            existing.setTechnologyType( TechnologyType.valueOf( technologyType ) );
        }

        arrayDesignService.update( existing );

        this.messageUtil.saveMessage( "object.updated", new Object[] { ad.getClass().getSimpleName().replaceFirst( "Impl", "" ), ad.getName() }, "Saved" );

        // go back to the aray we just edited.
        return new ModelAndView( new RedirectView( "/arrays/showArrayDesign.html?id=" + ad.getId(), true ) );
    }

    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        log.debug( "entering processFormSubmission" );

        if ( request.getParameter( "cancel" ) != null ) {
            messageUtil.saveMessage( "errors.cancel", "Cancelled" );
            return getCancelView( request );
        }

        return super.processFormSubmission( request, response, command, errors );
    }

    /**
     * Case = GET: Step 1 - return instance of command class (from database). This is not called in the POST case
     * because the sessionForm is set to 'true' in the constructor. This means the command object was already bound to
     * the session in the GET case.
     *
     * @param request http request
     * @return Object
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) {

        String idString = request.getParameter( "id" );

        ArrayDesignValueObject arrayDesign = null;

        // should be caught by validation.
        long id;
        if ( idString != null ) {
            try {
                id = Long.parseLong( idString );
            } catch ( NumberFormatException e ) {
                throw new IllegalArgumentException( "Invalid ID for platform.", e );
            }
            arrayDesign = arrayDesignService.loadValueObjectById( id );
        }

        if ( arrayDesign == null ) {
            return new ArrayDesignValueObject( -1L );
        }
        return arrayDesign;
    }

    protected ModelAndView getCancelView( HttpServletRequest request ) {
        long id = Long.parseLong( requireNonNull( request.getParameter( "id" ),
                "The 'id' query parameter is required." ) );
        // go back to the aray we just edited.
        return new ModelAndView(
                new RedirectView( "/arrays/showArrayDesign.html?id=" + id, true ) );
    }

    private static final List<String> TECHNOLOGY_TYPES = Arrays.stream( TechnologyType.values() )
            .map( TechnologyType::name )
            .sorted()
            .collect( Collectors.toList() );

    @Override
    protected Map<String, List<?>> referenceData( HttpServletRequest request ) {
        Map<String, List<?>> mapping = new HashMap<>();
        mapping.put( "technologyTypes", new ArrayList<>( TECHNOLOGY_TYPES ) );
        return mapping;
    }

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
}

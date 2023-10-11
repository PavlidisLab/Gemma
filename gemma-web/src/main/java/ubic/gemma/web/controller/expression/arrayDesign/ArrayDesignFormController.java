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

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.web.controller.BaseFormController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Controller for editing basic information about array designs.
 *
 * @author keshav
 */
public class ArrayDesignFormController extends BaseFormController {

    private ArrayDesignService arrayDesignService = null;

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

        saveMessage( request, "object.updated",
                new Object[] { ad.getClass().getSimpleName().replaceFirst( "Impl", "" ), ad.getName() }, "Saved" );

        // go back to the aray we just edited.
        return new ModelAndView( new RedirectView( "/arrays/showArrayDesign.html?id=" + ad.getId(), true ) );
    }

    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        log.debug( "entering processFormSubmission" );

        return super.processFormSubmission( request, response, command, errors );
    }

    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
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

        Long id;
        ArrayDesignValueObject arrayDesign = null;

        // should be caught by validation.
        if ( idString != null ) {
            try {
                id = Long.parseLong( idString );
            } catch ( NumberFormatException e ) {
                throw new IllegalArgumentException( "Invalid ID for platform.", e );
            }
            Collection<Long> ids = new HashSet<Long>();
            ids.add( id );
            Collection<ArrayDesignValueObject> arrayDesigns = arrayDesignService.loadValueObjectsByIds( ids );
            if ( arrayDesigns.size() > 0 )
                arrayDesign = arrayDesigns.iterator().next();

        }

        if ( arrayDesign == null ) {
            return new ArrayDesignValueObject( -1L );
        }
        return arrayDesign;
    }

    @Override
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

}

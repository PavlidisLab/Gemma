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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.web.util.EntityNotFoundException;
import ubic.gemma.web.util.MessageUtil;

import javax.servlet.http.HttpServletRequest;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for editing basic information about array designs.
 *
 * @author keshav
 */
@Controller
public class ArrayDesignEditController {

    private static final List<String> TECHNOLOGY_TYPES = Arrays.stream( TechnologyType.values() )
            .map( TechnologyType::name )
            .sorted()
            .collect( Collectors.toList() );

    @Autowired
    private ArrayDesignService arrayDesignService;

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

    @RequestMapping(value = "/arrayDesign/editArrayDesign.html", method = RequestMethod.GET)
    public ModelAndView getArrayDesign( @RequestParam("id") Long id ) {
        return new ModelAndView( "arrayDesign.edit" )
                .addObject( "arrayDesign", formBackingObject( id ) )
                .addObject( "technologyTypes", TECHNOLOGY_TYPES );
    }

    @RequestMapping(value = "/arrayDesign/editArrayDesign.html", method = RequestMethod.POST)
    public ModelAndView updateArrayDesign( ArrayDesignValueObject ad, HttpServletRequest request ) {
        ArrayDesign existing = arrayDesignService.loadOrFail( ad.getId(), EntityNotFoundException::new, "No platform with ID " + ad.getId() );

        // existing = arrayDesignService.thawLite( existing );
        existing.setDescription( ad.getDescription() );
        existing.setName( ad.getName() );
        existing.setShortName( ad.getShortName() );
        String technologyType = ad.getTechnologyType();
        if ( StringUtils.isNotBlank( technologyType ) ) {
            existing.setTechnologyType( TechnologyType.valueOf( technologyType ) );
        }

        arrayDesignService.update( existing );

        messageUtil.saveMessage( request, "object.updated",
                new Object[] { ad.getClass().getSimpleName().replaceFirst( "Impl", "" ), ad.getName() }, "Saved" );

        // go back to the array we just edited.
        return new ModelAndView( new RedirectView( "/arrays/showArrayDesign.html?id=" + ad.getId(), true ) );
    }

    /**
     * Case = GET: Step 1 - return instance of command class (from database). This is not called in the POST case
     * because the sessionForm is set to 'true' in the constructor. This means the command object was already bound to
     * the session in the GET case.
     *
     * @return Object
     */
    protected Object formBackingObject( Long id ) {
        ArrayDesignValueObject arrayDesign = arrayDesignService.loadValueObjectById( id );
        if ( arrayDesign == null ) {
            throw new EntityNotFoundException( "No platform with ID " + id );
        }
        return arrayDesign;
    }
}

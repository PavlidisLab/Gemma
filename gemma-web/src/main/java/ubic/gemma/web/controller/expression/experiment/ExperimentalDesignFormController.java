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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalDesignService;
import ubic.gemma.web.controller.BaseFormController;

/**
 * @author keshav
 * @version $Id$ "
 * @deprecated I'm not sure we're actively using this? It's quite old.
 */
@Deprecated
@Controller
@RequestMapping("/experimentalDesign/editExperimentalDesign.html")
public class ExperimentalDesignFormController extends BaseFormController {

    @Autowired
    private ExperimentalDesignService experimentalDesignService = null;

    /**
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.POST)
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, /*
                                                                                             * @ModelAttribute("experimentalDesign"
                                                                                             * )
                                                                                             */

    ExperimentalDesign command ) throws Exception {

        /*
         * FIXME: this won't work until we get rid of xdoclet.
         */

        experimentalDesignService.update( command );

        saveMessage( request, "object.saved", new Object[] { command.getClass().getSimpleName(), command.getId() },
                "Saved" );
        return new ModelAndView( "experimentalDesign.detail" ).addObject( "experimentalDesign", command );
    }

    /**
     * @param request
     * @return Object
     * @throws ServletException
     */
    @RequestMapping(method = RequestMethod.GET)
    protected String getForm( ModelMap model, HttpServletRequest request ) {

        ExperimentalDesign ed = null;

        Long id = Long.parseLong( request.getParameter( "id" ) );

        if ( id != null ) {
            ed = experimentalDesignService.load( id );
        } else {
            ed = ExperimentalDesign.Factory.newInstance();
        }

        model.addAttribute( "experimentalDesign", ed );

        // FIXME this is somewhat broken: ed.getId() will return null if request id was null.
        saveMessage( request, "object.editing", new Object[] { ed.getClass().getSimpleName(), ed.getId() }, "Editing" );

        return "experimentalDesign.edit";

    }

}

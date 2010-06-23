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
package ubic.gemma.web.controller.genome.bioSequence;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.web.controller.BaseController;

/**
 * @author joseph
 * @version $Id$
 */
@Controller
@RequestMapping("/genome/bioSequence")
public class BioSequenceController extends BaseController {

    @Autowired
    private BioSequenceService bioSequenceService = null;

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @RequestMapping("/showBioSequence.html")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );
        BioSequence bioSequence = bioSequenceService.load( id );
        if ( bioSequence == null ) {
            addMessage( request, "object.notfound", new Object[] { "Biosequence " + id } );
            return new ModelAndView( "mainMenu.html" );
        }
        bioSequence = bioSequenceService.thaw( bioSequence );
        ModelAndView mav = new ModelAndView( "bioSequence.detail" );
        mav.addObject( "bioSequence", bioSequence );
        return mav;
    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @RequestMapping("/showAllBioSequences.html")
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {

        String sId = request.getParameter( "id" );
        Collection<BioSequence> bioSequences = new ArrayList<BioSequence>();
        // if no IDs are specified, then show an error message
        if ( sId == null ) {
            addMessage( request, "object.notfound", new Object[] { "All biosequences cannot be listed. Biosequences " } );
        }

        // if ids are specified, then display only those bioSequences
        else {
            String[] idList = StringUtils.split( sId, ',' );

            for ( int i = 0; i < idList.length; i++ ) {
                Long id = Long.parseLong( idList[i] );
                BioSequence bioSequence = bioSequenceService.load( id );
                if ( bioSequence == null ) {
                    addMessage( request, "object.notfound", new Object[] { "Gene " + id } );
                }
                bioSequence = bioSequenceService.thaw( bioSequence );
                bioSequences.add( bioSequence );
            }
        }
        return new ModelAndView( "bioSequences" ).addObject( "bioSequences", bioSequences );

    }

}
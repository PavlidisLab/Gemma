/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.web.controller.genome;

import java.util.Collection;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import ubic.gemma.analysis.sequence.BlatResult2Psl;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultService;
import ubic.gemma.web.view.TextView;

/**
 * See {@link http://genome.ucsc.edu/goldenPath/help/customTrack.html}. This simply generates text that can be used to
 * view our alignments in the UCSC browser. For example, urls like the following would work:
 * <p>
 * http://genome.ucsc.edu/cgi-bin/hgTracks?org=human&hgt.customText=http://www.example.ca/Gemma/blatTrack. html&id=2929
 * </p>
 * <p>
 * Where the 'id' is the id in our system of the BLAT result to be views, and www.example.ca should be replaced with
 * ConfigUtils.getBaseUrl() (configured with gemma.base.url)
 * 
 * @author pavlidis
 * @version $Id$
 */
@Controller
public class BlatResultTrackController extends AbstractController {

    @Autowired
    private BlatResultService blatResultService;

    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response ) {
        String idS = request.getParameter( "id" );
        Long id = null;
        try {
            id = Long.parseLong( idS );
        } catch ( NumberFormatException e ) {
            // return error view.
        }
        Collection<Long> ids = new HashSet<Long>();
        ids.add( id );

        Collection<BlatResult> res = blatResultService.load( ids );

        if ( res.size() == 0 ) {
            // should be an error.
        }

        assert res.size() == 1;

        BlatResult toView = res.iterator().next();

        toView = blatResultService.thaw( toView );

        String val = BlatResult2Psl.blatResult2PslTrack( toView );

        return new ModelAndView( new TextView(), "text", val );

    }

}

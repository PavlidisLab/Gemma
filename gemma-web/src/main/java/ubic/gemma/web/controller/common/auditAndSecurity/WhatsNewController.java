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
package ubic.gemma.web.controller.common.auditAndSecurity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.core.analysis.report.WhatsNew;
import ubic.gemma.core.analysis.report.WhatsNewService;
import ubic.gemma.web.controller.WebConstants;

/**
 * Controller to provide information on "what's new" in the system
 *
 * @author pavlidis
 */
@Controller
@RequestMapping("/whatsnew")
public class WhatsNewController {

    @Autowired
    private WhatsNewService whatsNewService;

    @RequestMapping(value = "/daily.html", method = RequestMethod.GET)
    public ModelAndView daily() {
        ModelAndView mav = new ModelAndView( "wnDay" );
        WhatsNew wn = whatsNewService.getDailyReport();
        mav.addObject( "whatsnew", wn );
        mav.addObject( "timeSpan", "In the past day" );
        return mav;
    }

    @RequestMapping(value = "/generateCache.html", method = RequestMethod.GET)
    public ModelAndView generateCache() {
        ModelAndView mav = new ModelAndView( new RedirectView( WebConstants.HOME_PAGE, true ) );

        // save a report for a week's duration
        whatsNewService.generateWeeklyReport();

        return mav;
    }

    @RequestMapping(value = "/weekly.html", method = RequestMethod.GET)
    public ModelAndView weekly() {
        ModelAndView mav = new ModelAndView( "wnWeek" );
        WhatsNew wn = whatsNewService.getWeeklyReport();
        mav.addObject( "whatsnew", wn );
        mav.addObject( "timeSpan", "In the past week" );
        return mav;
    }
}

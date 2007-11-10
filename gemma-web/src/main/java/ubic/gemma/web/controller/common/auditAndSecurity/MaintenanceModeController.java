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

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.acegisecurity.GrantedAuthority;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.Constants;
import ubic.gemma.security.SecurityService;
import ubic.gemma.web.controller.BaseFormController;

/**
 * @spring.bean id="maintenanceModeController"
 * @spring.property name="formView" value="maintenanceMode" 
 * @author pavlidis
 * @version $Id$
 */
public class MaintenanceModeController extends BaseFormController {

    Map<String, Object> config;

    @SuppressWarnings("unchecked")
    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {

        config = ( Map<String, Object> ) getServletContext().getAttribute( Constants.CONFIG );
        
        /*
         * check that the user is admin!
         */
        boolean isAdmin = SecurityService.isUserAdmin();

        if ( !isAdmin ) {
            log.warn( "Attempt by non-admin to alter system maintenance mode status!" );
            this.saveMessage( request, "Request denied." );
            return new ModelAndView( new RedirectView( "/Gemma/mainMenu.html" ) );
        }

        String stop = request.getParameter( "stop" );
        String start = request.getParameter( "start" );

        if ( StringUtils.isBlank( start ) && StringUtils.isBlank( stop ) ) {
            return new ModelAndView( this.getFormView() );
        }

        if ( StringUtils.isNotBlank( start ) && StringUtils.isNotBlank( stop ) ) {
            this.saveMessage( request, "Can't decide whether to stop or start" );
            return new ModelAndView( new RedirectView( "/Gemma/mainMenu.html" ) );
        }

        if ( StringUtils.isNotBlank( stop ) ) {
            this.saveMessage( request, "Maintenance mode turned off" );
            config.put( "maintenanceMode", false );
        } else if ( StringUtils.isNotBlank( start ) ) {
            this.saveMessage( request, "Maintenance mode turned on" );
            config.put( "maintenanceMode", true );
        }
        return new ModelAndView( new RedirectView( "/Gemma/mainMenu.html" ) );

    }

    

}

/*
 * The Gemma_sec1 project
 * 
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.security.authentication;

import gemma.gsec.authentication.ManualAuthenticationService;
import gemma.gsec.authentication.UserManager;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import ubic.gemma.security.audit.AuditAdvice;
import ubic.gemma.util.Settings;

/**
 * Specialization of Spring task-running support so task threads have secure context (without using MODE_GLOBAL!). The
 * thread where Quartz is being run is authenticated as GROUP_AGENT.
 * 
 * @author paul
 * @version $Id$
 */
public class SecureMethodInvokingJobDetailFactoryBean extends MethodInvokingJobDetailFactoryBean {

    private static Logger log = LoggerFactory.getLogger( AuditAdvice.class.getName() );

    /**
     * @param log the log to set
     */
    public static void setLog( Logger log ) {
        SecureMethodInvokingJobDetailFactoryBean.log = log;
    }

    @Autowired
    ManualAuthenticationService manualAuthenticationService;

    @Autowired
    UserManager userManager;

    /*
     * (non-Javadoc)
     * @see org.springframework.util.MethodInvoker#invoke()
     */
    @Override
    public Object invoke() throws InvocationTargetException, IllegalAccessException {

        String serverUserName = Settings.getString( "gemma.agent.userName" );
        String serverPassword = Settings.getString( "gemma.agent.password" );
        Object result;
        Authentication oldAuth = SecurityContextHolder.getContext().getAuthentication();

        try {
            assert manualAuthenticationService != null;
            Authentication auth = manualAuthenticationService.attemptAuthentication( serverUserName, serverPassword );
            SecurityContextHolder.getContext().setAuthentication( auth );
        } catch ( AuthenticationException e ) {
            log.error( "Failed to authenticate schedule job, jobs probably won't work, but trying anonymous" );
            manualAuthenticationService.authenticateAnonymously();
        } finally {
            result = super.invoke();
            SecurityContextHolder.getContext().setAuthentication( oldAuth );

        }
        return result;
    }

    /**
     * @param manualAuthenticationService the manualAuthenticationService to set
     */
    public void setManualAuthenticationService( ManualAuthenticationService manualAuthenticationService ) {
        this.manualAuthenticationService = manualAuthenticationService;
    }

    /**
     * @param userManager the userManager to set
     */
    public void setUserManager( UserManager userManager ) {
        this.userManager = userManager;
    }
}

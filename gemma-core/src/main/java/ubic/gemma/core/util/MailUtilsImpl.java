/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;
import ubic.gemma.core.job.EmailNotificationContext;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.security.authentication.UserService;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.persistence.util.MailEngine;
import ubic.gemma.persistence.util.Settings;

/**
 * @author anton
 */
@Component
public class MailUtilsImpl implements MailUtils {

    private static final Log log = LogFactory.getLog( MailUtilsImpl.class );

    @Autowired
    private MailEngine mailEngine;

    @Autowired
    private UserService userService;

    @Override
    public void sendTaskCompletedNotificationEmail( EmailNotificationContext emailNotificationContext,
            TaskResult taskResult ) {
        String taskId = emailNotificationContext.getTaskId();
        String submitter = emailNotificationContext.getSubmitter();
        String taskName = emailNotificationContext.getTaskName();

        if ( StringUtils.isNotBlank( submitter ) ) {
            User user = userService.findByUserName( submitter );

            assert user != null;

            String emailAddress = user.getEmail();

            if ( emailAddress != null ) {
                MailUtilsImpl.log.info( "Sending email notification to " + emailAddress );
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo( emailAddress );
                msg.setFrom( Settings.getAdminEmailAddress() );
                msg.setSubject( "Gemma task completed" );

                String logs = "";
                if ( taskResult.getException() != null ) {
                    logs += "Task failed with :\n";
                    logs += taskResult.getException().getMessage();
                }

                msg.setText(
                        "A job you started on Gemma is completed (taskId=" + taskId + ", " + taskName + ")\n\n" + logs
                                + "\n" );

                mailEngine.send( msg );
            }
        }
    }
}

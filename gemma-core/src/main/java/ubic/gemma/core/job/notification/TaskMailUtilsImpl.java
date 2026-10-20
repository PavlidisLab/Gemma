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
package ubic.gemma.core.job.notification;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.mail.MailService;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.common.auditAndSecurity.User;

/**
 * @author anton
 */
@Component
public class TaskMailUtilsImpl implements TaskMailUtils {

    private static final Log log = LogFactory.getLog( TaskMailUtilsImpl.class );

    @Autowired
    private MailService mailService;

    @Autowired
    private UserManager userManager;

    @Override
    public void sendTaskCompletedNotificationEmail( EmailNotificationContext emailNotificationContext,
            TaskResult taskResult ) {
        String submitter = emailNotificationContext.getSubmitter();
        if ( StringUtils.isBlank( submitter ) ) {
            log.warn( "Submitted in blank in email notification context, not sending email." );
            return;
        }

        User user = userManager.findByUserName( submitter );
        if ( user == null ) {
            log.warn( "User with username '" + submitter + "' not found, not sending email." );
            return;
        }

        String emailAddress = user.getEmail();
        if ( StringUtils.isBlank( emailAddress ) ) {
            log.warn( "Email address for user '" + submitter + "' is blank, not sending email." );
            return;
        }

        String taskId = emailNotificationContext.getTaskId();
        String taskName = emailNotificationContext.getTaskName();
        String taskStatus = taskResult.getException() != null ? "FAILURE" : "SUCCESS";
        String taskLogs;
        if ( taskResult.getException() != null ) {
            taskLogs = taskResult.getException().getMessage();
        } else {
            taskLogs = "<empty>";
        }

        mailService.sendTaskCompletedEmail( user, taskId, taskName, taskStatus, taskLogs );
    }
}

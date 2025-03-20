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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.util.MailEngine;
import ubic.gemma.model.common.auditAndSecurity.User;

/**
 * @author anton
 */
@Component
public class TaskMailUtilsImpl implements TaskMailUtils {

    private static final Log log = LogFactory.getLog( TaskMailUtilsImpl.class );

    @Autowired
    private MailEngine mailEngine;

    @Autowired
    private UserManager userManager;

    @Override
    public void sendTaskCompletedNotificationEmail( EmailNotificationContext emailNotificationContext,
            TaskResult taskResult ) {
        String taskId = emailNotificationContext.getTaskId();
        String submitter = emailNotificationContext.getSubmitter();
        String taskName = emailNotificationContext.getTaskName();

        if ( StringUtils.isNotBlank( submitter ) ) {
            User user = userManager.findByUserName( submitter );

            assert user != null;

            String emailAddress = user.getEmail();

            if ( emailAddress != null ) {
                TaskMailUtilsImpl.log.info( "Sending email notification to " + emailAddress );
                SimpleMailMessage msg = new SimpleMailMessage();
                String logs = "";
                if ( taskResult.getException() != null ) {
                    logs += "Task failed with :\n";
                    logs += taskResult.getException().getMessage();
                }
                String body = "A job you started on Gemma is completed (taskId=" + taskId + ", " + taskName + ")\n\n" + logs + "\n";
                mailEngine.sendMessage( emailAddress, "Gemma task completed", body );
            }
        }
    }
}

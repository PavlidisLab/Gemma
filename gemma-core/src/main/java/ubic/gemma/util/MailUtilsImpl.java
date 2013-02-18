package ubic.gemma.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;
import ubic.gemma.job.EmailNotificationContext;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.security.authentication.UserService;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 04/02/13
 * Time: 5:29 PM
 * To change this template use File | Settings | File Templates.
  */
@Component
public class MailUtilsImpl implements MailUtils {

    private static final Log log = LogFactory.getLog( MailUtilsImpl.class );

    @Autowired MailEngine mailEngine;
    @Autowired UserService userService;

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
                log.debug( "Sending email notification to " + emailAddress );
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo( emailAddress );
                msg.setFrom( ConfigUtils.getAdminEmailAddress() );
                msg.setSubject( "Gemma task completed" );


                String logs = "";
                if (taskResult.getException() != null) {
                    logs += "Task failed with :\n";
                    logs += taskResult.getException().getMessage();
                }

                msg.setText( "A job you started on Gemma is completed (taskid=" + taskId + ", "
                        + taskName + ")\n\n" + logs + "\n" );

                /*
                 * TODO provide a link to something relevant something like:
                 */
                String url = ConfigUtils.getBaseUrl() + "user/tasks.html?taskId=" + taskId;

                mailEngine.send( msg );
            }
        }
    }

    //    private void emailNotifyCompletionOfTask( TaskCommand taskCommand, ExecutingTask executingTask ) {
//        if ( StringUtils.isNotBlank( taskCommand.getSubmitter() ) ) {
//            User user = userService.findByUserName( taskCommand.getSubmitter() );
//
//            assert user != null;
//
//            String emailAddress = user.getEmail();
//
//            if ( emailAddress != null ) {
//                log.debug( "Sending email notification to " + emailAddress );
//                SimpleMailMessage msg = new SimpleMailMessage();
//                msg.setTo( emailAddress );
//                msg.setFrom( ConfigUtils.getAdminEmailAddress() );
//                msg.setSubject( "Gemma task completed" );
//
//                String logs = "Event logs:\n";
//                if ( executingTask != null ) {
//                    logs += StringUtils.join( executingTask.getLocalProgressQueue(), "\n" );
//                }
//
//                msg.setText( "A job you started on Gemma is completed (taskid=" + taskCommand.getTaskId() + ", "
//                        + taskCommand.getTaskClass().getSimpleName() + ")\n\n" + logs + "\n" );
//
//                /*
//                 * TODO provide a link to something relevant something like:
//                 */
//                String url = ConfigUtils.getBaseUrl() + "user/tasks.html?taskId=" + taskCommand.getTaskId();
//
//                mailEngine.send( msg );
//            }
//        }
//    }

}

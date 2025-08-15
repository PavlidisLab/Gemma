package ubic.gemma.core.mail;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.auditAndSecurity.User;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@CommonsLog
@Service
public class MailServiceImpl implements MailService {

    @Autowired
    private MailEngine mailEngine;

    @Value("${gemma.hosturl}")
    private String hostUrl;
    @Value("${gemma.admin.email}")
    private String adminEmailAddress;
    @Value("${gemma.support.email}")
    private String supportEmailAddress;

    /**
     * Send an email to request signup confirmation.
     */
    @Override
    public void sendSignupConfirmationEmail( String email, String username, String token, Locale locale ) {
        Map<String, Object> model = new HashMap<>();
        model.put( "username", username );
        model.put( "confirmLink", hostUrl + "/confirmRegistration.html?key=" + urlEncode( token ) + "&username=" + urlEncode( username ) );
        model.put( "supportEmailAddress", supportEmailAddress );
        mailEngine.sendMessage( email, "Confirm your Gemma registration", "accountCreated", model );
    }

    /**
     * Send an email to request signup confirmation.
     */
    @Override
    public void sendResetConfirmationEmail( String email, String username, String password, String token, Locale locale ) {
        Map<String, Object> model = new HashMap<>();
        model.put( "username", username );
        model.put( "password", password );
        model.put( "resetLink", hostUrl + "/confirmRegistration.html?key=" + urlEncode( token ) + "&username=" + urlEncode( username ) );
        model.put( "supportEmailAddress", supportEmailAddress );
        mailEngine.sendMessage( email, "Your Gemma password has been reset", "passwordReset", model );
    }

    /**
     * Send an email when a user is added to a group.
     */
    @Override
    public void sendAddUserToGroupEmail( User user, String groupName, User userTakingAction ) {
        Map<String, Object> model = new HashMap<>();
        model.put( "userTakingAction", userTakingAction.getUserName() );
        model.put( "username", user.getUserName() );
        model.put( "group", groupName );
        model.put( "manageGroupsUrl", hostUrl + "/manageGroups.html" );
        model.put( "adminEmailAddress", adminEmailAddress );
        mailEngine.sendMessage( user.getEmail(), "You have been added to a group in Gemma", "userAddedToGroup", model );
    }

    /**
     * Send an email when a user is removed from a group.
     */
    @Override
    public void sendRemoveUserFromGroupEmail( User user, String groupName, User userTakingAction ) {
        Map<String, Object> model = new HashMap<>();
        model.put( "username", user.getUserName() );
        model.put( "userTakingAction", userTakingAction.getUserName() );
        model.put( "group", groupName );
        model.put( "manageGroupsUrl", hostUrl + "/manageGroups.html" );
        model.put( "adminEmailAddress", adminEmailAddress );
        mailEngine.sendMessage( user.getEmail(), "You have been added to a group in Gemma", "userRemovedFromGroup", model );
    }

    @Override
    public void sendTaskCompletedEmail( User user, String taskId, String taskName, String taskStatus, String logs ) {
        Map<String, Object> model = new HashMap<>();
        model.put( "username", user.getUserName() );
        model.put( "taskId", taskId );
        model.put( "taskName", taskName );
        model.put( "taskStatus", taskStatus );
        model.put( "taskLogs", logs );
        mailEngine.sendMessage( user.getEmail(), "Your Gemma task is completed", "taskCompleted", model );
    }

    private String urlEncode( String s ) {
        try {
            return URLEncoder.encode( s, StandardCharsets.UTF_8.name() );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }
}

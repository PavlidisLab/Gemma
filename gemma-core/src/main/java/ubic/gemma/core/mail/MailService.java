package ubic.gemma.core.mail;

import org.springframework.mail.MailException;
import ubic.gemma.model.common.auditAndSecurity.User;

import java.util.Locale;

/**
 * High-level service for sending mails.
 * @author poirigui
 */
public interface MailService {

    void sendSignupConfirmationEmail( String email, String username, String token, Locale locale ) throws MailException;

    void sendResetConfirmationEmail( String email, String username, String password, String token, Locale locale ) throws MailException;

    void sendAddUserToGroupEmail( User user, String groupName, User userTakingAction ) throws MailException;

    void sendRemoveUserFromGroupEmail( User user, String groupName, User userTakingAction ) throws MailException;
}

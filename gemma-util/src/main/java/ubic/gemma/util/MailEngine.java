package ubic.gemma.util;

import java.util.Map;

import javax.mail.MessagingException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;

public interface MailEngine {

    public abstract void sendAdminMessage( String bodyText, String subject );

    public abstract void sendMessage( String[] emailAddresses, ClassPathResource resource, String bodyText,
            String subject, String attachmentName ) throws MessagingException;

    public abstract void send( SimpleMailMessage msg );

    public abstract void sendMessage( SimpleMailMessage msg, String templateName, Map<String, Object> model );

}
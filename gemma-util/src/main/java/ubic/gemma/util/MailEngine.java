/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.util;

import java.util.Map;

import javax.mail.MessagingException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;

/**
 * @author paul
 * @version $Id$
 */
public interface MailEngine {

    public abstract void sendAdminMessage( String bodyText, String subject );

    public abstract void sendMessage( String[] emailAddresses, ClassPathResource resource, String bodyText,
            String subject, String attachmentName ) throws MessagingException;

    public abstract void send( SimpleMailMessage msg );

    public abstract void sendMessage( SimpleMailMessage msg, String templateName, Map<String, Object> model );

}
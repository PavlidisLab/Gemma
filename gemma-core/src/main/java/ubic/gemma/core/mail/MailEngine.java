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
package ubic.gemma.core.mail;

import org.springframework.mail.MailException;

import java.util.Map;

/**
 * @author paul
 */
public interface MailEngine {

    /**
     * Send an email message to the administrator.
     */
    void sendMessageToAdmin( String subject, String bodyText ) throws MailException;

    /**
     * Send a text email message.
     */
    void sendMessage( String to, String subject, String body ) throws MailException;

    /**
     * Send a templated email message.
     */
    void sendMessage( String to, String subject, String templateName, Map<String, Object> model ) throws MailException;
}
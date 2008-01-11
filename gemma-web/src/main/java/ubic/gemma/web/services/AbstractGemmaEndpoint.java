/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

package ubic.gemma.web.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.server.endpoint.AbstractDomPayloadEndpoint;

import ubic.gemma.security.authentication.ManualAuthenticationProcessing;

/**
 * 
 * @author klc Abstracts out the security and a few constants.
 * 
 */

public abstract class AbstractGemmaEndpoint extends AbstractDomPayloadEndpoint {

	protected ManualAuthenticationProcessing manualAuthenticationProcessing;

	/**
	 * Namespace of both request and response.
	 */
	public static final String NAMESPACE_URI = "http://bioinformatics.ubc.ca/Gemma/ws";

	private static Log log = LogFactory.getLog(AbstractGemmaEndpoint.class);

	private static final String USER = "administrator";

	private static final String PASSWORD = "gemmatoast";

	protected static final String REQUEST = "Request";

	protected static final String RESPONSE = "Response";

	public AbstractGemmaEndpoint() {
		super();
	}

	public void setManualAuthenticationProcessing(
			ManualAuthenticationProcessing map) {
		this.manualAuthenticationProcessing = map;
		
		authenticate();

	}

	protected boolean authenticate(){
		
		boolean result = this.manualAuthenticationProcessing.validateRequest( USER, PASSWORD);
			if (! result)       	log.error("Failed to authenticate");
        
			return result;
	}
}
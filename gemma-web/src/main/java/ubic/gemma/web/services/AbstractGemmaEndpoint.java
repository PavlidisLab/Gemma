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

import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.ws.server.endpoint.AbstractDomPayloadEndpoint;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ubic.gemma.security.authentication.ManualAuthenticationProcessing;

/**
 * 
 * @author gavin, klc Abstracts out the security and a few constants.
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

	private static final String PASSWORD = "testing";

	protected static final String REQUEST = "Request";

	protected static final String RESPONSE = "Response";
	
	private String localName;

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
	
	protected void setLocalName(String localName){
		this.localName = localName;
	}
	
	/**
	 * Function that handles the retrieval of xml input.  Use this method if there is only one value in the input but
	 * generically, this method can also store multiple input values as well.  This will depend on how the xml is parsed 
	 * by the client.  
	 * Still need to test on different types of client requests.    
	 * @param requestElement - xml request in node hierarchy
	 * @param document - 
	 * @param tagName
	 * @return a collection contain one string element
	 */
	protected Collection<String> getNodeValues(Element requestElement, String tagName){
		Assert.isTrue(NAMESPACE_URI.equals(requestElement.getNamespaceURI()),
		"Invalid namespace");
		Assert.isTrue(localName.equals(requestElement.getLocalName()),
		"Invalid local name");

		authenticate();
		
		Collection<String> value = new HashSet<String>();
		String node = "";
		
		//get the Element with name = tagName
		NodeList children = requestElement.getElementsByTagName(
				tagName).item(0).getChildNodes();
		// iterate over the child nodes
		for (int i = 0; i < children.getLength(); i++) {

			if (children.item(i).getNodeType() == Node.TEXT_NODE) {
				node = children.item(i).getNodeValue();
//			if (StringUtils.isNotEmpty(node)
//				&& StringUtils.isNumeric(node)) {
//					break;
//				}
				value.add(node);
				
				
			}
			node = null;
		}

		if (value == null) {
			throw new IllegalArgumentException(
					"Could not find request text node");
		}
		
		return value;
	}
	
	
	/**
	 * A method written for array input from MATLAB clients.  The more generic method to use is getNodeValues().
	 * This seems to work for both horizontal arrays and column arrays in MATLAB
	 * @param requestElement
	 * @param document
	 * @param tagName
	 * @return
	 */
	protected Collection<String> getArrayValues(Element requestElement, String tagName){
		Assert.isTrue(NAMESPACE_URI.equals(requestElement.getNamespaceURI()),
		"Invalid namespace");
		Assert.isTrue(localName.equals(requestElement.getLocalName()),
		"Invalid local name");

		authenticate();
		
		Collection<String> value = new HashSet<String>();
		String node = "";
		
		//get the Element with name = tagName
		NodeList children = requestElement.getElementsByTagName(
				tagName).item(0).getChildNodes();
		// iterate over the child nodes
		//but it appears that MATLAB encodes it so that every odd (ie. 1, 3, 5, 7, etc) grandchild holds the array value
		for (int i = 1; i < children.getLength(); i+=2) {
			
			Node child = children.item(i).getChildNodes().item(0); //need to go one more level down into the grandchildren
			
			if (child.getNodeType() == Node.TEXT_NODE) {
				node = child.getNodeValue();
//			if (StringUtils.isNotEmpty(node)
//				&& StringUtils.isNumeric(node)) {
//					break;
//				}
				value.add(node);
				
			}
			node = "";
		}

		if (value == null) {
			throw new IllegalArgumentException(
					"Could not find request text node");
		}
		
		return value;
	}
	
	/**
	 * Function to handle the constructing of output in xml format for returning the response to the client.
	 * @param document
	 * @param values - a collection of the values (in String format) to be returned to the client 
	 * @param elementName
	 * @return
	 */
	protected Element buildWrapper(Document document, Collection<String> values, String elementName){

		Element responseWrapper = document.createElementNS(NAMESPACE_URI,
				localName);
		Element responseElement = document.createElementNS(NAMESPACE_URI,
				localName + RESPONSE);
		responseWrapper.appendChild(responseElement);

		if (values == null || values.isEmpty())
			responseElement.appendChild(document
					.createTextNode("No "+elementName +" result"));
		else {
			// Need to create a list (array) of the geneIds
			for (String value : values) {
				Element e = document.createElement(elementName);
				e.appendChild(document.createTextNode(value));
				responseElement.appendChild(e);
			}
		}
		return responseWrapper;
	}

}
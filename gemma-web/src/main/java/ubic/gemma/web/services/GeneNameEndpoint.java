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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import ubic.gemma.model.genome.gene.GeneService;

/**
 * 
 * @author klc
 * 
 */

public class GeneNameEndpoint extends AbstractGemmaEndpoint {

	private static Log log = LogFactory.getLog(GeneNameEndpoint.class);

	private GeneService geneService;

	/**
	 * The local name of the expected Request/Response.
	 */
	public static final String GENE_LOCAL_NAME = "geneName";

	/**
	 * Sets the "business service" to delegate to.
	 */
	public void setGeneService(GeneService geneS) {
		this.geneService = geneS;
	}

	/**
	 * Reads the given <code>requestElement</code>, and sends a the response
	 * back.
	 * 
	 * @param requestElement
	 *            the contents of the SOAP message as DOM elements
	 * @param document
	 *            a DOM document to be used for constructing <code>Node</code>s
	 * @return the response element
	 */
	protected Element invokeInternal(Element requestElement, Document document)
			throws Exception {
		Assert.isTrue(NAMESPACE_URI.equals(requestElement.getNamespaceURI()),
				"Invalid namespace");
		Assert.isTrue(GENE_LOCAL_NAME.equals(requestElement.getLocalName()),
				"Invalid local name");

		// Tried to use a generic way to extract just the node with the data but
		// no luck getting this to work... perhaps the namespace is invalid...
		// NodeIterator nodeList = org.apache.xpath.XPathAPI.selectNodeIterator(
		// requestElement, "experimentNameRequest" );

		// The ExperimentNameRequest Element is going to be wrapped inside the
		// ExperimentName Element as specifided by the wsdl

		NodeList children = requestElement.getElementsByTagName(
				GENE_LOCAL_NAME + REQUEST).item(0).getChildNodes();

		authenticate();
		String nodeValue = null;

		// We unwrapped the node, now get the 1st value that is a number (should
		// only be one)
		for (int i = 0; i < children.getLength(); i++) {

			if (children.item(i).getNodeType() == Node.TEXT_NODE) {
				nodeValue = children.item(i).getNodeValue();
				if (StringUtils.isNotEmpty(nodeValue)
						&& StringUtils.isNumeric(nodeValue)) {
					break;
				}
			}
			nodeValue = null;
		}

		if (nodeValue == null) {
			throw new IllegalArgumentException(
					"Could not find request text node");
		}

		String geneName = geneService.load(Long.parseLong(nodeValue)).getName();

		Element responseWrapper = document.createElementNS(NAMESPACE_URI,
				GENE_LOCAL_NAME);
		Element responseElement = document.createElementNS(NAMESPACE_URI,
				GENE_LOCAL_NAME + RESPONSE);
		Text responseText = document.createTextNode(geneName);
		responseElement.appendChild(responseText);
		responseWrapper.appendChild(responseElement);

		return responseWrapper;
	}

}

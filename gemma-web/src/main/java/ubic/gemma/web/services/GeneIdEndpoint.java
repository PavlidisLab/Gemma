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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * 
 * @author klc
 * 
 */

public class GeneIdEndpoint extends AbstractGemmaEndpoint {

	private static Log log = LogFactory.getLog(GeneIdEndpoint.class);

	private GeneService geneService;

	/**
	 * The local name of the expected Request/Response.
	 */
	public static final String GENE_LOCAL_NAME = "geneId";

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

		// TODO: might be wise to not assert this and just send back an error
		// code or something 
		Assert.isTrue(NAMESPACE_URI.equals(requestElement.getNamespaceURI()),
				"Invalid namespace");
		Assert.isTrue(GENE_LOCAL_NAME.equals(requestElement.getLocalName()),
				"Invalid local name");

		authenticate();
		
		NodeList children = requestElement.getElementsByTagName(
				GENE_LOCAL_NAME + REQUEST).item(0).getChildNodes();

		Text requestText = null;
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).getNodeType() == Node.TEXT_NODE) {
				requestText = (Text) children.item(i);
				break;
			}
		}
		if (requestText == null) {
			throw new IllegalArgumentException(
					"Could not find request text node");
		}

		Collection<Gene> genes = geneService
				.findByOfficialSymbolInexact(requestText.getNodeValue());

		Element responseWrapper = document.createElementNS(NAMESPACE_URI,
				GENE_LOCAL_NAME);
		Element responseElement = document.createElementNS(NAMESPACE_URI,
				GENE_LOCAL_NAME + RESPONSE);
		responseWrapper.appendChild(responseElement);

		
		if (genes == null)
					responseElement.appendChild(document.createTextNode("No genes with that common name"));
		else {
			//Need to create a list (array) of the geneIds
			for (Gene gene : genes) {
				Element e = document.createElement("geneIds");
				e.appendChild(document.createTextNode(gene.getId().toString()));
				responseElement.appendChild(e);
			}
		}

		return responseWrapper;
	}

}

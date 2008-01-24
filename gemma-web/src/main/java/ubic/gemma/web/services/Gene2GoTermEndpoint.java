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

import org.apache.commons.lang.StringUtils;
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
import ubic.gemma.ontology.GeneOntologyService;
import ubic.gemma.ontology.OntologyTerm;

/**
 * 
 * @author klc
 * 
 */

public class Gene2GoTermEndpoint extends AbstractGemmaEndpoint {

	private static Log log = LogFactory.getLog(Gene2GoTermEndpoint.class);

	private GeneOntologyService geneOntologyService;

	private GeneService geneService;

	/**
	 * The local name of the expected request/response.
	 */
	public static final String GENE2GO_LOCAL_NAME = "gene2Go";

	/**
	 * Sets the "business service" to delegate to.
	 */
	public void setGeneOntologyService(GeneOntologyService goS) {
		this.geneOntologyService = goS;
	}
	
	public void setGeneService(GeneService geneS){
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
		Assert.isTrue(GENE2GO_LOCAL_NAME.equals(requestElement.getLocalName()),
				"Invalid local name");

		authenticate();
		// Tried to use a generic way to extract just the node with the data but
		// no luck getting this to work... perhaps the namespace is invalid...
		// NodeIterator nodeList = org.apache.xpath.XPathAPI.selectNodeIterator(
		// requestElement, "experimentNameRequest" );

		// The ExperimentNameRequest Element is going to be wrapped inside the
		// ExperimentName Element as specifided by the wsdl

		// NodeList children = requestElement.getChildNodes();

		NodeList children = requestElement.getElementsByTagName(
				GENE2GO_LOCAL_NAME + REQUEST).item(0).getChildNodes();
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

		Long geneId = Long.parseLong(nodeValue);
		Gene gene = geneService.load(geneId);
		Collection<OntologyTerm> terms = geneOntologyService.getGOTerms(gene);

		Element responseWrapper = document.createElementNS(NAMESPACE_URI,
				GENE2GO_LOCAL_NAME);
		Element responseElement = document.createElementNS(NAMESPACE_URI,
				GENE2GO_LOCAL_NAME + RESPONSE);
		responseWrapper.appendChild(responseElement);

		if (terms == null || terms.isEmpty())
			responseElement.appendChild(document
					.createTextNode("No go terms found for given gene:  "
							+ geneId));
		else {
			// Need to create a list (array) of the geneIds
			for (OntologyTerm term : terms) {
				Element e = document.createElement("goId");
				e.appendChild(document.createTextNode(term.getUri()));
				responseElement.appendChild(e);
			}
		}

		return responseWrapper;
	}

}

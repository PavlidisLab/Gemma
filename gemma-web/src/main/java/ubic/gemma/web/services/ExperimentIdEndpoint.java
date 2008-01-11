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
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

/**
 * 
 * @author klc
 * 
 */

public class ExperimentIdEndpoint extends AbstractGemmaEndpoint {

	private static Log log = LogFactory.getLog(ExperimentIdEndpoint.class);

	private ExpressionExperimentService expressionExperimentService;

	/**
	 * The local name of the request/response.
	 */
	public static final String EXPERIMENT_LOCAL_NAME = "experimentId";

	/**
	 * Sets the "business service" to delegate to.
	 */
	public void setExpressionExperimentService(ExpressionExperimentService ees) {
		this.expressionExperimentService = ees;
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
		Assert.isTrue(EXPERIMENT_LOCAL_NAME.equals(requestElement
				.getLocalName()), "Invalid local name");

		authenticate();
		NodeList children = requestElement.getElementsByTagName(
				EXPERIMENT_LOCAL_NAME+REQUEST).item(0).getChildNodes();

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

		ExpressionExperiment ee = expressionExperimentService
				.findByShortName(requestText.getNodeValue());

		Element responseWrapper = document.createElementNS(NAMESPACE_URI,
				EXPERIMENT_LOCAL_NAME);
		Element responseElement = document.createElementNS(NAMESPACE_URI,
				EXPERIMENT_LOCAL_NAME + RESPONSE);

		Text responseText;

		if (ee == null)
			responseText = document
					.createTextNode("No expression experiment with that short name");
		else
			responseText = document.createTextNode(ee.getId().toString());

		responseElement.appendChild(responseText);
		responseWrapper.appendChild(responseElement);

		return responseWrapper;
	}

}

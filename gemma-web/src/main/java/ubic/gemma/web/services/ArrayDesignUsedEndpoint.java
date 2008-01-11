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

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

/**
 * 
 * @author klc
 * 
 */

public class ArrayDesignUsedEndpoint extends AbstractGemmaEndpoint {

	private static Log log = LogFactory.getLog(ArrayDesignUsedEndpoint.class);

	private ExpressionExperimentService expressionExperimentService;

	/**
	 * The local name of the expected request.
	 */
	public static final String ARRAY_LOCAL_NAME = "arrayDesignUsed";

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
		Assert.isTrue(ARRAY_LOCAL_NAME.equals(requestElement.getLocalName()),
				"Invalid local name");

		authenticate();

		String nodeValue = null;
		NodeList children = requestElement.getElementsByTagName(
				ARRAY_LOCAL_NAME + REQUEST).item(0).getChildNodes();
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
			// else if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
			// nodeValue = children.item(i).getFirstChild().getNodeValue();
			// if (StringUtils.isNotEmpty(nodeValue) &&
			// StringUtils.isNumeric(nodeValue)){
			// break;
			// }
			// }
			nodeValue = null;
		}

		ExpressionExperiment ee = expressionExperimentService.load(Long
				.parseLong(nodeValue));

		Element responseWrapper = document.createElementNS(NAMESPACE_URI,
				ARRAY_LOCAL_NAME);
		Element responseElement = document.createElementNS(NAMESPACE_URI,
				ARRAY_LOCAL_NAME + RESPONSE);
		responseWrapper.appendChild(responseElement);

		if (ee == null)
			responseElement.appendChild(document
					.createTextNode("No experiment with that id"));
		else {
			Collection<ArrayDesign> ads = expressionExperimentService
					.getArrayDesignsUsed(ee);
			for (ArrayDesign ad : ads) {
				Element e = document.createElement("adName");
				e.appendChild(document.createTextNode(ad.getName()));
				responseElement.appendChild(e);
			}
		}

		return responseWrapper;
	}

}

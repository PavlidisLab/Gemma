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
 * Given a gene ID, will return the matching gene name.
 * Note: this is not the short name
 * @author klc, gavin
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
		
		setLocalName(GENE_LOCAL_NAME);
		String geneId ="";
		
		Collection<String> geneResults = getNodeValues(requestElement, "gene_id");
		
		for (String id: geneResults){
			geneId = id;
		}

		String geneName = geneService.load(Long.parseLong(geneId)).getName();

		//get Array Design ID and build results in the form of a collection
		Collection<String> gName = new HashSet<String>();
		gName.add(geneName);
		
		

		return buildWrapper(document, gName, "gene_name");
	}

}

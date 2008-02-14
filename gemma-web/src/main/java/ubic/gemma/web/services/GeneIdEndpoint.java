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
 * Given the official symbolic of a gene, will return the matching gene ID.
 * @author klc, gavin
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
		setLocalName(GENE_LOCAL_NAME);
		String geneName ="";
		
		Collection<String> geneResults = getNodeValues(requestElement, "gene_official_symbol");
		
		for (String name: geneResults){
			geneName = name;
		}

		Collection<Gene> genes = geneService.findByOfficialSymbolInexact(geneName);

				
		
		

		//get Array Design ID and build results in the form of a collection
		Collection<String> gIDs = new HashSet<String>();
		for (Gene gene: genes)
		gIDs.add(gene.getId().toString());
		
		

		return buildWrapper(document, gIDs, "gene_name");
	}

}

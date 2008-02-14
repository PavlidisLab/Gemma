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

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.GeneOntologyService;
import ubic.gemma.ontology.OntologyTerm;

/**
 * Given a Gene ID, will return a collection of Gene Ontology URIs that matching the gene.
 * @author klc, gavin
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
	
		setLocalName(GENE2GO_LOCAL_NAME);
		String gid ="";
		
		Collection<String> geneResult = getNodeValues(requestElement, "gene_id");
		for (String id: geneResult)
			gid = id;
		
		Long geneId = Long.parseLong(gid);
		Gene gene = geneService.load(geneId);
		Collection<OntologyTerm> terms = geneOntologyService.getGOTerms(gene);

		//build Collection to send to wrapper
		Collection<String> goTerms = new HashSet<String>();
		for (OntologyTerm ot : terms) {	
			goTerms.add(ot.getUri());
		}
		
		return buildWrapper(document, goTerms, "go_uris");
	}
	
}

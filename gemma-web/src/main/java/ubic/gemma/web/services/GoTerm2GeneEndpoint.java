package ubic.gemma.web.services;


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

	import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

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
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
	import ubic.gemma.model.genome.gene.GeneService;
	import ubic.gemma.ontology.GeneOntologyService;
import ubic.gemma.ontology.OntologyTerm;

	/**
	 * Given a Gene Ontology Term ID and a Taxon as input, will return a collection of gene IDs that match the GO ID and Taxon.
	 * 
	 * @author gavin, klc 
	 * 
	 */

	public class GoTerm2GeneEndpoint extends AbstractGemmaEndpoint {

		private static Log log = LogFactory.getLog(Gene2GoTermEndpoint.class);

		private GeneOntologyService geneOntologyService;

		private GeneService geneService;
		
		private TaxonService taxonService;

		/**
		 * The local name of the expected request/response.
		 */
		public static final String GO2Gene_LOCAL_NAME = "goTerm2Gene";

		/**
		 * Sets the "business service" to delegate to.
		 */
		public void setGeneOntologyService(GeneOntologyService goS) {
			this.geneOntologyService = goS;
		}
		
		public void setGeneService(GeneService geneS){
			this.geneService = geneS;
		}
		
		public void settaxonService(TaxonService taxS){
			this.taxonService = taxS;
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
			setLocalName(GO2Gene_LOCAL_NAME);
			String goId = "";
			String taxonId = "";
			
			//get GO id from request
			Collection<String> goIdResult = getNodeValues(requestElement, "go_id");
			for (String id: goIdResult){
				goId = id;
			}
			
			//get taxon id from request
			Collection<String> taxonIdResult = getNodeValues(requestElement, "taxon_id");
			for (String id: taxonIdResult){
				taxonId = id;
			}
			

			//get gene from GO term
			Taxon taxon = taxonService.load(Long.parseLong(taxonId));
			Collection<Gene> genes = geneOntologyService.getGenes(goId, taxon);
			
			//build results in the form of a collection
			Collection<String> geneIds = new HashSet<String>();
			for (Gene gene : genes) {	
				geneIds.add(gene.getId().toString());
			}
			
			

			return buildWrapper(document, geneIds, "gene_id");
		}
		
		
	}



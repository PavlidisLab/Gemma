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

	import org.apache.commons.lang.StringUtils;
	import org.apache.commons.logging.Log;
	import org.apache.commons.logging.LogFactory;
	import org.springframework.util.Assert;
	import org.w3c.dom.Document;
	import org.w3c.dom.Element;
	import org.w3c.dom.Node;
	import org.w3c.dom.NodeList;
	import org.w3c.dom.Text;

	import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
	import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
	import ubic.gemma.model.common.quantitationtype.QuantitationType;
	import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
	import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
	
import ubic.gemma.model.genome.Gene;
	import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;

	/**
	 * Given the Taxon (eg. "1" for Homo Sapiens), will return all the Gene IDs that match the taxon.
	 * @author klc, gavin
	 * 
	 */

	public class GeneIDbyTaxonEndpoint extends AbstractGemmaEndpoint {

		private static Log log = LogFactory.getLog(GeneIDbyTaxonEndpoint.class);

		private GeneService geneService;
		private TaxonService taxonService;

		/**
		 * The local name of the expected request/response.
		 */
		private static final String EXPERIMENT_LOCAL_NAME = "geneIDbyTaxon";
		

		/**
		 * Sets the "business service" to delegate to.
		 */
		public void setGeneService(GeneService gs) {
			this.geneService = gs;
		}
		
		public void setTaxonService(TaxonService taxonService){
			this.taxonService = taxonService;
		}

		/**
		 * Reads the given <code>requestElement</code>, and sends the response
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
			
			setLocalName(EXPERIMENT_LOCAL_NAME);
			Collection<String> taxonResults = getNodeValues(requestElement, "taxon_id");
			String taxonId = "";
			
			for (String id: taxonResults){
				taxonId = id;
			}
			//Get Gene matched with Taxon
			Taxon tax = taxonService.load(Long.parseLong(taxonId));
			Collection<Gene> geneCollection = geneService.loadKnownGenes(tax);	
		   

			

			log.info("Finished. Sending response to client.");
				
			//build results in the form of a collection
			Collection<String> geneIds = new HashSet<String>();
			for (Gene gene : geneCollection) {	
				geneIds.add(gene.getId().toString());
			}
			
			

			return buildWrapper(document, geneIds, "gene_ids");
		}
		
		
		

	}






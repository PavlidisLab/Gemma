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

	import org.apache.commons.logging.Log;
	import org.apache.commons.logging.LogFactory;
	import org.springframework.util.Assert;
	import org.w3c.dom.Document;
	import org.w3c.dom.Element;
	import org.w3c.dom.Node;
	import org.w3c.dom.NodeList;
	import org.w3c.dom.Text;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Chromosome;
	import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneService;

	/**
	 * Given a gene id, will return physical location (chromosome #, nucleotide range: start and end)
	 * 
	 * Note: There may be >1 gene products for a particular gene id and hence >1 physical locations associated with 
	 * gene id.  Therefore, the nucleotide range will be the global max and min nucleotides for all the gene products.
	 * The max and min do not necessarily correspond to start and end because each gene product can be transcribed on
	 * either the +ve strand or the -ve strand (ie. opposite directions of transcription).
	 * @author gavin, klc
	 * 
	 */

	public class PhysicalLocationEndpoint extends AbstractGemmaEndpoint {

		private static Log log = LogFactory.getLog(GeneIdEndpoint.class);

		private GeneService geneService;
		
		private Long minNT;
		private Long maxNT;
		private Long chromId;
		private PhysicalLocation pLoc;
		/**
		 * The local name of the expected Request/Response.
		 */
		public static final String PLOC_LOCAL_NAME = "physicalLoc";

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

			
			setLocalName(PLOC_LOCAL_NAME);
			Collection<String> geneResults = getNodeValues(requestElement, "gene_id");
			String geneId = "";
			
			for (String id: geneResults){
				geneId = id;
			}
			
			//get the physical location of gene using GeneService
			Gene gene = geneService.load(Long.parseLong(geneId));
			geneService.thaw(gene);
			Collection<GeneProduct> gpCollection = gene.getProducts();
			
					
			Long nt;
			int ntLength;
			boolean chromCheck = false;
			String strand = null;
			int gpCount = 0;	//see how many gene products have physical locations annotated
			
			//each Gene Product 
			for (GeneProduct gp : gpCollection){
				//use PhysicalLocation accessor methods to get:
				//do we use getCdsPhysicalLocation() (from GeneProduct) or getPhysicalLocation() (from ChromosomeFeature)?
				pLoc = gp.getPhysicalLocation();
				if (pLoc != null){
					//nucleotide length
					ntLength = pLoc.getNucleotideLength();
					
					//nucleotide 
					nt = pLoc.getNucleotide();
					
					//strand (+ or -)
					strand = pLoc.getStrand();
					
					//see if start/end is max/min, then set minNT and maxNT					
					
					if (maxNT < getMaxNT(strand, nt, ntLength))
						maxNT = getMaxNT(strand, nt, ntLength);
					
					if (minNT > getMinNT(strand, nt, ntLength))
						minNT = getMinNT(strand, nt, ntLength);
					
					//chromosome id
					if (chromCheck == false){
					Chromosome chrom = pLoc.getChromosome();
					chromId = chrom.getId();
					chromCheck = true;
					}
					
					gpCount++;

				}
				
			
			} //for each gene product
		
			
//			if (minNT == -1 || maxNT == -1)
//				responseElement.appendChild(document.createTextNode("No nucleotide range for this physical location."));
			if (gpCount == 0){
				log.error("No physical locations annotated for any of the gene products of gene id: " +geneId+ ".");
				throw new Exception("No physical locations annotated for any of the gene products of gene id: " +geneId+ ".");
		}
			else {
			//build results in the form of a collection
			Collection<String> physLoc = new HashSet<String>();
			physLoc.add(Long.toString(chromId));
			physLoc.add(Long.toString(minNT));
			physLoc.add(Long.toString(maxNT));
			
			return buildWrapper(document, physLoc, "chromID_minNT_maxNT");
			}
			
	

			
		}
		
		private Long getMaxNT(String strand, Long nt, int ntLength){
			if (strand.equals("-"))
				return nt;
			else {
				return (nt + ntLength);
			}	
		}
		
		private Long getMinNT(String strand, Long nt, int ntLength){
			if (strand.equals("+"))
				return (nt - ntLength);
			else {
				return nt;
			}
		}

	}


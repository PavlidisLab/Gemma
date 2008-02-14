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

import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.analysis.service.AnalysisHelperService;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * Given an Expression Experiment ID, will return a collection of Design Element Data Vectors and the corresponding 
 * composite gene sequences.  
 * 
 * @author klc, gavin
 * 
 */

public class ExperimentDEDVEndpoint extends AbstractGemmaEndpoint {

	private static Log log = LogFactory.getLog(ExperimentDEDVEndpoint.class);

	private ExpressionExperimentService expressionExperimentService;
	private AnalysisHelperService analysisHelperService;
	private CompositeSequenceService compositeSequenceService;
	//private GeneService geneService;

	/**
	 * The local name of the expected request/response.
	 */
	private static final String EXPERIMENT_LOCAL_NAME = "experimentDEDV";
	private static final String DELIMITER = " ";

	/**
	 * Sets the "business service" to delegate to.
	 */
	public void setExpressionExperimentService(ExpressionExperimentService ees) {
		this.expressionExperimentService = ees;
	}
	
	public void setAnalysisHelperService(AnalysisHelperService analysisHelperService){
		this.analysisHelperService = analysisHelperService;
	}
	
//	public void setGeneService(GeneService geneService){
//		this.geneService = geneService;
//	}
	
	
	public void setCompositeSequenceService(CompositeSequenceService compositeSequenceService){
		this.compositeSequenceService = compositeSequenceService;
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
		setLocalName(EXPERIMENT_LOCAL_NAME);
		String eeid ="";

		Collection<String> eeResults  = getNodeValues(requestElement, "ee_id");
		

		for (String id: eeResults){
			eeid = id;
		}
		
		//Build the matrix
		ExpressionExperiment ee = expressionExperimentService.load(Long.parseLong(eeid));
		expressionExperimentService.thawLite(ee);
		
	    ExpressionDataDoubleMatrix dmatrix = analysisHelperService.getMaskedPreferredDataMatrix( ee );

	  //start building the wrapper
	    //build xml manually rather than use buildWrapper inherited from AbstractGemmeEndpoint
		String elementName1 = "dedv";
		String elementName2 = "geneIdist";
		
		
		Element responseWrapper = document.createElementNS(NAMESPACE_URI,
				EXPERIMENT_LOCAL_NAME);
		Element responseElement = document.createElementNS(NAMESPACE_URI,
				EXPERIMENT_LOCAL_NAME + RESPONSE);
		responseWrapper.appendChild(responseElement);

		if (dmatrix == null || (dmatrix.rows()==0))
			responseElement.appendChild(document
					.createTextNode("No "+elementName1 +" result"));
		else {
			
			for (int rowNum = 0; rowNum < dmatrix.rows(); rowNum++) {
				String elementString1 = encode(dmatrix.getRow(rowNum)); //data vector string for output
				String elementString2 = "";
				
				
				CompositeSequence de = (CompositeSequence)dmatrix.getDesignElementForRow(rowNum);
				Collection<Gene> geneCol = compositeSequenceService.getGenes(de);
				for (Gene gene:geneCol){
					if (elementString2.equals(""))
						elementString2 = elementString2.concat(gene.getId().toString());
					else 
						elementString2 = elementString2.concat(DELIMITER + gene.getId().toString());
				}
				
				
				Element e1 = document.createElement(elementName1);
				e1.appendChild(document.createTextNode(elementString1));
				responseElement.appendChild(e1);
				
				Element e2 = document.createElement(elementName2);
				e2.appendChild(document.createTextNode(elementString2));
				responseElement.appendChild(e2);
			}
		}
			
			
		log.info("Finished generating matrix. Sending response to client.");
		return responseWrapper;
	}
	
	
	/**
	 * Helper method that returns an array of data as a single string of the array elements, space delimited 
	 * @param data
	 * @return a string delimited representation of the double array passed in. 
	 */
	private String encode(Object[] data){
		
		StringBuffer result = new StringBuffer();
		
		for (int i = 0; i < data.length; i++) {
			if (i == 0)
				result.append(data[i]);
			else 
				result.append(DELIMITER + data[i]);
		}
		
		return result.toString();
	}

}

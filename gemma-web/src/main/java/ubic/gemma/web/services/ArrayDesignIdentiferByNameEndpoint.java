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
import java.util.Map;

	import org.apache.commons.lang.StringUtils;
	import org.apache.commons.logging.Log;
	import org.apache.commons.logging.LogFactory;

	import org.springframework.util.Assert;
	import org.w3c.dom.Document;
	import org.w3c.dom.Element;
	import org.w3c.dom.Node;
	import org.w3c.dom.NodeList;

	import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
	import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;

	/**
	 * array design short name -> return matching array design identifier
	 * 
	 * @author klc, gavin
	 * 
	 */

	public class ArrayDesignIdentiferByNameEndpoint extends AbstractGemmaEndpoint {

		private static Log log = LogFactory.getLog(ArrayDesignUsedEndpoint.class);

		private ArrayDesignService arrayDesignService;
		private SearchService searchService;

		/**
		 * The local name of the expected request.
		 */
		public static final String ARRAY_LOCAL_NAME = "arrayDesignIdentiferByName";
		

		/**
		 * Sets the "business service" to delegate to.
		 */
		public void setArrayDesignService(ArrayDesignService ads) {
			this.arrayDesignService = ads;
		}
		public void setSearchService(SearchService ss) {
			this.searchService = ss;
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
			setLocalName(ARRAY_LOCAL_NAME);
			String adName="";
			//get GO id from request
			Collection<String> adResult = getNodeValues(requestElement,"ad_name");
			for (String ad: adResult){
				adName = ad;
			}
			
			//using the SearchService to get array design(s) when given free text
			
//			Map<Class, List<SearchResult>> results = searchService.search(SearchSettings.ArrayDesignSearch(name));
//			
//			List<SearchResult> adResults = results.get(ArrayDesign.class);	
//						
//			if (adResults == null)
//				responseElement.appendChild(document
//						.createTextNode("No Array Design Service with that name."));
//	
//			else {
//				
//				//get array design identifier(s) and write it(them) to XML
//				for (SearchResult ad: adResults){
//					Element e = document.createElement("arrayDesignID");
//					//ad is a SearchResult object, but it will return the id of the returned entity...?
//					e.appendChild(document.createTextNode(ad.getId().toString()));
//					responseElement.appendChild(e);
//				}
//				
//			}

			//using the ArrayDesignService
			ArrayDesign ad = arrayDesignService.findByShortName( adName );
			
			//get Array Design ID and build results in the form of a collection
			Collection<String> adId = new HashSet<String>();
			adId.add(ad.getId().toString());
			
			

			return buildWrapper(document, adId, "arrayDesign_ids");
		}

	}

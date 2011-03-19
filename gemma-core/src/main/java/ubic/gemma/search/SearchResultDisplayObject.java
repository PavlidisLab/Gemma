/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;


import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.web.controller.common.auditAndSecurity.GeneSetValueObject;

/**
 * Object to store search results of different classes in a similar way for displaying to user
 * (ex: enables genes and gene sets to be entries in the same combo box)
 * 
 * object types handled are: Gene, GeneSet, ExpressionExperiment and ExpressionExperimentSet 
 * SearchObject is also handled if the object it holds is of any of those types
 * 
 * @author thea
 * @version $Id$
 */
public class SearchResultDisplayObject implements Comparable<SearchResultDisplayObject>  {
	
	/**
	 * Method to create a display object from scratch
	 * @param resultClass cannot be null
	 * @param id can be null
	 * @param name cannot be null
	 * @param description should not be null
	 * @param isGroup cannot be null
	 * @param size should not be null (should be 1 for non-groups)
	 * @param taxon can be null
	 * @param type can be null
	 */
	 public SearchResultDisplayObject(Class<?> resultClass, Long id, String name, 
			 String description, Boolean isGroup, int size, Taxon taxon, String type) {
	    	
	    	this.resultClass = resultClass;
	    	this.id = id;
	    	this.name = name;
	    	this.description = description;
	    	this.isGroup = isGroup;
	    	this.size = size;
	    	this.taxon = taxon;
	        this.type = type;
	 }
	 
	
    /**
     * 
     * @param searchResult
     */
    public SearchResultDisplayObject( SearchResult searchResult ) {

    	// if it's a search result, grab the underlying object
    	this.resultClass = searchResult.getResultClass();

        // class-specific construction
    	if(this.resultClass == Gene.class ){
        	Gene gene = (Gene) searchResult.getResultObject();
        	this.id = gene.getId();
        	this.isGroup = false;
        	this.size = 1;
        	this.taxon = gene.getTaxon();
        	this.name = gene.getOfficialSymbol();
        	this.description = gene.getOfficialName();
        	this.type = "gene";
        }else if(this.resultClass == GeneSet.class ){
        	GeneSet geneSet = (GeneSet) searchResult.getResultObject();
        	this.id = geneSet.getId();
        	this.isGroup = true;
        	this.size = (geneSet.getMembers()!=null)?geneSet.getMembers().size():null;
        	this.taxon = null;
        	this.name = geneSet.getName();
        	this.description = geneSet.getDescription();
        	this.type= "geneSet";
        }else if(this.resultClass == ExpressionExperiment.class){
        	ExpressionExperiment ee = (ExpressionExperiment) searchResult.getResultObject();
        	this.id = ee.getId();
        	this.isGroup = false;
        	this.size = 1;
        	this.taxon = null; //expressionExperimentService.getTaxon(this.id);
        	this.name = ee.getShortName(); 
        	this.description = ee.getName();
        	this.type = "experiment";
        }else if(this.resultClass == ExpressionExperimentSet.class){
        	ExpressionExperimentSet eeSet = (ExpressionExperimentSet) searchResult.getResultObject();
        	this.id = eeSet.getId();
        	this.isGroup = true; 
        	this.size = (eeSet.getExperiments()!=null)?eeSet.getExperiments().size():null;
        	this.taxon = null; //eeSet.getTaxon(); //ExpressionExperimentSetService eess; eess.thaw(eeSet);
        	this.name = eeSet.getName();
        	this.description = eeSet.getDescription();
        	this.type = "experimentSet";
        }else{
        	this.id = new Long(-1);
        	this.isGroup = false;
        	this.size = -1;
        	this.taxon = null;
        	this.name = "Unhandled type";
        	this.description = "Unhandled result type: "+this.resultClass;
        	this.type= this.getClass().getSimpleName();
        }
    }
    
   /**
    * 
    * @param gene
    */
    public SearchResultDisplayObject( Gene gene ) {
    	this.id = gene.getId();
    	this.resultClass = Gene.class;
        this.isGroup = false;
        this.size = 1;
    	this.taxon = gene.getTaxon();
    	this.name = gene.getOfficialSymbol();
    	this.description = gene.getOfficialName();
        this.type = "gene";
    }
    
   /**
    * 
    * @param geneSet
    */
    public SearchResultDisplayObject( GeneSet geneSet ) {
    	this.id = geneSet.getId();
    	this.resultClass = GeneSet.class;
    	this.isGroup = true;
    	this.size = (geneSet.getMembers()!=null)?geneSet.getMembers().size():null;
    	this.taxon = null;
    	this.name = geneSet.getName();
    	this.description = geneSet.getDescription();
        this.type = "geneSet";
    }
    /**
     * 
     * @param geneSet
     */
     public SearchResultDisplayObject( GeneSetValueObject geneSet ) {
     	this.id = (geneSet.isSession())? geneSet.getSessionId(): geneSet.getId();
     	this.resultClass = GeneSet.class;
     	this.isGroup = true;
     	this.size = (geneSet.getGeneIds()!=null)?geneSet.getGeneIds().size():null;
     	this.taxon = null;
     	this.name = geneSet.getName();
     	this.description = geneSet.getDescription();
        this.type = (geneSet.isSession())? "geneSetSession": "geneSet";
        this.memberIds = geneSet.getGeneIds();
     }

    /**
     * 
     * @param expressionExperiment
     */
    public SearchResultDisplayObject( ExpressionExperiment expressionExperiment) {
    	this.id = expressionExperiment.getId();
    	this.resultClass = ExpressionExperiment.class;
    	this.isGroup = false;
    	this.size = 1; 
    	this.taxon = null;  //expressionExperimentService.getTaxon(this.id);
    	this.name = expressionExperiment.getShortName();
    	this.description = expressionExperiment.getName();
        this.type = "experiment";
        
    }
    /**
     * 
     * @param expressionExperimentSet
     */
    public SearchResultDisplayObject( ExpressionExperimentSet expressionExperimentSet) {
    	this.id = expressionExperimentSet.getId();
    	this.resultClass = ExpressionExperimentSet.class;
    	this.isGroup = true;
    	this.size = (expressionExperimentSet.getExperiments()!=null)?expressionExperimentSet.getExperiments().size():null;
    	this.taxon = expressionExperimentSet.getTaxon();
    	this.name = expressionExperimentSet.getName();
    	this.description = expressionExperimentSet.getDescription();
        this.type = "experimentSet";
    }


    private Class<?> resultClass;
    
    private Long id; 
    
    private Boolean isGroup; // whether this search result represents a group of entities or not
    
    //private String text; // the text to show to user when displaying search results, how this is built depends on object type
    
    private String name;
    
    private String description;
    
    private int size; // the number of items; 1 if not a group
    
    private Taxon taxon; // the id of the associated taxon, can be null
   /**
    * only used with geneSet value object to support session-bound groups
    */
    private Collection<Long> memberIds = new HashSet<Long>(); 
    
    private String type;

    public Class<?> getResultClass() {
        return this.resultClass;
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Boolean getIsGroup() {
        return this.isGroup;
    }
    public String getName() {
        return this.name;
    }
    public String getDescription() {
        return this.description;
    }
    public int getSize() {
        return this.size;
    }
    public Taxon getTaxon() {
        return this.taxon;
    }
    public void setTaxon(Taxon taxon) {
        this.taxon = taxon;
    }
    public String getType(){
    	return this.type;
    }
    public void setType(String type){
    	this.type = type;
    }
    public Collection<Long> getMemberIds(){
    	return this.memberIds;
    }
    public void setMemberIds(Collection<Long> memberIds){
    	this.memberIds = memberIds;
    }
    /**
     * Creates a collection of SearchResultDisplayObjects from a collection of objects
     * object types handled are: Gene, GeneSet, ExpressionExperiment, ExpressionExperimentSet and SearchObjects
     * containing an object of any of those types
     * @param results a collection of SearchResult objects to create SearchResultDisplayObjects for
     * @return a collection of SearchResultDisplayObjects created from the objects passed in, sorted by name
     */
    public static Collection<SearchResultDisplayObject> convertSearchResults2SearchResultDisplayObjects( List<SearchResult> results){
    	
    	// collection of SearchResultDisplayObjects to return
    	List<SearchResultDisplayObject> searchResultDisplayObjects = new ArrayList<SearchResultDisplayObject>();
    	
    	if(results!=null && results.size()>0){
    		// for every object passed in, create a SearchResultDisplayObject
    		for (SearchResult result : results){
    			searchResultDisplayObjects.add(new SearchResultDisplayObject(result));
    		}
    	}
    	Collections.sort(searchResultDisplayObjects);
    	
    	return searchResultDisplayObjects;
    }


	@Override
	public int compareTo(SearchResultDisplayObject o) {
		int result = this.name.compareTo(o.name);
		return (result == 0)? this.description.compareTo(o.description):result;
	}
    
  /*  public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }*/

}

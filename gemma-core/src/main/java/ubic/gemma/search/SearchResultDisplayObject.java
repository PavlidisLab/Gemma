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

import ubic.gemma.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.genome.gene.GOGroupValueObject;
import ubic.gemma.genome.gene.SessionBoundGeneSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * Object to store search results of different classes in a similar way for displaying to user (ex: enables genes and
 * gene sets to be entries in the same combo box) 
 * object types handled are: Gene, GeneSet, GeneSetValueObject, ExpressionExperiment and ExpressionExperimentSet
 * SearchObject is also handled if the object it holds is of any of those types 
 * 
 * for a gene or experiment, the memberIds field is a collection just containing the object's id
 * 
 * @author thea
 * @version $Id$
 */
public class SearchResultDisplayObject implements Comparable<SearchResultDisplayObject> {
    
    private Class<?> resultClass;
    
    //private boolean isSession;

    private Boolean isGroup; // whether this search result represents a group of entities or not

    private String name;

    private String description;

    private int size; // the number of items; 1 if not a group

    private String taxonName; // the common name of the associated taxon

    private Long taxonId;

    /**
     * for genes and experiments, the memeberIds field is a collection containing just their id
     */
    private Collection<Long> memberIds = new HashSet<Long>(); 

    private Object resultValueObject;
    
    private boolean userOwned = false;
    

    /**
     * satisfy javaBean contract
     */
    public SearchResultDisplayObject() {
    }
    /**
     * this method does not set the publik variable for the returned object (cannot autowire security service from here)
     * @param searchResult
     */
    public SearchResultDisplayObject( SearchResult searchResult ) {

        // if it's a search result, grab the underlying object
        Class<?> searchResultClass = searchResult.getResultClass();
        // class-specific construction
        if ( searchResult.getResultObject() instanceof GeneValueObject ) {
            GeneValueObject gene = ( GeneValueObject ) searchResult.getResultObject();
            setValues( gene );
        } else if ( searchResult.getResultObject() instanceof GeneSetValueObject ) {
            GeneSetValueObject geneSet = ( GeneSetValueObject ) searchResult.getResultObject();
            setValues( geneSet );
        } else if ( searchResult.getResultObject() instanceof ExpressionExperimentValueObject ) {
            ExpressionExperimentValueObject ee = ( ExpressionExperimentValueObject ) searchResult.getResultObject();
            setValues( ee );
        } else if ( searchResult.getResultObject() instanceof ExpressionExperimentSetValueObject ) {
            ExpressionExperimentSetValueObject eeSet = ( ExpressionExperimentSetValueObject ) searchResult.getResultObject();
            setValues( eeSet );
        } else {
            this.isGroup = false;
            this.size = -1;
            this.taxonId = new Long( -1 );
            this.taxonName = "unknown";
            this.name = "Unhandled type";
            this.description = "Unhandled result type: " + searchResultClass;
            this.memberIds = null;
        }
    }

    /**
     * @param gene
     */
    public SearchResultDisplayObject( GeneValueObject gene ) {
        setValues( gene );
    }

    /**
     * @param geneSet
     */
    public SearchResultDisplayObject( DatabaseBackedGeneSetValueObject geneSet ) {
        setValues( geneSet );
    }
    
    /**
     * @param geneSet
     */
    public SearchResultDisplayObject( GeneSetValueObject geneSet ) {
        setValues( geneSet );
    }
    
    /**
     * @param geneSet
     */
    public SearchResultDisplayObject( SessionBoundGeneSetValueObject geneSet ) {
        setValues( geneSet );
    }

    /**
     * @param expressionExperimentSet
     */
    public SearchResultDisplayObject( ExpressionExperimentValueObject expressionExperiment ) {
        setValues( expressionExperiment );
    }
    
    /**
     * @param expressionExperimentSet
     */
    public SearchResultDisplayObject( ExpressionExperimentSetValueObject expressionExperimentSet ) {
        setValues( expressionExperimentSet );
    }

    /**
     * @param gene
     */
    private void setValues( GeneValueObject gene ) {
        setResultValueObject( gene );
        this.isGroup = false;
        this.size = 1;
        this.taxonId = gene.getTaxonId();
        this.taxonName = gene.getTaxonCommonName();
        this.name = gene.getOfficialSymbol();
        this.description = gene.getOfficialName();
        this.memberIds.add(gene.getId());
    }

    /**
     * @param geneSet
     */
    private void setValues( GeneSetValueObject geneSet ) {
        this.isGroup = true;
        this.size = ( geneSet.getGeneIds() != null ) ? geneSet.getGeneIds().size() : null;
        this.taxonId = geneSet.getTaxonId();
        this.taxonName = geneSet.getTaxonName();
        this.name = geneSet.getName();
        this.description = geneSet.getDescription();
        this.memberIds = geneSet.getGeneIds();
        this.setResultValueObject( geneSet );
    }
    
    /**
     * @param geneSet
     */
    private void setValues( ExpressionExperimentSetValueObject eeSet ) {
        this.isGroup = true;
        this.size = ( eeSet.getExpressionExperimentIds() != null ) ? eeSet.getExpressionExperimentIds().size() : null;
        this.taxonId = eeSet.getTaxonId();
        this.taxonName = eeSet.getTaxonName();
        this.name = eeSet.getName();
        this.description = eeSet.getDescription();
        this.memberIds = eeSet.getExpressionExperimentIds();
        this.setResultValueObject( eeSet );
    }

    /**
     * @param expressionExperiment
     */
    private void setValues( ExpressionExperimentValueObject expressionExperiment ) {
        this.isGroup = false;
        this.size = 1;
        this.taxonId = null;
        this.taxonName = null;
        this.name = expressionExperiment.getShortName();
        this.description = expressionExperiment.getName();
        this.memberIds.add(expressionExperiment.getId());
        setResultValueObject( expressionExperiment );
    }
     
    public Class<?> getResultClass() {
        return this.resultClass;
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

    public Long getTaxonId() {
        return this.taxonId;
    }

    public void setTaxonId( Long id ) {
        this.taxonId = id;
    }

    public String getTaxonName() {
        return this.taxonName;
    }

    public void setTaxonName( String name ) {
        this.taxonName = name;
    }

    public Collection<Long> getMemberIds() {
        return this.memberIds;
    }

    public void setMemberIds( Collection<Long> memberIds ) {
        this.memberIds = memberIds;
    }
    

    /**
     * @param resultValueObject the resultValueObject to set
     */
    public void setResultValueObject( Object resultValueObject ) {
        this.resultValueObject = resultValueObject;
        this.resultClass = resultValueObject.getClass();
    }

    /**
     * @return the resultValueObject
     */
    public Object getResultValueObject() {
        return resultValueObject;
    }

    /**
     * @param userOwned the userOwned to set
     */
    public void setUserOwned( boolean userOwned ) {
        this.userOwned = userOwned;
    }

    /**
     * @return the userOwned
     */
    public boolean isUserOwned() {
        return userOwned;
    }

    /**
     * Creates a collection of SearchResultDisplayObjects from a collection of objects. Object types handled are: GeneValueObject,
     * GeneSetValueObject, ExpressionExperimentValueObject, ExpressionExperimentSetValueObject and SearchObjects containing 
     * an object of any of those types
     * 
     * @param results a collection of SearchResult objects to create SearchResultDisplayObjects for
     * @return a collection of SearchResultDisplayObjects created from the objects passed in, sorted by name
     */
    public static List<SearchResultDisplayObject> convertSearchResults2SearchResultDisplayObjects(
            List<SearchResult> results ) {

        // collection of SearchResultDisplayObjects to return
        List<SearchResultDisplayObject> searchResultDisplayObjects = new ArrayList<SearchResultDisplayObject>();

        if ( results != null && results.size() > 0 ) {
            // for every object passed in, create a SearchResultDisplayObject
            for ( SearchResult result : results ) {
                searchResultDisplayObjects.add( new SearchResultDisplayObject( result ) );
            }
        }
        Collections.sort( searchResultDisplayObjects );

        return searchResultDisplayObjects;
    }

    @Override
    public int compareTo( SearchResultDisplayObject o ) {
        if(o.name == null || o.description== null){
            return 1;
        }
        if(this.name == null || this.description== null){
            return -1;
        }
        // sort GO groups by their text name, not their GO id
        if(o.getResultValueObject() instanceof GOGroupValueObject){
            int result = this.description.toLowerCase().compareTo( o.description.toLowerCase() );
            return ( result == 0 ) ? this.name.toLowerCase().compareTo( o.name.toLowerCase() ) : result;    
        }
        // sort experiments by their text name, not their GSE id
        if(o.getResultValueObject() instanceof ExpressionExperimentValueObject){
            int result = this.description.toLowerCase().compareTo( o.description.toLowerCase() );
            return ( result == 0 ) ? this.name.toLowerCase().compareTo( o.name.toLowerCase() ) : result;    
        }
        int result = this.name.toLowerCase().compareTo( o.name.toLowerCase() );
        return ( result == 0 ) ? this.description.toLowerCase().compareTo( o.description.toLowerCase() ) : result;
    }

}

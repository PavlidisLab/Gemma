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

import ubic.gemma.model.Reference;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.search.SearchResult;

/**
 * Object to store search results of different classes in a similar way for displaying to user (ex: enables genes and
 * gene sets to be entries in the same combo box) 
 * object types handled are: Gene, GeneSet, GeneSetValueObject, ExpressionExperiment and ExpressionExperimentSet
 * SearchObject is also handled if the object it holds is of any of those types 
 * 
 * 
 * @author thea
 * @version $Id$
 */
public class SearchResultDisplayObject implements Comparable<SearchResultDisplayObject> {

    /**
     * Method to create a display object from scratch
     * 
     * @param resultClass cannot be null
     * @param reference can be null if result record is temporary (ex GO groups)
     * @param name cannot be null
     * @param description should not be null
     * @param isGroup cannot be null
     * @param size should not be null (should be 1 for non-groups)
     * @param taxonId can be null
     * @param taxonName can be null
     * @param type can be null
     * @param memberIds can be null
     */
    public SearchResultDisplayObject( Class<?> resultClass, Reference reference, String name, 
            String description, Boolean isGroup, int size, Long taxonId, String taxonName,String type, 
            Collection<Long> memberIds ) {

        this.resultClass = resultClass;
        this.reference = reference;
        this.name = name;
        this.description = description;
        this.isGroup = isGroup;
        this.size = size;
        this.taxonId = taxonId;
        this.taxonName = taxonName;
        this.type = type;
        this.memberIds = memberIds;
    }

    /**
     * @param searchResult
     */
    public SearchResultDisplayObject( SearchResult searchResult ) {

        // if it's a search result, grab the underlying object
        this.resultClass = searchResult.getResultClass();

        // class-specific construction
        if ( this.resultClass == Gene.class ) {
            Gene gene = ( Gene ) searchResult.getResultObject();
            setValues( gene );
        } else if ( this.resultClass == GeneSet.class ) {
            GeneSet geneSet = ( GeneSet ) searchResult.getResultObject();
            setValues( geneSet );
        } else if ( this.resultClass == ExpressionExperiment.class ) {
            ExpressionExperiment ee = ( ExpressionExperiment ) searchResult.getResultObject();
            setValues( ee );
        } else if ( this.resultClass == ExpressionExperimentSet.class ) {
            ExpressionExperimentSet eeSet = ( ExpressionExperimentSet ) searchResult.getResultObject();
            setValues( eeSet );
        } else {
            this.reference = null;
            this.isGroup = false;
            this.size = -1;
            this.taxonId = new Long( -1 );
            this.taxonName = "unknown";
            this.name = "Unhandled type";
            this.description = "Unhandled result type: " + this.resultClass;
            this.type = this.getClass().getSimpleName();
            this.memberIds = null;
        }
    }

    /**
     * @param gene
     */
    public SearchResultDisplayObject( Gene gene ) {
        setValues( gene );
    }

    /**
     * @param geneSet
     */
    public SearchResultDisplayObject( GeneSet geneSet ) {
        setValues( geneSet );
    }

    /**
     * @param geneSet
     */
    public SearchResultDisplayObject( GeneSetValueObject geneSet ) {
        setValues( geneSet );
    }

    /**
     * @param expressionExperiment
     */
    public SearchResultDisplayObject( ExpressionExperiment expressionExperiment ) {
        setValues( expressionExperiment );

    }

    /**
     * @param expressionExperimentSet
     */
    public SearchResultDisplayObject( ExpressionExperimentSet expressionExperimentSet ) {
        setValues( expressionExperimentSet );
    }

    /**
     * @param gene
     */
    private void setValues( Gene gene ) {
        this.reference = new Reference( gene.getId(), Reference.DB_GENE );
        this.resultClass = Gene.class;
        this.isGroup = false;
        this.size = 1;
        this.taxonId = ( gene.getTaxon() != null ) ? gene.getTaxon().getId() : null;
        this.taxonName = ( gene.getTaxon() != null ) ? gene.getTaxon().getCommonName() : null;
        this.name = gene.getOfficialSymbol();
        this.description = gene.getOfficialName();
        this.type = "gene";
        this.memberIds = null;
    }

    /**
     * @param geneSet
     */
    private void setValues( GeneSet geneSet ) {
        this.reference = new Reference(geneSet.getId(), Reference.DATABASE_BACKED_GROUP);
        this.resultClass = GeneSet.class;
        this.isGroup = true;
        this.size = ( geneSet.getMembers() != null ) ? geneSet.getMembers().size() : null;
        this.taxonId = null;
        this.taxonName = null;
        this.name = geneSet.getName();
        this.description = geneSet.getDescription();
        this.type = "geneSet";
        for ( GeneSetMember gm : geneSet.getMembers() ) {
            this.memberIds.add( gm.getGene().getId() );
        }
    }

    /**
     * @param geneSet
     */
    private void setValues( GeneSetValueObject geneSet ) {
        if(geneSet.getReference() == null){
            if(geneSet.isSessionBound()){
                this.reference = new Reference(geneSet.getId(), Reference.SESSION_BOUND_GROUP) ;
            }else{
                this.reference = new Reference(geneSet.getId(), Reference.DATABASE_BACKED_GROUP) ;
            }
        }else{
            this.reference = geneSet.getReference();
        }
        this.resultClass = GeneSet.class;
        this.isGroup = true;
        this.size = ( geneSet.getGeneIds() != null ) ? geneSet.getGeneIds().size() : null;
        this.taxonId = geneSet.getTaxonId();
        this.taxonName = geneSet.getTaxonName();
        this.name = geneSet.getName();
        this.description = geneSet.getDescription();
        this.type = ( geneSet.isSessionBound() ) ? "geneSetSession" : "geneSet";
        this.memberIds = geneSet.getGeneIds();
    }

    /**
     * @param expressionExperiment
     */
    private void setValues( ExpressionExperiment expressionExperiment ) {
        this.reference = new Reference( expressionExperiment.getId(), Reference.DB_EXPERIMENT ) ;
        this.resultClass = ExpressionExperiment.class;
        this.isGroup = false;
        this.size = 1;
        this.taxonId = null;
        this.taxonName = null;
        this.name = expressionExperiment.getShortName();
        this.description = expressionExperiment.getName();
        this.type = "experiment";
        this.memberIds = null;

    }

    /**
     * @param expressionExperimentSet
     */
    private void setValues( ExpressionExperimentSet expressionExperimentSet ) {
 ///       this.id = expressionExperimentSet.getId();
        this.reference =  new Reference( expressionExperimentSet.getId(), Reference.DATABASE_BACKED_GROUP ) ;
        this.resultClass = ExpressionExperimentSet.class;
        this.isGroup = true;
        this.size = ( expressionExperimentSet.getExperiments() != null ) ? expressionExperimentSet.getExperiments()
                .size() : null;
        this.taxonName = ( expressionExperimentSet.getTaxon() != null ) ? expressionExperimentSet.getTaxon()
                .getCommonName() : null;
        this.taxonId = ( expressionExperimentSet.getTaxon() != null ) ? expressionExperimentSet.getTaxon().getId()
                : null;
        this.name = expressionExperimentSet.getName();
        this.description = expressionExperimentSet.getDescription();
        this.type = "experimentSet";
        for ( BioAssaySet bas : expressionExperimentSet.getExperiments() ) {
            this.memberIds.add( bas.getId() );
        }
    }

    private Class<?> resultClass;
    
    private Reference reference;

    //private boolean isSession;

    private Boolean isGroup; // whether this search result represents a group of entities or not

    private String name;

    private String description;

    private int size; // the number of items; 1 if not a group

    private String taxonName; // the common name of the associated taxon

    private Long taxonId;

    private Collection<Long> memberIds = new HashSet<Long>();

    private String type;
    
    public Class<?> getResultClass() {
        return this.resultClass;
    }
  
    public Reference getReference() {
        return this.reference;
    }

    public void setReference( Reference reference ) {
        this.reference = reference;
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

    public String getType() {
        return this.type;
    }

    public void setType( String type ) {
        this.type = type;
    }

    public Collection<Long> getMemberIds() {
        return this.memberIds;
    }

    public void setMemberIds( Collection<Long> memberIds ) {
        this.memberIds = memberIds;
    }
    

    /**
     * Creates a collection of SearchResultDisplayObjects from a collection of objects object types handled are: Gene,
     * GeneSet, ExpressionExperiment, ExpressionExperimentSet and SearchObjects containing an object of any of those
     * types
     * 
     * @param results a collection of SearchResult objects to create SearchResultDisplayObjects for
     * @return a collection of SearchResultDisplayObjects created from the objects passed in, sorted by name
     */
    public static Collection<SearchResultDisplayObject> convertSearchResults2SearchResultDisplayObjects(
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
        if(o.getType()=="GOgroup"){
            int result = this.description.toLowerCase().compareTo( o.description.toLowerCase() );
            return ( result == 0 ) ? this.name.toLowerCase().compareTo( o.name.toLowerCase() ) : result;    
        }
        // sort experiments by their text name, not their GSE id
        if(o.getType()=="experiment"){
            int result = this.description.toLowerCase().compareTo( o.description.toLowerCase() );
            return ( result == 0 ) ? this.name.toLowerCase().compareTo( o.name.toLowerCase() ) : result;    
        }
        int result = this.name.toLowerCase().compareTo( o.name.toLowerCase() );
        return ( result == 0 ) ? this.description.toLowerCase().compareTo( o.description.toLowerCase() ) : result;
    }

}

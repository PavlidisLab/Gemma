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
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.search.SearchResult;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentSetValueObject;

/**
 * Object to store search results of different classes in a similar way for displaying to user (ex: enables genes and
 * gene sets to be entries in the same combo box) object types handled are: Gene, GeneSet, GeneSetValueObject,
 * ExpressionExperiment and ExpressionExperimentSet SearchObject is also handled if the object it holds is of any of
 * those types sessionId field is the unique id that a search result is given when session-bound and db-backed entities
 * will be displayed together if a geneSet is session-bound (has the type: "usergeneSetSession"), then id=sessionId if a
 * geneSet is db-backed (has the type: "geneSet" or "usergeneSet"), then id is the database id for the set and sessionId
 * is the id used by the store
 * 
 * @author thea
 * @version $Id$
 */
public class SearchResultDisplayObject implements Comparable<SearchResultDisplayObject> {

    /**
     * Method to create a display object from scratch
     * 
     * @param resultClass cannot be null
     * @param id can be null
     * @param sessionId can be null
     * @param name cannot be null
     * @param description should not be null
     * @param isGroup cannot be null
     * @param size should not be null (should be 1 for non-groups)
     * @param taxonId can be null
     * @param taxonName can be null
     * @param type can be null
     * @param memberIds can be null
     */
    public SearchResultDisplayObject( Class<?> resultClass, Long id, String name, String description, Boolean isGroup,
            int size, Long taxonId, String taxonName,String type, Collection<Long> memberIds ) {

        this.resultClass = resultClass;
        this.id = id;
        this.sessionId = id;
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
            this.id = new Long( -1 );
            this.sessionId = this.getId();
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
     * @param expressionExperiment
     */
    public SearchResultDisplayObject( ExpressionExperimentSetValueObject expressionExperimentSet ) {
        setValues( expressionExperimentSet );

    }

    /**
     * @param gene
     */
    private void setValues( Gene gene ) {
        this.id = gene.getId();
        this.sessionId = this.getId();
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
        this.id = geneSet.getId();
        this.sessionId = this.getId();
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
        this.id = geneSet.getId();
        this.sessionId = ( geneSet.getSessionId() != null ) ? geneSet.getSessionId() : geneSet.getId();
        this.resultClass = GeneSet.class;
        this.isGroup = true;
        this.size = ( geneSet.getGeneIds() != null ) ? geneSet.getGeneIds().size() : null;
        this.taxonId = geneSet.getTaxonId();
        this.taxonName = geneSet.getTaxonName();
        this.name = geneSet.getName();
        this.description = geneSet.getDescription();
        this.type = ( geneSet.isSession() ) ? "geneSetSession" : "geneSet";
        this.memberIds = geneSet.getGeneIds();
    }

    /**
     * @param expressionExperiment
     */
    private void setValues( ExpressionExperiment expressionExperiment ) {
        this.id = expressionExperiment.getId();
        this.sessionId = this.getId();
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
        this.id = expressionExperimentSet.getId();
        this.sessionId = this.getId();
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
    
    /**
     * @param expressionExperimentSet
     */
    private void setValues( ExpressionExperimentSetValueObject expressionExperimentSet ) {
        this.id = expressionExperimentSet.getId();
        this.sessionId = this.getId();
        this.resultClass = ExpressionExperimentSet.class;
        this.isGroup = true;
        this.size = expressionExperimentSet.getExpressionExperimentIds().size();
        this.taxonName = expressionExperimentSet.getTaxonName();
        this.taxonId = expressionExperimentSet.getTaxonId();
        this.name = expressionExperimentSet.getName();
        this.description = expressionExperimentSet.getDescription();
        this.type = "experimentSet";
        this.memberIds = expressionExperimentSet.getExpressionExperimentIds();
    }

    private Class<?> resultClass;

    private Long id;

    private Long sessionId;

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

    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public Long getSessionId() {
        return this.sessionId;
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
        int result = this.name.toLowerCase().compareTo( o.name.toLowerCase() );
        return ( result == 0 ) ? this.description.toLowerCase().compareTo( o.description.toLowerCase() ) : result;
    }

}

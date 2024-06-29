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
package ubic.gemma.core.search;

import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GOGroupValueObject;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.SessionBoundGeneSetValueObject;

import java.util.*;

/**
 * Object to store search results of different classes in a similar way for displaying to user (ex: enables genes and
 * gene sets to be entries in the same combo box) object types handled are: Gene, GeneSet, GeneSetValueObject,
 * ExpressionExperiment and ExpressionExperimentSet SearchObject is also handled if the object it holds is of any of
 * those types for a gene or experiment, the memberIds field is a collection just containing the object's id. memberIds
 * is just for convenience on the client.
 * In effect this wraps the resultValueObject.
 *
 * @author thea
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public class SearchResultDisplayObject implements Comparable<SearchResultDisplayObject> {

    private String description;

    // private boolean isSession;
    private Long id;
    /**
     * whether this search result represents a group of entities or not (the resultValueObject)
     */
    private Boolean isGroup;
    /**
     * for genes and experiments, the memeberIds field is a collection containing just their id. This is primarly used
     * as a convenience on the client.
     */
    private Collection<Long> memberIds = new HashSet<>();
    private String name;
    // the query exactly as entered by the user.
    private String originalQuery;

    private Class<?> resultClass;
    /**
     * The actual underlying valueobject; class is of resultClass.
     */
    private Object resultValueObject;
    private int size; // the number of items; 1 if not a group
    private Long taxonId;
    private String taxonName; // the common name of the associated taxon
    private boolean userOwned = false;

    /**
     * satisfy javaBean contract
     */
    public SearchResultDisplayObject() {
    }

    public SearchResultDisplayObject( Object entity ) {

        if ( ExpressionExperimentSetValueObject.class.isAssignableFrom( entity.getClass() ) ) {
            this.setValues( ( ExpressionExperimentSetValueObject ) entity );
        } else if ( Gene.class.isAssignableFrom( entity.getClass() ) ) {
            this.setValues( ( Gene ) entity );
        } else if ( GeneSetValueObject.class.isAssignableFrom( entity.getClass() ) ) {
            this.setValues( ( GeneSetValueObject ) entity );
        } else if ( GeneValueObject.class.isAssignableFrom( entity.getClass() ) ) {
            this.setValues( ( GeneValueObject ) entity );
        } else if ( ExpressionExperimentValueObject.class.isAssignableFrom( entity.getClass() ) ) {
            this.setValues( ( ExpressionExperimentValueObject ) entity );
        } else if ( SearchResult.class.isAssignableFrom( entity.getClass() ) ) {
            this.setValues( ( SearchResult<?> ) entity );
        } else {
            throw new UnsupportedOperationException( entity.getClass() + " not supported" );
        }
    }

    public SearchResultDisplayObject( SessionBoundGeneSetValueObject geneSet ) {
        this.setValues( geneSet );
    }

    /**
     * Creates a collection of SearchResultDisplayObjects from a collection of objects. Object types handled are:
     * GeneValueObject, GeneSetValueObject, ExpressionExperimentValueObject, ExpressionExperimentSetValueObject and
     * SearchObjects containing an object of any of those types
     *
     * @param  results a collection of SearchResult objects to create SearchResultDisplayObjects for
     * @return a collection of SearchResultDisplayObjects created from the objects passed in, sorted by name
     */
    public static <T extends Identifiable> List<SearchResultDisplayObject> convertSearchResults2SearchResultDisplayObjects(
            @Nullable List<SearchResult<T>> results ) {

        // collection of SearchResultDisplayObjects to return
        List<SearchResultDisplayObject> searchResultDisplayObjects = new ArrayList<>();

        if ( results != null && results.size() > 0 ) {
            // for every object passed in, create a SearchResultDisplayObject
            for ( SearchResult<T> result : results ) {
                searchResultDisplayObjects.add( new SearchResultDisplayObject( result ) );
            }
        }
        Collections.sort( searchResultDisplayObjects );

        return searchResultDisplayObjects;
    }

    @Override
    public int compareTo( SearchResultDisplayObject o ) {
        if ( o.name == null || o.description == null ) {
            return 1;
        }
        if ( this.name == null || this.description == null ) {
            return -1;
        }
        // sort GO groups by their text name, not their GO id
        if ( o.getResultValueObject() instanceof GOGroupValueObject ) {
            int result = this.description.toLowerCase().compareTo( o.description.toLowerCase() );
            return ( result == 0 ) ? this.name.toLowerCase().compareTo( o.name.toLowerCase() ) : result;
        }
        // sort experiments by their text name, not their GSE id
        if ( o.getResultValueObject() instanceof ExpressionExperimentValueObject ) {
            int result = this.description.toLowerCase().compareTo( o.description.toLowerCase() );
            return ( result == 0 ) ? this.name.toLowerCase().compareTo( o.name.toLowerCase() ) : result;
        }
        int result = this.name.toLowerCase().compareTo( o.name.toLowerCase() );
        return ( result == 0 ) ? this.description.toLowerCase().compareTo( o.description.toLowerCase() ) : result;
    }

    public String getDescription() {
        return this.description;
    }

    public Boolean getIsGroup() {
        return this.isGroup;
    }

    public Collection<Long> getMemberIds() {
        return this.memberIds;
    }

    public String getName() {
        return this.name;
    }

    public String getOriginalQuery() {
        return originalQuery;
    }

    public void setOriginalQuery( String originalQuery ) {
        this.originalQuery = originalQuery;
    }

    public Class<?> getResultClass() {
        return this.resultClass;
    }

    /**
     * @return the resultValueObject, which will be (for the example of genes) a GeneValueObject or a
     *         GeneSetValueObject, which also has several subclasses (SessionBound etc.)
     */
    public Object getResultValueObject() {
        return resultValueObject;
    }

    /**
     * @param resultValueObject the resultValueObject to set
     */
    private void setResultValueObject( Object resultValueObject ) {
        this.resultValueObject = resultValueObject;
        this.resultClass = resultValueObject.getClass();
    }

    public int getSize() {
        return this.size;
    }

    /*
     * DO NOT USE :) Size is inferred from the wrapped resultValueObject.
     */
    @SuppressWarnings("unused")
    private void setSize( int size ) {
        this.size = size;
    }

    public Long getTaxonId() {
        return this.taxonId;
    }

    public void setTaxonId( Long id ) {
        this.taxonId = id;
    }

    // Do not allow setting this directly.
    // public void setMemberIds( Collection<Long> memberIds ) {
    // this.memberIds = memberIds;
    // }

    public String getTaxonName() {
        return this.taxonName;
    }

    public void setTaxonName( String name ) {
        this.taxonName = name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        result = prime * result + ( ( resultValueObject == null ) ? 0 : resultValueObject.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null ) {
            return false;
        }
        if ( this.getClass() != obj.getClass() ) {
            return false;
        }
        SearchResultDisplayObject other = ( SearchResultDisplayObject ) obj;
        if ( id == null ) {
            if ( other.id != null ) {
                return false;
            }
        } else if ( !id.equals( other.id ) ) {
            return false;
        }
        if ( name == null ) {
            if ( other.name != null ) {
                return false;
            }
        } else if ( !name.equals( other.name ) ) {
            return false;
        }
        if ( resultValueObject == null ) {
            return other.resultValueObject == null;
        }
        return resultValueObject.equals( other.resultValueObject );
    }

    /**
     * @return the userOwned
     */
    public boolean isUserOwned() {
        return userOwned;
    }

    /**
     * @param userOwned the userOwned to set
     */
    public void setUserOwned( boolean userOwned ) {
        this.userOwned = userOwned;
    }

    private void setValues( ExpressionExperimentSetValueObject eeSet ) {
        this.isGroup = true;
        this.size = eeSet.getSize();
        this.taxonId = eeSet.getTaxonId();
        this.taxonName = eeSet.getTaxonName();
        this.name = eeSet.getName();
        this.description = eeSet.getDescription();
        // this.memberIds = eeSet.getExpressionExperimentIds(); // might not be filled in.
        this.id = eeSet.getId();
        this.setResultValueObject( eeSet );
    }

    private void setValues( ExpressionExperimentValueObject expressionExperiment ) {
        this.isGroup = false;
        this.size = 1;
        if ( expressionExperiment.getTaxonObject() != null ) {
            this.taxonId = expressionExperiment.getTaxonObject().getId();
        }

        this.taxonName = expressionExperiment.getTaxon();
        this.name = expressionExperiment.getShortName();
        this.description = expressionExperiment.getName();
        // .memberIds.add( expressionExperiment.getId() );
        this.id = expressionExperiment.getId();
        this.setResultValueObject( expressionExperiment );
    }

    private void setValues( Gene gene ) {
        this.setResultValueObject( new GeneValueObject( gene ) );
        this.isGroup = false;
        this.size = 1;
        if ( gene.getTaxon() != null ) {
            this.taxonId = gene.getTaxon().getId();
            this.taxonName = gene.getTaxon().getCommonName();

        }

        this.name = gene.getOfficialSymbol();
        this.description = gene.getOfficialName();
        this.memberIds.add( gene.getId() );
        this.id = gene.getId();
    }

    private void setValues( GeneSetValueObject geneSet ) {
        this.isGroup = true;
        this.size = geneSet.getSize().intValue();
        this.taxonId = geneSet.getTaxonId();
        this.taxonName = geneSet.getTaxonName();
        this.name = geneSet.getName();
        this.description = geneSet.getDescription();
        this.memberIds = geneSet.getGeneIds();
        this.id = geneSet.getId();
        this.setResultValueObject( geneSet );
    }

    private void setValues( GeneValueObject gene ) {
        this.setResultValueObject( gene );
        this.isGroup = false;
        this.size = 1;
        this.taxonId = gene.getTaxonId();
        this.taxonName = gene.getTaxonCommonName();
        this.name = gene.getOfficialSymbol();
        this.description = gene.getOfficialName();
        this.memberIds.add( gene.getId() );
        this.id = gene.getId();
    }

    /**
     * this method does not set the publik variable for the returned object (cannot autowire security service from here)
     *
     * @param searchResult search result
     */
    private void setValues( SearchResult<?> searchResult ) {

        // if it's a search result, grab the underlying object
        Class<?> searchResultClass = searchResult.getResultType();
        Object resultObject = searchResult.getResultObject();
        // class-specific construction
        if ( resultObject instanceof GeneValueObject ) {
            GeneValueObject gene = ( GeneValueObject ) resultObject;
            this.setValues( gene );
        } else if ( resultObject instanceof Gene ) {
            this.setValues( ( Gene ) resultObject );
        } else if ( resultObject instanceof GeneSetValueObject ) {
            this.setValues( ( GeneSetValueObject ) resultObject );
        } else if ( resultObject instanceof ExpressionExperimentValueObject ) {
            this.setValues( ( ExpressionExperimentValueObject ) resultObject );
        } else if ( resultObject instanceof ExpressionExperimentSetValueObject ) {
            this.setValues( ( ExpressionExperimentSetValueObject ) resultObject );
        } else {
            this.isGroup = false;
            this.size = -1;
            this.taxonId = ( long ) -1;
            this.taxonName = "unknown";
            this.name = "Unhandled type";
            this.description = "Unhandled result type: " + searchResultClass;
            this.memberIds = null;
        }
    }

}

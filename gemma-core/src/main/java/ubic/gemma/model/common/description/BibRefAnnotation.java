/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.common.description;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;

public abstract class BibRefAnnotation implements java.io.Serializable {

    private static final long serialVersionUID = -2925637697790915030L;
    private Long id;
    private Boolean isMajorTopic;

    private String term;

    @DocumentId
    public Long getId() {
        return this.id;
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public void setId( Long id ) {
        this.id = id;
    }

    public Boolean getIsMajorTopic() {
        return this.isMajorTopic;
    }

    public void setIsMajorTopic( Boolean isMajorTopic ) {
        this.isMajorTopic = isMajorTopic;
    }

    @Field
    public String getTerm() {
        return this.term;
    }

    public void setTerm( String term ) {
        this.term = term;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    /**
     * Returns <code>true</code> if the argument is an BibRefAnnotation instance and all identifiers for this entity
     * equal the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof BibRefAnnotation ) ) {
            return false;
        }
        final BibRefAnnotation that = ( BibRefAnnotation ) object;
        return this.id != null && that.getId() != null && this.id.equals( that.getId() );
    }

}
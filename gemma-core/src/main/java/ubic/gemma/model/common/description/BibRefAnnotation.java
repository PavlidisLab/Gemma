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

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import ubic.gemma.model.common.AbstractIdentifiable;

import java.util.Objects;

/**
 * Base class for {@link Keyword} and {@link MedicalSubjectHeading}; the {@code @Indexed}
 * annotation lives on the concrete subclasses. The {@code term} field is the only Lucene-indexed
 * property in the hierarchy.
 */
@Entity
@Table(name = "BIB_REF_ANNOTATION")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "class")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public abstract class BibRefAnnotation extends AbstractIdentifiable {

    @Column(name = "IS_MAJOR_TOPIC", columnDefinition = "TINYINT")
    private Boolean isMajorTopic;

    @Column(name = "TERM", nullable = false, columnDefinition = "VARCHAR(255)")
    private String term;

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
    }

    public Boolean getIsMajorTopic() {
        return this.isMajorTopic;
    }

    public void setIsMajorTopic( Boolean isMajorTopic ) {
        this.isMajorTopic = isMajorTopic;
    }

    @FullTextField
    public String getTerm() {
        return this.term;
    }

    public void setTerm( String term ) {
        this.term = term;
    }

    @Override
    public int hashCode() {
        return Objects.hash( term );
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
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        } else {
            return Objects.equals( term, that.term );
        }
    }
}

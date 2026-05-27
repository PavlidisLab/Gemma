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

package ubic.gemma.model.analysis;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import ubic.gemma.model.common.Identifiable;

/**
 * An abstract class representing a related set of generic analysis results, part of an analysis.
 * <p>
 * Hibernate 6 visibility regression fix (mirrors AuditEvent fix ab8b4c443c): no L2 entity cache
 * on this hierarchy because the same shape (mutable parent + child collections + read-only L2
 * cache, two levels deep through ExpressionAnalysisResultSet.results ->
 * DifferentialExpressionAnalysisResult.contrasts) served stale empty-bag results to fresh-session
 * reads after cross-tx writes. Rows are still write-once-immutable (@Immutable retained); we just
 * no longer cache them in L2.
 *
 * @author Paul
 */
@Entity
@Table(name = "ANALYSIS_RESULT_SET",
        indexes = {
                @Index(name = "ANALYSIS_RESULT_SET_NUMBER_OF_PROBES_TESTED", columnList = "NUMBER_OF_PROBES_TESTED"),
                @Index(name = "ANALYSIS_RESULT_SET_NUMBER_OF_GENES_TESTED", columnList = "NUMBER_OF_GENES_TESTED")
        })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "class", discriminatorType = DiscriminatorType.STRING)
@Immutable
@Access(AccessType.FIELD)
public abstract class AnalysisResultSet<R extends AnalysisResult> implements Identifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", columnDefinition = "BIGINT")
    private Long id = null;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public AnalysisResultSet() {
    }

    /**
     * Returns <code>true</code> if the argument is an AnalysisResultSet instance and all identifiers for this entity
     * equal the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof AnalysisResultSet ) ) {
            return false;
        }
        final AnalysisResultSet<?> that = ( AnalysisResultSet<?> ) object;
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        }
        return false;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }
}

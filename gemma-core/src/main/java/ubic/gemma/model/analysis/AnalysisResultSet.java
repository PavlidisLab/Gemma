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

import ubic.gemma.model.common.Identifiable;

import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Collection;

/**
 * An abstract class representing a related set of generic analysis results, part of an analysis.
 */
public abstract class AnalysisResultSet<R extends AnalysisResult> implements Identifiable, Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -6749501453616281312L;

    private Long id = null;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public AnalysisResultSet() {
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
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
        final AnalysisResultSet that = ( AnalysisResultSet ) object;
        return !( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) );
    }

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    @Transient
    public abstract Collection<R> getResults();

}
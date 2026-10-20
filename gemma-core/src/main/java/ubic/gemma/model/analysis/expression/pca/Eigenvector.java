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
package ubic.gemma.model.analysis.expression.pca;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.persistence.hibernate.ByteArrayType;

import java.util.Arrays;
import java.util.Objects;

/**
 * A right singular vector (a.k.a. eigengenes)
 */
@Entity
@Table(name = "EIGENVECTOR")
@Immutable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class Eigenvector extends AbstractIdentifiable {

    @Column(name = "COMPONENT_NUMBER", nullable = false, columnDefinition = "INTEGER")
    private Integer componentNumber;
    @Type(value = ByteArrayType.class, parameters = @Parameter(name = "arrayType", value = "double"))
    @Column(name = "VECTOR", nullable = false, columnDefinition = "LONGBLOB")
    private double[] vector;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public Eigenvector() {
    }

    public Integer getComponentNumber() {
        return this.componentNumber;
    }

    public void setComponentNumber( Integer componentNumber ) {
        this.componentNumber = componentNumber;
    }

    /**
     * @return Binary representing array of doubles
     */
    public double[] getVector() {
        return this.vector;
    }

    public void setVector( double[] vector ) {
        this.vector = vector;
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        return Objects.hash( componentNumber );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Eigenvector ) ) {
            return false;
        }
        final Eigenvector that = ( Eigenvector ) object;
        if ( getId() != null && that.getId() != null ) {
            return Objects.equals( getId(), that.getId() );
        } else {
            return Objects.equals( componentNumber, that.componentNumber )
                    && Arrays.equals( vector, that.vector );
        }
    }

    public static final class Factory {

        @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
        public static Eigenvector newInstance() {
            return new Eigenvector();
        }

        public static Eigenvector newInstance( Integer componentNumber, double[] vector ) {
            final Eigenvector entity = new Eigenvector();
            entity.setComponentNumber( componentNumber );
            entity.setVector( vector );
            return entity;
        }
    }

}
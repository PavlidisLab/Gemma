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
package ubic.gemma.model.association;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.genome.Gene;

import java.util.Objects;

/**
 * Entity representing a relationship between two genes. The designation of "first" and "second" gene is by default
 * completely arbitrary, there is no direction to the association defined by this. However, a direction can be imposed
 * by the implementing subclass.
 * Ideally subclasses are immutable, but this is not always possible. It can be done when the data is never updated but
 * just loaded in anew.
 *
 * @author paul
 */
public abstract class Gene2GeneAssociation implements Identifiable {

    final private Long id = null;
    final private Gene firstGene = null;
    final private Gene secondGene = null;

    @SuppressWarnings("ConstantConditions") // Hibernate populates fields via reflection.
    @Override
    public int hashCode() {
        return Objects.hash( firstGene, secondGene );
    }

    @SuppressWarnings("ConstantConditions") // Hibernate populates fields via reflection.
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( !( obj instanceof Gene2GeneAssociation ) )
            return false;
        Gene2GeneAssociation other = ( Gene2GeneAssociation ) obj;
        if ( getId() != null && other.getId() != null )
            return getId().equals( other.getId() );
        return Objects.equals( getFirstGene(), other.getFirstGene() )
                && Objects.equals( getSecondGene(), other.getSecondGene() );
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " [id=" + id + ", firstGene=" + firstGene + ", secondGene="
                + secondGene + "]";
    }

    public Gene getFirstGene() {
        return this.firstGene;
    }

    @Override
    public Long getId() {
        return id;
    }

    public Gene getSecondGene() {
        return this.secondGene;
    }

}
/*
 * The gemma project
 *
 * Copyright (c) 2014 University of British Columbia
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

/**
 * Entity representing a relationship between two genes identified by ID, rather than by the Gene entity (for efficiency
 * reasons). The designation of "first" and "second" Long is by default completely arbitrary, there is no direction to
 * the association defined by this. However, a direction can be imposed by the implementing subclass.
 * Ideally subclasses are immutable, but this is not always possible. It can be done when the data is never updated but
 * just loaded in anew.
 *
 * @author paul
 */
public abstract class Gene2GeneIdAssociation {

    final private Long id = null;
    final private Long firstGene = null;
    final private Long secondGene = null;

    @SuppressWarnings("ConstantConditions") // Hibernate populates fields via reflection.
    @Override
    public int hashCode() {
        if ( this.id != null )
            return this.id.hashCode();

        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( firstGene == null ) ? 0 : firstGene.hashCode() );
        result = prime * result + ( ( secondGene == null ) ? 0 : secondGene.hashCode() );
        return result;
    }

    @SuppressWarnings("ConstantConditions") // Hibernate populates fields via reflection.
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;

        Gene2GeneIdAssociation other = ( Gene2GeneIdAssociation ) obj;

        if ( this.id != null )
            return this.id.equals( other.getId() );

        if ( firstGene == null ) {
            if ( other.firstGene != null )
                return false;
        } else if ( !firstGene.equals( other.firstGene ) )
            return false;

        if ( secondGene == null ) {
            return other.secondGene == null;
        }
        return secondGene.equals( other.secondGene );
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " [id=" + id + ", firstGene=" + firstGene + ", secondGene="
                + secondGene + "]";
    }

    public Long getFirstGene() {
        return this.firstGene;
    }

    public Long getId() {
        return id;
    }

    public Long getSecondGene() {
        return this.secondGene;
    }
}

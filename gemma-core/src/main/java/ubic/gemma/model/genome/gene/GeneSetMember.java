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
package ubic.gemma.model.genome.gene;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.genome.Gene;

import java.io.Serializable;
import java.util.Objects;

@Indexed
public class GeneSetMember implements Identifiable, Serializable {

    @Override
    public String toString() {
        return "GeneSetMember [id=" + id + ", gene=" + gene + ", score=" + score + "]";
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -926690781193097196L;
    private Double score;
    private Long id;
    private Gene gene;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public GeneSetMember() {
    }

    @IndexedEmbedded
    public Gene getGene() {
        return this.gene;
    }

    public void setGene( Gene gene ) {
        this.gene = gene;
    }

    @Override
    @DocumentId
    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    /**
     * @return A generic value that can be used to provide additional information about group membership, to provide rankings of
     * group members for example.
     */
    public Double getScore() {
        return this.score;
    }

    public void setScore( Double score ) {
        this.score = score;
    }

    @Override
    public int hashCode() {
        // we cannot simply hash gene because it is lazily loaded, the id is fine to use though
        return getGene().getId().hashCode();
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof GeneSetMember ) ) {
            return false;
        }
        final GeneSetMember that = ( GeneSetMember ) object;
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        }
        if ( getGene() != null && that.getGene() != null ) {
            // compare gene by ID because they are lazy-loaded
            return Objects.equals( getGene().getId(), that.getGene().getId() );
        }
        return false;
    }

    public static final class Factory {

        public static GeneSetMember newInstance() {
            return new GeneSetMember();
        }

        @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
        public static GeneSetMember newInstance( Double score, ubic.gemma.model.genome.Gene gene ) {
            final GeneSetMember entity = new GeneSetMember();
            entity.setScore( score );
            entity.setGene( gene );
            return entity;
        }
    }

}
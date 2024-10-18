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

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import ubic.gemma.model.common.Identifiable;

import java.io.Serializable;
import java.util.Objects;

@Indexed
public class GeneAlias implements Identifiable, Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -4156628260205167700L;
    private String alias;
    private Long id;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public GeneAlias() {
    }

    @Override
    public int hashCode() {
        return Objects.hash( alias );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof GeneAlias ) ) {
            return false;
        }
        final GeneAlias that = ( GeneAlias ) object;
        return this.id != null && that.getId() != null && this.id.equals( that.getId() );
    }

    @Field(analyze = Analyze.NO)
    public String getAlias() {
        return this.alias;
    }

    public void setAlias( String alias ) {
        this.alias = alias;
    }

    @Override
    @DocumentId
    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public static final class Factory {
        public static ubic.gemma.model.genome.gene.GeneAlias newInstance() {
            return new ubic.gemma.model.genome.gene.GeneAlias();
        }

        @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
        public static ubic.gemma.model.genome.gene.GeneAlias newInstance( String alias ) {
            final ubic.gemma.model.genome.gene.GeneAlias entity = new ubic.gemma.model.genome.gene.GeneAlias();
            entity.setAlias( alias );
            return entity;
        }
    }

}
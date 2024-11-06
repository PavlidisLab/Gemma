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
import ubic.gemma.model.common.AbstractIdentifiable;

import java.util.Objects;

@Indexed
public class GeneAlias extends AbstractIdentifiable {

    private String alias;

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
        return super.getId();
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
        if ( this.getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        } else {
            return Objects.equals( alias, that.alias );
        }
    }

    public static final class Factory {

        public static GeneAlias newInstance() {
            return new GeneAlias();
        }

        public static GeneAlias newInstance( String alias ) {
            final GeneAlias entity = new GeneAlias();
            entity.setAlias( alias );
            return entity;
        }
    }
}
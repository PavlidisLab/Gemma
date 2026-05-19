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
package ubic.gemma.model.expression.arrayDesign;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import ubic.gemma.model.common.AbstractIdentifiable;

import java.util.Objects;

/**
 * Hibernate Search 7 mapping: contributes its {@link #getName()} as a tokenized field to
 * {@link ArrayDesign}'s document via {@code @IndexedEmbedded}. Carries its own {@code @Indexed}
 * for symmetry with the pre-strip HS 5 mapping, even though the only Lucene path that consumes
 * AlternateName documents today is the {@code ArrayDesign.alternateNames} embedded path.
 */
@Indexed
public class AlternateName extends AbstractIdentifiable {

    private String name;

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
    }

    @FullTextField
    public String getName() {
        return this.name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash( name );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof AlternateName ) ) {
            return false;
        }
        final AlternateName that = ( AlternateName ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        } else {
            return Objects.equals( getName(), that.getName() );
        }
    }

    public static final class Factory {

        @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
        public static ubic.gemma.model.expression.arrayDesign.AlternateName newInstance() {
            return new ubic.gemma.model.expression.arrayDesign.AlternateName();
        }

        public static ubic.gemma.model.expression.arrayDesign.AlternateName newInstance( String name ) {
            final ubic.gemma.model.expression.arrayDesign.AlternateName entity = new ubic.gemma.model.expression.arrayDesign.AlternateName();
            entity.setName( name );
            return entity;
        }
    }

}
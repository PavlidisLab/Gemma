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
package ubic.gemma.model.expression.biomaterial;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import ubic.gemma.model.common.AbstractDescribable;

/**
 * Hibernate Search 7 mapping: chemicals attached to {@link ubic.gemma.model.common.description.BibliographicReference}
 * via {@code @IndexedEmbedded}; {@code name} is tokenized, {@code registryNumber} is keyword (CAS).
 */
@Indexed
public class Compound extends AbstractDescribable {

    private String registryNumber;

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
    }

    @Override
    @FullTextField
    public String getName() {
        return super.getName();
    }

    /**
     * @return CAS registry number (see http://www.cas.org/)
     */
    @KeywordField
    public String getRegistryNumber() {
        return this.registryNumber;
    }

    public void setRegistryNumber( String registryNumber ) {
        this.registryNumber = registryNumber;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof Compound ) )
            return false;
        Compound that = ( Compound ) object;
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        } else {
            return false;
        }
    }

    public static final class Factory {

        public static Compound newInstance() {
            return new Compound();
        }

        public static Compound newInstance( String name, String description, String registryNumber ) {
            final Compound entity = new Compound();
            entity.setName( name );
            entity.setDescription( description );
            entity.setRegistryNumber( registryNumber );
            return entity;
        }
    }

}
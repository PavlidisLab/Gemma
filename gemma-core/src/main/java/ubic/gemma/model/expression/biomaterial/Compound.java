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

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import ubic.gemma.model.common.AbstractDescribable;

import java.io.Serializable;

@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
@Indexed
public class Compound extends AbstractDescribable implements Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -6782144197298874202L;
    private String registryNumber;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public Compound() {
    }

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
    }

    @Override
    @Field
    public String getName() {
        return super.getName();
    }

    /**
     * @return CAS registry number (see http://www.cas.org/)
     */
    @Field(analyze = Analyze.NO)
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
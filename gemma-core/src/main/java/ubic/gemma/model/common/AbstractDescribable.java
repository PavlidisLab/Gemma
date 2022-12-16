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
package ubic.gemma.model.common;

public abstract class AbstractDescribable implements Describable {

    private String name;
    private String description;
    private Long id;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    @SuppressWarnings("WeakerAccess") // Required by spring
    public AbstractDescribable() {
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
     * Returns <code>true</code> if the argument is an Describable instance and all identifiers for this entity equal
     * the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof AbstractDescribable ) ) {
            return false;
        }
        final Describable that = ( Describable ) object;
        return !( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) );
    }

    /**
     * @see AbstractDescribable#toString()
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ( this.getId() == null ? "" : " Id=" + this.getId() ) + (
                this.getName() == null ? "" : " Name=" + this.getName() );
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName( String name ) {
        this.name = name;
    }

}
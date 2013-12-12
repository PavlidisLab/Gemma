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
package ubic.gemma.model.common.auditAndSecurity;

/**
 * Represents the basic status of an Auditable, with possible information about state in workflows etc.
 */
public abstract class Status implements java.io.Serializable {

    /**
     * Constructs new instances of {@link Status}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link Status}.
         */
        public static Status newInstance() {
            return new StatusImpl();
        }

        /**
         * Constructs a new instance of {@link Status}, taking all required and/or read-only properties as arguments.
         */
        public static Status newInstance( java.util.Date createDate, Boolean troubled, Boolean validated ) {
            final Status entity = new StatusImpl();
            entity.setCreateDate( createDate );
            entity.setTroubled( troubled );
            entity.setValidated( validated );
            return entity;
        }

        /**
         * Constructs a new instance of {@link Status}, taking all possible properties (except the identifier(s))as
         * arguments.
         */
        public static Status newInstance( java.util.Date createDate, java.util.Date lastUpdateDate, Boolean troubled,
                Boolean validated ) {
            final Status entity = new StatusImpl();
            entity.setCreateDate( createDate );
            entity.setLastUpdateDate( lastUpdateDate );
            entity.setTroubled( troubled );
            entity.setValidated( validated );
            return entity;
        }
    }

    private java.util.Date createDate;

    private java.util.Date lastUpdateDate;

    private Boolean troubled = Boolean.valueOf( false );

    private Boolean validated = Boolean.valueOf( false );

    private Long id;

    /**
     * Returns <code>true</code> if the argument is an Status instance and all identifiers for this entity equal the
     * identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Status ) ) {
            return false;
        }
        final Status that = ( Status ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * 
     */
    public java.util.Date getCreateDate() {
        return this.createDate;
    }

    /**
     * 
     */
    public Long getId() {
        return this.id;
    }

    /**
     * 
     */
    public java.util.Date getLastUpdateDate() {
        return this.lastUpdateDate;
    }

    /**
     * 
     */
    public Boolean getTroubled() {
        return this.troubled;
    }

    /**
     * 
     */
    public Boolean getValidated() {
        return this.validated;
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

    public void setCreateDate( java.util.Date createDate ) {
        this.createDate = createDate;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setLastUpdateDate( java.util.Date lastUpdateDate ) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public void setTroubled( Boolean troubled ) {
        this.troubled = troubled;
    }

    public void setValidated( Boolean validated ) {
        this.validated = validated;
    }

}
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

import ubic.gemma.model.common.search.SearchSettings;

import java.io.Serializable;

public class UserQuery implements Serializable, SecuredNotChild {

    private static final long serialVersionUID = -2790140985613402440L;
    private String url;
    private String name;
    private java.util.Date lastUsed;
    private Integer autoRunFrequencyHours;
    private Long id;
    private SearchSettings searchSettings;

    /**
     * @return How often to auto-run this query, given in hours.
     */
    public Integer getAutoRunFrequencyHours() {
        return this.autoRunFrequencyHours;
    }

    public void setAutoRunFrequencyHours( Integer autoRunFrequencyHours ) {
        this.autoRunFrequencyHours = autoRunFrequencyHours;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public java.util.Date getLastUsed() {
        return this.lastUsed;
    }

    public void setLastUsed( java.util.Date lastUsed ) {
        this.lastUsed = lastUsed;
    }

    public String getName() {
        return this.name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public SearchSettings getSearchSettings() {
        return this.searchSettings;
    }

    public void setSearchSettings( SearchSettings searchSettings ) {
        this.searchSettings = searchSettings;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl( String url ) {
        this.url = url;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof UserQuery ) ) {
            return false;
        }
        final UserQuery that = ( UserQuery ) object;
        return !( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) );
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static final class Factory {

        public static UserQuery newInstance() {
            return new UserQuery();
        }

        public static UserQuery newInstance( String url, java.util.Date lastUsed ) {
            final UserQuery entity = new UserQuery();
            entity.setUrl( url );
            entity.setLastUsed( lastUsed );
            return entity;
        }

        public static UserQuery newInstance( String url, String name, java.util.Date lastUsed,
                Integer autoRunFrequencyHours, SearchSettings searchSettings ) {
            final UserQuery entity = new UserQuery();
            entity.setUrl( url );
            entity.setName( name );
            entity.setLastUsed( lastUsed );
            entity.setAutoRunFrequencyHours( autoRunFrequencyHours );
            entity.setSearchSettings( searchSettings );
            return entity;
        }
    }

}
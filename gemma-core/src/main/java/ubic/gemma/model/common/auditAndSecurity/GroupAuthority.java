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

import ubic.gemma.model.common.AbstractIdentifiable;

import java.util.Objects;

/**
 * Authority for groups (kind of like a "user role", but for group-based authorization)
 */
public class GroupAuthority extends AbstractIdentifiable implements gemma.gsec.model.GroupAuthority {

    private String authority;

    @Override
    public String getAuthority() {
        return this.authority;
    }

    public void setAuthority( String authority ) {
        this.authority = authority;
    }

    @Override
    public int hashCode() {
        return Objects.hash( authority );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof GroupAuthority ) ) {
            return false;
        }
        final GroupAuthority that = ( GroupAuthority ) object;
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        } else {
            return Objects.equals( authority, that.authority );
        }
    }

    public static final class Factory {

        public static GroupAuthority newInstance() {
            return new GroupAuthority();
        }

        public static GroupAuthority newInstance( String authority ) {
            final GroupAuthority entity = new GroupAuthority();
            entity.setAuthority( authority );
            return entity;
        }
    }

}
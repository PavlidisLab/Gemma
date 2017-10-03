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

package ubic.gemma.model.common.description;

import ubic.gemma.model.common.AbstractAuditable;

import java.util.Collection;

/**
 * @author Paul
 */
public class ExternalDatabase extends AbstractAuditable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 6807023718405086508L;
    private String localInstallDbName;
    private String webUri;
    private String ftpUri;
    private ubic.gemma.model.common.description.DatabaseType type;
    private ubic.gemma.model.common.auditAndSecurity.Contact databaseSupplier;
    private Collection<ubic.gemma.model.common.description.LocalFile> flatFiles = new java.util.HashSet<>();

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public ExternalDatabase() {
    }

    @Override
    public boolean equals( Object object ) {
        if ( !( object instanceof ExternalDatabase ) )
            return false;

        ExternalDatabase that = ( ExternalDatabase ) object;
        if ( this.getId() != null && that.getId() != null )
            return super.equals( object );

        return this.getName().equals( that.getName() );
    }

    @Override
    public int hashCode() {
        if ( this.getId() != null )
            return super.hashCode();

        return this.getName().hashCode();
    }

    public ubic.gemma.model.common.auditAndSecurity.Contact getDatabaseSupplier() {
        return this.databaseSupplier;
    }

    public void setDatabaseSupplier( ubic.gemma.model.common.auditAndSecurity.Contact databaseSupplier ) {
        this.databaseSupplier = databaseSupplier;
    }

    public Collection<ubic.gemma.model.common.description.LocalFile> getFlatFiles() {
        return this.flatFiles;
    }

    public void setFlatFiles( Collection<ubic.gemma.model.common.description.LocalFile> flatFiles ) {
        this.flatFiles = flatFiles;
    }

    public String getFtpUri() {
        return this.ftpUri;
    }

    public void setFtpUri( String ftpUri ) {
        this.ftpUri = ftpUri;
    }

    /**
     * @return The name of the database on a local server.
     */
    public String getLocalInstallDbName() {
        return this.localInstallDbName;
    }

    public void setLocalInstallDbName( String localInstallDbName ) {
        this.localInstallDbName = localInstallDbName;
    }

    public ubic.gemma.model.common.description.DatabaseType getType() {
        return this.type;
    }

    public void setType( ubic.gemma.model.common.description.DatabaseType type ) {
        this.type = type;
    }

    public String getWebUri() {
        return this.webUri;
    }

    public void setWebUri( String webUri ) {
        this.webUri = webUri;
    }

    public static final class Factory {

        public static ubic.gemma.model.common.description.ExternalDatabase newInstance() {
            return new ubic.gemma.model.common.description.ExternalDatabase();
        }

    }

}
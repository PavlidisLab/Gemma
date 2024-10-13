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

import ubic.gemma.model.common.AbstractDescribable;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.Contact;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Paul
 */
public class ExternalDatabase extends AbstractDescribable implements Auditable, Versioned, Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 6807023718405086508L;
    private String localInstallDbName;
    private String webUri;
    private String ftpUri;
    private DatabaseType type;
    private Contact databaseSupplier;
    /**
     * Related external databases.
     */
    private Set<ExternalDatabase> externalDatabases = new HashSet<>();
    private AuditTrail auditTrail = new AuditTrail();
    @Nullable
    private String releaseVersion;
    @Nullable
    private URL releaseUrl;
    @Nullable
    private Date lastUpdated;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public ExternalDatabase() {
    }

    @Override
    public boolean equals( Object object ) {
        if ( !( object instanceof ExternalDatabase ) ) return false;

        ExternalDatabase that = ( ExternalDatabase ) object;
        if ( this.getId() != null && that.getId() != null ) return super.equals( object );

        return this.getName().equals( that.getName() );
    }

    @Override
    public int hashCode() {
        if ( this.getId() != null ) return super.hashCode();

        return this.getName().hashCode();
    }

    public Contact getDatabaseSupplier() {
        return this.databaseSupplier;
    }

    public void setDatabaseSupplier( Contact databaseSupplier ) {
        this.databaseSupplier = databaseSupplier;
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

    public DatabaseType getType() {
        return this.type;
    }

    public void setType( DatabaseType type ) {
        this.type = type;
    }

    public String getWebUri() {
        return this.webUri;
    }

    public void setWebUri( String webUri ) {
        this.webUri = webUri;
    }

    public Set<ExternalDatabase> getExternalDatabases() {
        return this.externalDatabases;
    }

    public void setExternalDatabases( Set<ExternalDatabase> externalDatabases ) {
        this.externalDatabases = externalDatabases;
    }

    @Override
    public AuditTrail getAuditTrail() {
        return this.auditTrail;
    }

    @Override
    public void setAuditTrail( AuditTrail auditTrail ) {
        this.auditTrail = auditTrail;
    }

    @Nullable
    @Override
    public String getReleaseVersion() {
        return releaseVersion;
    }

    public void setReleaseVersion( @Nullable String releaseVersion ) {
        this.releaseVersion = releaseVersion;
    }

    @Nullable
    @Override
    public URL getReleaseUrl() {
        return releaseUrl;
    }

    public void setReleaseUrl( @Nullable URL releaseUrl ) {
        this.releaseUrl = releaseUrl;
    }

    @Nullable
    @Override
    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated( @Nullable Date lastUpdated ) {
        this.lastUpdated = lastUpdated;
    }

    public static final class Factory {

        public static ExternalDatabase newInstance() {
            return new ExternalDatabase();
        }

        public static ExternalDatabase newInstance( String name, DatabaseType other ) {
            ExternalDatabase ed = new ExternalDatabase();
            ed.setName( name );
            ed.setType( other );
            return ed;
        }
    }

}
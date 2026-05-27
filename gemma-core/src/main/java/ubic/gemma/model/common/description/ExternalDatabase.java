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

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.lang.Nullable;
import ubic.gemma.model.common.AbstractDescribable;
import ubic.gemma.model.common.DescribableUtils;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.Contact;

import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Paul
 */
@Entity
@Table(name = "EXTERNAL_DATABASE")
@AttributeOverride(name = "name", column = @Column(name = "NAME", nullable = false, unique = true, columnDefinition = "VARCHAR(255)"))
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class ExternalDatabase extends AbstractDescribable implements Auditable, Versioned {

    @Column(name = "LOCAL_INSTALL_DB_NAME", columnDefinition = "VARCHAR(255)")
    private String localInstallDbName;
    @Column(name = "WEB_URI", columnDefinition = "VARCHAR(255)")
    private String webUri;
    @Column(name = "FTP_URI", columnDefinition = "VARCHAR(255)")
    private String ftpUri;
    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", columnDefinition = "VARCHAR(255)")
    private DatabaseType type;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DATABASE_SUPPLIER_FK", columnDefinition = "BIGINT")
    private Contact databaseSupplier;
    /**
     * Related external databases.
     */
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "EXTERNAL_DATABASE_FK", columnDefinition = "BIGINT", foreignKey = @ForeignKey(name = "EXTERNAL_DATABASE_FKC"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<ExternalDatabase> externalDatabases = new HashSet<>();
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "AUDIT_TRAIL_FK", nullable = false, unique = true, columnDefinition = "BIGINT")
    private AuditTrail auditTrail = new AuditTrail();
    @Nullable
    @Column(name = "RELEASE_VERSION", columnDefinition = "VARCHAR(255)")
    private String releaseVersion;
    @Nullable
    @Column(name = "RELEASE_URL", columnDefinition = "VARCHAR(255)")
    private URL releaseUrl;
    @Nullable
    @Column(name = "LAST_UPDATED", columnDefinition = "DATETIME(3)")
    private Date lastUpdated;

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

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof ExternalDatabase ) )
            return false;
        ExternalDatabase that = ( ExternalDatabase ) object;
        if ( this.getId() != null && that.getId() != null )
            return getId().equals( that.getId() );
        return DescribableUtils.equalsByName( this, that );
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
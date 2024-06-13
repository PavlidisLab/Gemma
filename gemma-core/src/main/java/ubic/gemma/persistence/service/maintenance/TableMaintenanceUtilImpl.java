/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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

package ubic.gemma.persistence.service.maintenance;

import io.micrometer.core.annotation.Timed;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.core.util.MailEngine;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignGeneMappingEvent;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.genome.GeneDao;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

/**
 * Functions for maintaining the database. This is intended for denormalized tables and statistics tables that need to
 * be generated periodically.
 *
 * @author jsantos
 * @author paul
 */
@Service("tableMaintenanceUtil")
public class TableMaintenanceUtilImpl implements TableMaintenanceUtil {

    /**
     * The query used to repopulate the contents of the GENE2CS table.
     */
    private static final String GENE2CS_REPOPULATE_QUERY =
            "REPLACE INTO GENE2CS (GENE, CS, AD) " + "SELECT DISTINCT gene.ID, cs.ID, cs.ARRAY_DESIGN_FK "
                    + " FROM CHROMOSOME_FEATURE AS gene, CHROMOSOME_FEATURE AS geneprod,BIO_SEQUENCE2_GENE_PRODUCT AS bsgp,COMPOSITE_SEQUENCE cs "
                    + " WHERE geneprod.GENE_FK = gene.ID AND bsgp.GENE_PRODUCT_FK = geneprod.ID AND "
                    + " bsgp.BIO_SEQUENCE_FK = cs.BIOLOGICAL_CHARACTERISTIC_FK ORDER BY gene.ID,cs.ARRAY_DESIGN_FK";

    /**
     * Select the bitmask of permissions that applies to the {@code IS_AUTHENTICATED_ANONYMOUSLY} granted authority. If
     * more than one ACL entry are present, they are combined with a bitwise OR.
     * <p>
     * If no ACL entries exist for the anonymous SID, 0 is returned which effectively grants no permission at all.
     */
    private static final String SELECT_ANONYMOUS_MASK =
            "coalesce((select BIT_OR(ACE.MASK) "
                    + "from ACLOBJECTIDENTITY AOI "
                    + "join ACLENTRY ACE on ACE.OBJECTIDENTITY_FK = AOI.ID "
                    + "where AOI.OBJECT_CLASS = 'ubic.gemma.model.expression.experiment.ExpressionExperiment' "
                    + "and AOI.OBJECT_ID = I.ID "
                    + "and ACE.SID_FK = (select ACLSID.ID from ACLSID where ACLSID.GRANTED_AUTHORITY = 'IS_AUTHENTICATED_ANONYMOUSLY') "
                    + "group by AOI.ID), 0)";

    /**
     * Clause for selecting entities updated since a given date.
     */
    private static final String CD_LAST_UPDATED_SINCE = "(CD.LAST_UPDATED is null or :since is null or CD.LAST_UPDATED >= :since)";

    private static final String EE2C_EE_QUERY =
            "select MIN(C.ID), C.NAME, C.DESCRIPTION, C.CATEGORY, C.CATEGORY_URI, C.`VALUE`, C.VALUE_URI, C.ORIGINAL_VALUE, C.EVIDENCE_CODE, I.ID, (" + SELECT_ANONYMOUS_MASK + "), cast(:eeClass as char(255)) "
                    + "from INVESTIGATION I "
                    + "join CURATION_DETAILS CD on I.CURATION_DETAILS_FK = CD.ID "
                    + "join CHARACTERISTIC C on I.ID = C.INVESTIGATION_FK "
                    + "where I.class = 'ExpressionExperiment' "
                    + "and " + CD_LAST_UPDATED_SINCE + " "
                    + "group by I.ID, COALESCE(C.CATEGORY_URI, C.CATEGORY), COALESCE(C.VALUE_URI, C.`VALUE`)";

    private static final String EE2C_BM_QUERY =
            "select MIN(C.ID), C.NAME, C.DESCRIPTION, C.CATEGORY, C.CATEGORY_URI, C.`VALUE`, C.VALUE_URI, C.ORIGINAL_VALUE, C.EVIDENCE_CODE, I.ID, (" + SELECT_ANONYMOUS_MASK + "), cast(:bmClass as char(255)) "
                    + "from INVESTIGATION I "
                    + "join CURATION_DETAILS CD on I.CURATION_DETAILS_FK = CD.ID "
                    + "join BIO_ASSAY BA on I.ID = BA.EXPRESSION_EXPERIMENT_FK "
                    + "join BIO_MATERIAL BM on BA.SAMPLE_USED_FK = BM.ID "
                    + "join CHARACTERISTIC C on BM.ID = C.BIO_MATERIAL_FK "
                    + "where I.class = 'ExpressionExperiment' "
                    + "and " + CD_LAST_UPDATED_SINCE + " "
                    + "group by I.ID, COALESCE(C.CATEGORY_URI, C.CATEGORY), COALESCE(C.VALUE_URI, C.`VALUE`)";

    private static final String EE2C_ED_QUERY =
            "select MIN(C.ID), C.NAME, C.DESCRIPTION, C.CATEGORY, C.CATEGORY_URI, C.`VALUE`, C.VALUE_URI, C.ORIGINAL_VALUE, C.EVIDENCE_CODE, I.ID, (" + SELECT_ANONYMOUS_MASK + "), cast(:edClass as char(255)) "
                    + "from INVESTIGATION I "
                    + "join CURATION_DETAILS CD on I.CURATION_DETAILS_FK = CD.ID "
                    + "join EXPERIMENTAL_DESIGN on I.EXPERIMENTAL_DESIGN_FK = EXPERIMENTAL_DESIGN.ID "
                    + "join EXPERIMENTAL_FACTOR EF on EXPERIMENTAL_DESIGN.ID = EF.EXPERIMENTAL_DESIGN_FK "
                    + "join FACTOR_VALUE FV on FV.EXPERIMENTAL_FACTOR_FK = EF.ID "
                    + "join CHARACTERISTIC C on FV.ID = C.FACTOR_VALUE_FK "
                    + "where I.class = 'ExpressionExperiment' "
                    // remove C.class = 'Statement' once the old-style characteristics are removed (see https://github.com/PavlidisLab/Gemma/issues/929 for details)
                    + "and C.class = 'Statement' "
                    + "and " + CD_LAST_UPDATED_SINCE + " "
                    + "group by I.ID, COALESCE(C.CATEGORY_URI, C.CATEGORY), COALESCE(C.VALUE_URI, C.`VALUE`)";

    private static final String EE2AD_QUERY = "insert into EXPRESSION_EXPERIMENT2ARRAY_DESIGN (EXPRESSION_EXPERIMENT_FK, ARRAY_DESIGN_FK, IS_ORIGINAL_PLATFORM, ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK) "
            + "select I.ID, AD.ID, FALSE, (" + SELECT_ANONYMOUS_MASK + ") from INVESTIGATION I "
            + "join CURATION_DETAILS CD on I.CURATION_DETAILS_FK = CD.ID "
            + "join BIO_ASSAY BA on I.ID = BA.EXPRESSION_EXPERIMENT_FK "
            + "join ARRAY_DESIGN AD on BA.ARRAY_DESIGN_USED_FK = AD.ID "
            + "where I.class = 'ExpressionExperiment' "
            + "and COALESCE(CD.LAST_UPDATED, 0) >= COALESCE(:since, 0) "
            + "group by I.ID, AD.ID "
            + "union "
            + "select I.ID, AD.ID, TRUE, (" + SELECT_ANONYMOUS_MASK + ") from INVESTIGATION I "
            + "join CURATION_DETAILS CD on I.CURATION_DETAILS_FK = CD.ID "
            + "join BIO_ASSAY BA on I.ID = BA.EXPRESSION_EXPERIMENT_FK "
            + "join ARRAY_DESIGN AD on BA.ORIGINAL_PLATFORM_FK = AD.ID "
            + "where I.class = 'ExpressionExperiment' "
            + "and " + CD_LAST_UPDATED_SINCE + " "
            + "group by I.ID, AD.ID "
            + "on duplicate key update ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK = VALUES(ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK)";

    private static final Log log = LogFactory.getLog( TableMaintenanceUtil.class.getName() );

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private MailEngine mailEngine;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private SessionFactory sessionFactory;

    @Value("${gemma.gene2cs.path}")
    private Path gene2CsInfoPath;

    private boolean sendEmail = true;

    @Override
    @Transactional
    @Timed
    public void updateGene2CsEntries() {
        TableMaintenanceUtilImpl.log.debug( "Running Gene2CS status check" );

        String annotation = "";
        try {
            Gene2CsStatus status = this.getLastGene2CsUpdateStatus();
            boolean needToRefresh = false;
            if ( status == null ) {
                needToRefresh = true;
            }

            if ( !needToRefresh ) {
                Collection<Auditable> newObj = auditEventService.getNewSinceDate( status.getLastUpdate() );

                for ( Auditable a : newObj ) {
                    if ( a instanceof ArrayDesign ) {
                        needToRefresh = true;
                        annotation = a + " is new since " + status.getLastUpdate();
                        TableMaintenanceUtilImpl.log.debug( annotation );
                        break;
                    }
                }
            }

            if ( !needToRefresh ) {
                Collection<Auditable> updatedObj = auditEventService.getUpdatedSinceDate( status.getLastUpdate() );
                for ( Auditable a : updatedObj ) {
                    if ( a instanceof ArrayDesign ) {
                        for ( AuditEvent ae : auditEventService.getEvents( a ) ) {
                            if ( ae == null )
                                continue; // legacy of ordered-list which could end up with gaps; should
                            // not be needed any more
                            if ( ae.getEventType() != null && ae.getEventType() instanceof ArrayDesignGeneMappingEvent
                                    && ae.getDate().after( status.getLastUpdate() ) ) {
                                needToRefresh = true;
                                annotation = a + " had probe mapping done since: " + status.getLastUpdate();
                                TableMaintenanceUtilImpl.log.debug( annotation );
                                break;
                            }
                        }
                    }
                    if ( needToRefresh )
                        break;
                }
            }

            if ( needToRefresh ) {
                TableMaintenanceUtilImpl.log.debug( "Update of GENE2CS initiated" );
                this.generateGene2CsEntries();
                Gene2CsStatus updatedStatus = this.writeUpdateStatus( annotation, null );
                this.updateGene2csExternalDatabaseLastUpdated( updatedStatus );
                this.sendEmail( updatedStatus );

            } else {
                TableMaintenanceUtilImpl.log.debug( "No update of GENE2CS needed" );
            }

        } catch ( RuntimeException e ) {
            Gene2CsStatus updatedStatus = this.writeUpdateStatus( annotation, e );
            this.sendEmail( updatedStatus );
            throw e;
        }
    }

    @Override
    @Transactional
    @Timed
    public int updateExpressionExperiment2CharacteristicEntries( @Nullable Date sinceLastUpdate, boolean truncate ) {
        Assert.isTrue( !( sinceLastUpdate != null && truncate ), "Cannot perform a partial update with sinceLastUpdate with truncate." );
        StopWatch timer = StopWatch.createStarted();
        log.info( String.format( "Updating the EXPRESSION_EXPERIMENT2CHARACTERISTIC table%s...",
                sinceLastUpdate != null ? " since " + sinceLastUpdate : "" ) );
        if ( truncate ) {
            log.info( "Truncating EXPRESSION_EXPERIMENT2CHARACTERISTIC..." );
            sessionFactory.getCurrentSession()
                    .createSQLQuery( "delete from EXPRESSION_EXPERIMENT2CHARACTERISTIC" )
                    .executeUpdate();
        }
        int updated = sessionFactory.getCurrentSession()
                .createSQLQuery(
                        "insert into EXPRESSION_EXPERIMENT2CHARACTERISTIC (ID, NAME, DESCRIPTION, CATEGORY, CATEGORY_URI, `VALUE`, VALUE_URI, ORIGINAL_VALUE, EVIDENCE_CODE, EXPRESSION_EXPERIMENT_FK, ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK, LEVEL) "
                                + EE2C_EE_QUERY
                                + " union "
                                + EE2C_BM_QUERY
                                + " union "
                                + EE2C_ED_QUERY + " "
                                + "on duplicate key update NAME = VALUES(NAME), DESCRIPTION = VALUES(DESCRIPTION), CATEGORY = VALUES(CATEGORY), CATEGORY_URI = VALUES(CATEGORY_URI), `VALUE` = VALUES(`VALUE`), VALUE_URI = VALUES(VALUE_URI), ORIGINAL_VALUE = VALUES(ORIGINAL_VALUE), EVIDENCE_CODE = VALUES(EVIDENCE_CODE), ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK = VALUES(ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK), LEVEL = VALUES(LEVEL)" )
                .addSynchronizedQuerySpace( EE2C_QUERY_SPACE )
                .setParameter( "eeClass", ExpressionExperiment.class )
                .setParameter( "bmClass", BioMaterial.class )
                .setParameter( "edClass", ExperimentalDesign.class )
                .setParameter( "since", sinceLastUpdate )
                .executeUpdate();
        log.info( String.format( "Done updating the EXPRESSION_EXPERIMENT2CHARACTERISTIC table; %d entries were updated%s in %d ms.",
                updated,
                sinceLastUpdate != null ? " since " + sinceLastUpdate : "",
                timer.getTime() ) );
        return updated;
    }

    @Override
    @Timed
    @Transactional
    public int updateExpressionExperiment2CharacteristicEntries( Class<?> level, @Nullable Date sinceLastUpdate, boolean truncate ) {
        Assert.isTrue( !( sinceLastUpdate != null && truncate ), "Cannot perform a partial update with sinceLastUpdate with truncate." );
        String levelParamName;
        String query;
        if ( level.equals( ExpressionExperiment.class ) ) {
            levelParamName = "eeClass";
            query = EE2C_EE_QUERY;
        } else if ( level.equals( BioMaterial.class ) ) {
            levelParamName = "bmClass";
            query = EE2C_BM_QUERY;
        } else if ( level.equals( ExperimentalDesign.class ) ) {
            levelParamName = "edClass";
            query = EE2C_ED_QUERY;
        } else {
            throw new IllegalArgumentException( "Level must be one of ExpressionExperiment.class, BioMaterial.class or ExperimentalDesign.class." );
        }
        StopWatch timer = StopWatch.createStarted();
        log.info( String.format( "Updating the EXPRESSION_EXPERIMENT2CHARACTERISTIC table at %s level%s...",
                level.getSimpleName(),
                sinceLastUpdate != null ? " since " + sinceLastUpdate : "" ) );
        if ( truncate ) {
            log.info( "Truncating EXPRESSION_EXPERIMENT2CHARACTERISTIC at " + level.getSimpleName() + " level..." );
            sessionFactory.getCurrentSession()
                    .createSQLQuery( "delete from EXPRESSION_EXPERIMENT2CHARACTERISTIC where LEVEL = :level" )
                    .setParameter( "level", level )
                    .executeUpdate();
        }
        int updated = sessionFactory.getCurrentSession()
                .createSQLQuery(
                        "insert into EXPRESSION_EXPERIMENT2CHARACTERISTIC (ID, NAME, DESCRIPTION, CATEGORY, CATEGORY_URI, `VALUE`, VALUE_URI, ORIGINAL_VALUE, EVIDENCE_CODE, EXPRESSION_EXPERIMENT_FK, ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK, LEVEL) "
                                + query + " "
                                + "on duplicate key update NAME = VALUES(NAME), DESCRIPTION = VALUES(DESCRIPTION), CATEGORY = VALUES(CATEGORY), CATEGORY_URI = VALUES(CATEGORY_URI), `VALUE` = VALUES(`VALUE`), VALUE_URI = VALUES(VALUE_URI), ORIGINAL_VALUE = VALUES(ORIGINAL_VALUE), EVIDENCE_CODE = VALUES(EVIDENCE_CODE), ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK = VALUES(ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK), LEVEL = VALUES(LEVEL)" )
                .addSynchronizedQuerySpace( EE2C_QUERY_SPACE )
                .setParameter( levelParamName, level )
                .setParameter( "since", sinceLastUpdate )
                .executeUpdate();
        log.info( String.format( "Done updating the EXPRESSION_EXPERIMENT2CHARACTERISTIC table at %s level; %d entries were updated%s in %d ms.",
                level.getSimpleName(), updated,
                sinceLastUpdate != null ? " since " + sinceLastUpdate : "",
                timer.getTime() ) );
        return updated;
    }

    @Override
    @Transactional
    public int updateExpressionExperiment2ArrayDesignEntries( @Nullable Date sinceLastUpdate ) {
        StopWatch timer = StopWatch.createStarted();
        log.info( String.format( "Updating the EXPRESSION_EXPERIMENT2ARRAY_DESIGN table%s...",
                sinceLastUpdate != null ? " since " + sinceLastUpdate : "" ) );
        int updated = sessionFactory.getCurrentSession().createSQLQuery( EE2AD_QUERY )
                .addSynchronizedQuerySpace( EE2AD_QUERY_SPACE )
                .setParameter( "since", sinceLastUpdate )
                .executeUpdate();
        log.info( String.format( "Done updating the EXPRESSION_EXPERIMENT2ARRAY_DESIGN table; %d entries were updated%s in %d ms.",
                updated, sinceLastUpdate != null ? " since " + sinceLastUpdate : "", timer.getTime() ) );
        return updated;
    }

    /**
     * For use in tests.
     */
    @Override
    public void disableEmail() {
        this.sendEmail = false;
    }

    /**
     * Function to regenerate the GENE2CS entries. Gene2Cs is a denormalized join table that allows for a quick link
     * between Genes and CompositeSequences
     *
     * @see GeneDao for where the GENE2CS table is used extensively.
     */
    private void generateGene2CsEntries() {
        StopWatch timer = StopWatch.createStarted();
        TableMaintenanceUtilImpl.log.info( "Updating the GENE2CS table..." );
        int updated = this.sessionFactory.getCurrentSession()
                .createSQLQuery( TableMaintenanceUtilImpl.GENE2CS_REPOPULATE_QUERY )
                .addSynchronizedQuerySpace( GENE2CS_QUERY_SPACE )
                .executeUpdate();
        TableMaintenanceUtilImpl.log.info( String.format( "Done regenerating the GENE2CS table; %d entries were updated in %d ms.", updated, timer.getTime() ) );
    }

    /**
     * Reads previous run information from disk.
     *
     * @return null if there is no update information available.
     */
    @Nullable
    private Gene2CsStatus getLastGene2CsUpdateStatus() {
        try ( ObjectInputStream ois = new ObjectInputStream( Files.newInputStream( gene2CsInfoPath ) ) ) {
            return ( Gene2CsStatus ) ois.readObject();
        } catch ( NoSuchFileException e ) {
            return null;
        } catch ( IOException | ClassNotFoundException e ) {
            throw new RuntimeException( "Failed to obtain last gene2cs update status.", e );
        }
    }

    private void sendEmail( Gene2CsStatus results ) {
        if ( !sendEmail )
            return;
        mailEngine.sendAdminMessage( "Gene2Cs update status.", "Gene2Cs updating was run.\n" + results.getAnnotation() );
    }

    /**
     * @param annotation extra text that describes the status
     */
    private Gene2CsStatus writeUpdateStatus( String annotation, @Nullable Exception e ) {
        Gene2CsStatus status = new Gene2CsStatus();
        Calendar c = Calendar.getInstance();
        Date date = c.getTime();
        status.setLastUpdate( date );
        status.setError( e );
        status.setAnnotation( annotation );
        try {
            FileUtils.forceMkdirParent( gene2CsInfoPath.toFile() );
            try ( ObjectOutputStream oos = new ObjectOutputStream( Files.newOutputStream( gene2CsInfoPath ) ) ) {
                oos.writeObject( status );
            }
        } catch ( IOException e2 ) {
            throw new RuntimeException( "Failed to update gene2cs update status.", e2 );
        }
        return status;
    }

    private void updateGene2csExternalDatabaseLastUpdated( Gene2CsStatus status ) {
        ExternalDatabase ed = externalDatabaseService.findByNameWithAuditTrail( ExternalDatabases.GENE2CS );
        if ( ed != null ) {
            externalDatabaseService.updateReleaseLastUpdated( ed, status.getAnnotation(), status.getLastUpdate() );
        } else {
            log.warn( String.format( "External database with name %s is missing, no audit event will be recorded.", ExternalDatabases.GENE2CS ) );
        }
    }
}

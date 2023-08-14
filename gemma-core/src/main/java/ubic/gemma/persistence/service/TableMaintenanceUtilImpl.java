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

package ubic.gemma.persistence.service;

import io.micrometer.core.annotation.Timed;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignGeneMappingEvent;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.model.Gene2CsStatus;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.genome.GeneDao;
import ubic.gemma.persistence.util.MailEngine;
import ubic.gemma.persistence.util.Settings;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
@Service
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
     * Query used to repopulate the EXPRESSION_EXPERIMENT2CHARACTERISTIC table.
     */
    private static final String E2C_QUERY =
            "replace into EXPRESSION_EXPERIMENT2CHARACTERISTIC (ID, NAME, DESCRIPTION, CATEGORY, CATEGORY_URI, `VALUE`, VALUE_URI, ORIGINAL_VALUE, EVIDENCE_CODE, EXPRESSION_EXPERIMENT_FK, LEVEL) "
                    + "select C.ID, C.NAME, C.DESCRIPTION, C.CATEGORY, C.CATEGORY_URI, C.`VALUE`, C.VALUE_URI, C.ORIGINAL_VALUE, C.EVIDENCE_CODE, I.ID, cast(:eeClass as char(255)) "
                    + "from INVESTIGATION I "
                    + "join CHARACTERISTIC C on I.ID = C.INVESTIGATION_FK "
                    + "where I.class = 'ExpressionExperiment' "
                    + "union "
                    + "select C.ID, C.NAME, C.DESCRIPTION, C.CATEGORY, C.CATEGORY_URI, C.`VALUE`, C.VALUE_URI, C.ORIGINAL_VALUE, C.EVIDENCE_CODE, I.ID, cast(:bmClass as char(255)) "
                    + "from INVESTIGATION I "
                    + "join BIO_ASSAY BA on I.ID = BA.EXPRESSION_EXPERIMENT_FK "
                    + "join BIO_MATERIAL BM on BA.SAMPLE_USED_FK = BM.ID "
                    + "join BIO_MATERIAL_FACTOR_VALUES BMFV on BM.ID = BMFV.BIO_MATERIALS_FK "
                    + "join FACTOR_VALUE FV on BMFV.FACTOR_VALUES_FK = FV.ID "
                    + "join CHARACTERISTIC C on FV.ID = C.FACTOR_VALUE_FK "
                    + "where I.class = 'ExpressionExperiment' "
                    + "union "
                    + "select C.ID, C.NAME, C.DESCRIPTION, C.CATEGORY, C.CATEGORY_URI, C.`VALUE`, C.VALUE_URI, C.ORIGINAL_VALUE, C.EVIDENCE_CODE, I.ID, cast(:edClass as char(255)) "
                    + "from INVESTIGATION I "
                    + "join EXPERIMENTAL_DESIGN on I.EXPERIMENTAL_DESIGN_FK = EXPERIMENTAL_DESIGN.ID "
                    + "join EXPERIMENTAL_FACTOR EF on EXPERIMENTAL_DESIGN.ID = EF.EXPERIMENTAL_DESIGN_FK "
                    + "join FACTOR_VALUE FV on FV.EXPERIMENTAL_FACTOR_FK = EF.ID "
                    + "join CHARACTERISTIC C on FV.ID = C.FACTOR_VALUE_FK "
                    + "where I.class = 'ExpressionExperiment'";
    private static final Path DEFAULT_GENE2CS_INFO_PATH = Paths.get( Settings.getString( "gemma.appdata.home" ), "DbReports", "gene2cs.info" );
    private static final Log log = LogFactory.getLog( TableMaintenanceUtil.class.getName() );
    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private MailEngine mailEngine;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private SessionFactory sessionFactory;

    private Path gene2CsInfoPath = DEFAULT_GENE2CS_INFO_PATH;
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
    public int updateExpressionExperiment2CharacteristicEntries() {
        log.info( "Updating the EXPRESSION_EXPERIMENT2CHARACTERISTIC table..." );
        int updated = sessionFactory.getCurrentSession().createSQLQuery( E2C_QUERY )
                .addSynchronizedQuerySpace( "EXPRESSION_EXPERIMENT2CHARACTERISTIC" )
                .setParameter( "eeClass", ExpressionExperiment.class )
                .setParameter( "bmClass", BioMaterial.class )
                .setParameter( "edClass", ExperimentalDesign.class )
                .executeUpdate();
        log.info( String.format( "Done updating the EXPRESSION_EXPERIMENT2CHARACTERISTIC table; %d entries were updated.", updated ) );
        return updated;
    }

    /**
     * For use in tests.
     */
    @Override
    public void setGene2CsInfoPath( Path gene2CsInfoPath ) {
        this.gene2CsInfoPath = gene2CsInfoPath;
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
        TableMaintenanceUtilImpl.log.info( "Updating the GENE2CS table..." );
        int updated = this.sessionFactory.getCurrentSession()
                .createSQLQuery( TableMaintenanceUtilImpl.GENE2CS_REPOPULATE_QUERY )
                .addSynchronizedQuerySpace( "GENE2CS" )
                .executeUpdate();
        TableMaintenanceUtilImpl.log.info( String.format( "Done regenerating the GENE2CS table; %d entries were updated.", updated ) );
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
        SimpleMailMessage msg = new SimpleMailMessage();
        String adminEmailAddress = Settings.getAdminEmailAddress();
        if ( StringUtils.isBlank( adminEmailAddress ) ) {
            TableMaintenanceUtilImpl.log
                    .warn( "No administrator email address could be found, so gene2cs status email will not be sent." );
            return;
        }
        msg.setTo( adminEmailAddress );
        msg.setSubject( "Gene2Cs update status." );
        msg.setText( "Gene2Cs updating was run.\n" + results.getAnnotation() );
        mailEngine.send( msg );
        TableMaintenanceUtilImpl.log.info( "Email notification sent to " + adminEmailAddress );
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

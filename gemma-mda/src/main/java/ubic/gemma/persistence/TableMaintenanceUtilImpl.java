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

package ubic.gemma.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventService;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignGeneMappingEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.GeneDao;
import ubic.gemma.util.Settings;
import ubic.gemma.util.MailEngine;

/**
 * Functions for maintaining the database. This is intended for denormalized tables and statistics tables that need to
 * be generated periodically.
 * 
 * @author jsantos
 * @author paul
 * @version $Id$
 */
@Service
public class TableMaintenanceUtilImpl implements TableMaintenenceUtil {

    /**
     * The query used to repopulate the contents of the GENE2CS table.
     */
    private static final String GENE2CS_REPOPULATE_QUERY = "INSERT INTO GENE2CS (GENE, CS, AD) "
            + "SELECT DISTINCT gene.ID, cs.ID, cs.ARRAY_DESIGN_FK "
            + " FROM CHROMOSOME_FEATURE AS gene, CHROMOSOME_FEATURE AS geneprod,BIO_SEQUENCE2_GENE_PRODUCT AS bsgp,COMPOSITE_SEQUENCE cs "
            + " WHERE geneprod.GENE_FK = gene.ID and bsgp.GENE_PRODUCT_FK = geneprod.ID and "
            + " bsgp.BIO_SEQUENCE_FK = cs.BIOLOGICAL_CHARACTERISTIC_FK ORDER BY gene.ID,cs.ARRAY_DESIGN_FK";

    private static Log log = LogFactory.getLog( TableMaintenanceUtilImpl.class.getName() );

    private static final String HOME_DIR = Settings.getString( "gemma.appdata.home" );

    /**
     * The location where reports are stored.
     */
    private static final String DB_INFO_DIR = "DbReports";

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private MailEngine mailEngine;

    @Autowired
    private SessionFactory sessionFactory;

    private boolean sendEmail = true;

    /*
     * For use in tests.
     */
    @Override
    public void disableEmail() {
        this.sendEmail = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.TableMaintenenceUtilI#updateGene2CsEntries()
     */
    @Override
    @Transactional
    public void updateGene2CsEntries() {

        log.debug( "Running Gene2CS status check" );

        String annotation = "";
        try {

            Gene2CsStatus status = getLastGene2CsUpdateStatus();
            boolean needToRefresh = false;
            if ( status == null ) {
                needToRefresh = true;
            }

            if ( !needToRefresh && status != null ) {
                Collection<Auditable> newObj = auditEventService.getNewSinceDate( status.getLastUpdate() );

                for ( Auditable a : newObj ) {
                    if ( a instanceof ArrayDesign ) {
                        needToRefresh = true;
                        annotation = a + " is new since " + status.getLastUpdate();
                        log.debug( annotation );
                        break;
                    }
                }
            }

            if ( !needToRefresh && status != null ) {
                Collection<Auditable> updatedObj = auditEventService.getUpdatedSinceDate( status.getLastUpdate() );
                for ( Auditable a : updatedObj ) {
                    if ( a instanceof ArrayDesign ) {
                        for ( AuditEvent ae : auditEventService.getEvents( a ) ) {
                            if ( ae == null ) continue; // legacy of ordered-list which could end up with gaps; should
                                                        // not be needed any more
                            if ( ae.getEventType() != null && ae.getEventType() instanceof ArrayDesignGeneMappingEvent
                                    && ae.getDate().after( status.getLastUpdate() ) ) {
                                needToRefresh = true;
                                annotation = a + " had probe mapping done since: " + status.getLastUpdate();
                                log.debug( annotation );
                                break;
                            }
                        }
                    }
                    if ( needToRefresh == true ) break;
                }
            }

            if ( needToRefresh ) {
                log.debug( "Update of GENE2CS initiated" );
                generateGene2CsEntries();
                Gene2CsStatus updatedStatus = writeUpdateStatus( annotation, null );
                sendEmail( updatedStatus );

            } else {
                log.debug( "No update of GENE2CS needed" );
            }

        } catch ( Exception e ) {
            try {
                log.info( "Error during attempt to check status or update GENE2CS", e );
                Gene2CsStatus updatedStatus = writeUpdateStatus( annotation, e );
                sendEmail( updatedStatus );
            } catch ( IOException e1 ) {
                throw new RuntimeException( e1 );
            }
        }
    }

    /**
     * Function to regenerate the GENE2CS entries. Gene2Cs is a denormalized join table that allows for a quick link
     * between Genes and CompositeSequences
     * 
     * @see GeneDao for where the GENE2CS table is used extensively.
     */
    private void generateGene2CsEntries() throws Exception {
        log.info( "Updating Gene2Cs ..." );
        Session session = this.sessionFactory.getCurrentSession();
        log.info( "Deleting all entries for Gene2Cs." );
        String queryString = "DELETE FROM GENE2CS"; // Truncate doesn't work., bug 2057
        org.hibernate.SQLQuery queryObject;

        queryObject = session.createSQLQuery( queryString ); // for native query.
        int deleted = queryObject.executeUpdate();

        log.info( "Deleted " + deleted + "; Recreating all entries for Gene2Cs." );
        queryString = GENE2CS_REPOPULATE_QUERY;
        queryObject = session.createSQLQuery( queryString ); // for native query.
        queryObject.executeUpdate();
        log.info( "Done regenerating Gene2Cs." );

        session.flush();
        session.clear();

    }

    /**
     * 
     */
    private File getGene2CsInfopath() {
        return new File( HOME_DIR + File.separatorChar + DB_INFO_DIR + File.separatorChar + "gene2cs.info" );
    }

    /**
     * Reads previous run information from disk.
     * 
     * @return null if there is no update information available.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private Gene2CsStatus getLastGene2CsUpdateStatus() throws IOException, ClassNotFoundException {
        File gene2CsInfopath = getGene2CsInfopath();
        if ( !gene2CsInfopath.canRead() ) {
            return null;
        }
        FileInputStream fis = new FileInputStream( gene2CsInfopath );
        ObjectInputStream ois = new ObjectInputStream( fis );
        Gene2CsStatus d = ( Gene2CsStatus ) ois.readObject();
        ois.close();
        fis.close();

        return d;
    }

    /**
     * @param deleteFiles
     */
    private void initDirectories() {
        FileTools.createDir( HOME_DIR );
        FileTools.createDir( HOME_DIR + File.separatorChar + DB_INFO_DIR );
    }

    /**
     * @param results
     */
    private void sendEmail( Gene2CsStatus results ) {
        if ( !sendEmail ) return;
        SimpleMailMessage msg = new SimpleMailMessage();
        String adminEmailAddress = Settings.getAdminEmailAddress();
        if ( StringUtils.isBlank( adminEmailAddress ) ) {
            log.warn( "No administrator email address could be found, so gene2cs status email will not be sent." );
            return;
        }
        msg.setTo( adminEmailAddress );
        msg.setSubject( "Gene2Cs update status." );
        msg.setText( "Gene2Cs updating was run.\n" + results.getAnnotation() );
        mailEngine.send( msg );
        log.info( "Email notification sent to " + adminEmailAddress );
    }

    /**
     * @param annotation extra text that describes the status
     * @param e
     * @throws IOException
     */
    private Gene2CsStatus writeUpdateStatus( String annotation, Exception e ) throws IOException {
        initDirectories();
        Gene2CsStatus status = new Gene2CsStatus();
        Calendar c = Calendar.getInstance();
        Date date = c.getTime();
        status.setLastUpdate( date );
        status.setError( e );
        status.setAnnotation( annotation );

        FileOutputStream fos = new FileOutputStream( getGene2CsInfopath() );
        ObjectOutputStream oos = new ObjectOutputStream( fos );
        oos.writeObject( status );
        oos.flush();
        oos.close();
        return status;
    }

}

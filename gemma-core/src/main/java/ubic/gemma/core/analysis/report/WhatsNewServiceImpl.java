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
package ubic.gemma.core.analysis.report;

import gemma.gsec.AuthorityConstants;
import gemma.gsec.SecurityService;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.util.FileTools;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to collect data on object that are new in the system.
 *
 * @author pavlidis
 */
@Component("whatsNewService")
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class WhatsNewServiceImpl implements WhatsNewService {

    private static final Log log = LogFactory.getLog( WhatsNewServiceImpl.class.getName() );

    private static final String WHATS_NEW_DIR = "WhatsNew";
    private static final String WHATS_NEW_FILE = "WhatsNew";

    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private AuditEventService auditEventService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private SecurityService securityService;

    @Value("${gemma.appdata.home}")
    private String homeDir;

    @Override
    @Transactional(readOnly = true)
    public WhatsNew getDailyReport() {
        Calendar c = Calendar.getInstance();
        Date date = c.getTime();
        date = DateUtils.addDays( date, -1 );
        return this.getReportAsAnonymousUser( date );
    }

    @Override
    @Transactional(readOnly = true)
    public WhatsNew getWeeklyReport() {
        Calendar c = Calendar.getInstance();
        Date date = c.getTime();
        date = DateUtils.addWeeks( date, -1 );
        return this.getReportAsAnonymousUser( date );
    }

    @Override
    @Transactional(readOnly = true)
    public WhatsNew generateWeeklyReport() {
        Calendar c = Calendar.getInstance();
        Date date = c.getTime();
        WhatsNew wn = this.getReportAsAnonymousUser( DateUtils.addWeeks( date, -1 ) );
        this.initDirectories();
        this.saveLatestWeeklyReport( wn, date );
        return wn;
    }

    @Override
    @Transactional(readOnly = true)
    public WhatsNew getLatestWeeklyReport() {
        WhatsNew wn = new WhatsNew();
        File newObjects = new File(
                homeDir + File.separatorChar + WhatsNewServiceImpl.WHATS_NEW_DIR + File.separatorChar
                        + WhatsNewServiceImpl.WHATS_NEW_FILE + ".new" );
        File updatedObjects = new File(
                homeDir + File.separatorChar + WhatsNewServiceImpl.WHATS_NEW_DIR + File.separatorChar
                        + WhatsNewServiceImpl.WHATS_NEW_FILE + ".updated" );
        if ( !newObjects.exists() && !updatedObjects.exists() ) {
            return null;
        }

        StopWatch timer = StopWatch.createStarted();
        StopWatch loadNewObjectsTimer = StopWatch.create();
        StopWatch loadUpdatedObjectsTimer = StopWatch.create();

        // restore the date from the last modified -1 week
        wn.setDate( DateUtils.addWeeks( new Date( newObjects.lastModified() ), -1 ) );

        // load up all new objects
        loadNewObjectsTimer.start();
        if ( newObjects.exists() ) {
            Collection<AuditableObject> aos = this.loadAuditableObjects( newObjects );
            Map<String, List<AuditableObject>> aosByType = aos.stream()
                    .collect( Collectors.groupingBy( AuditableObject::getType, Collectors.toList() ) );
            for ( Map.Entry<String, List<AuditableObject>> entry : aosByType.entrySet() ) {
                Collection<? extends Auditable> objects = this.fetchAllByType( entry.getValue(), entry.getKey() );
                for ( AuditableObject ao : entry.getValue() ) {
                    this.updateDate( wn, ao );
                }
                wn.addNewObjects( objects );
            }
        }
        loadNewObjectsTimer.stop();

        // load up all updated objects
        loadUpdatedObjectsTimer.start();
        if ( updatedObjects.exists() ) {
            Collection<AuditableObject> aos = this.loadAuditableObjects( updatedObjects );
            Map<String, List<AuditableObject>> aosByType = aos.stream()
                    .collect( Collectors.groupingBy( AuditableObject::getType, Collectors.toList() ) );
            for ( Map.Entry<String, List<AuditableObject>> entry : aosByType.entrySet() ) {
                /*
                 * This call takes ~ 15-20 ms, but it can be called many times if there are a lot of updated
                 * experiments, meaning this loop can take >8500 ms (over tunnel for ~450 experiments).
                 *
                 * Loading objects could be avoided since we only need ids on the front end, but we would need to
                 * refactor the cache, because object-type is used to calculate counts for updated array design
                 * objects vs updated experiments
                 *
                 * This is probably not necessary because usually the number of updated or new experiments will be
                 * much lower than 450.
                 */
                Collection<? extends Auditable> objects = this.fetchAllByType( entry.getValue(), entry.getKey() );
                for ( AuditableObject ao : entry.getValue() ) {
                    this.updateDate( wn, ao );
                }
                wn.addUpdatedObjects( objects );
            }
        }
        loadUpdatedObjectsTimer.stop();

        // build total, new and updated counts by taxon to display in data summary widget on front page
        StopWatch funkyMapGenerationTimer = StopWatch.createStarted();
        wn.setNewEEIdsPerTaxon( this.getExpressionExperimentIdsByTaxon( wn.getNewExpressionExperiments() ) );
        wn.setUpdatedEEIdsPerTaxon(
                this.getExpressionExperimentIdsByTaxon( wn.getUpdatedExpressionExperiments() ) );
        funkyMapGenerationTimer.stop();
        timer.stop();

        if ( timer.getTime() > 500 ) {
            log.info( "Retrieving report took " + timer.getTime() + "ms ("
                    + "loading new: " + loadNewObjectsTimer.getTime() + " ms, "
                    + "loading updated: " + loadUpdatedObjectsTimer.getTime() + " ms, "
                    + "ee by taxon map creation: " + funkyMapGenerationTimer.getTime() + "ms)." );
        }
        return wn;
    }

    private Collection<? extends Auditable> fetchAllByType( List<AuditableObject> object, String type ) {
        if ( object == null )
            return null;
        List<Long> objectIds = object.stream().map( AuditableObject::getId ).collect( Collectors.toList() );
        if ( type.equalsIgnoreCase( "ArrayDesign" ) ) {
            return arrayDesignService.load( objectIds );
        } else if ( type.equalsIgnoreCase( "ExpressionExperiment" ) ) {
            // this is slower than loading them all at once but the cache saves even more time.
            return expressionExperimentService.load( objectIds );
        } else {
            throw new IllegalArgumentException( "Unsupported auditable " + type + "." );
        }
    }

    /**
     * Obtain the report from the perspective of an anonymous user.
     */
    private WhatsNew getReportAsAnonymousUser( Date date ) {
        // generate the report from an anonymous user perspective
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication( new AnonymousAuthenticationToken( "1234", AuthorityConstants.ANONYMOUS_USER_NAME,
                Collections.singleton( new SimpleGrantedAuthority( AuthorityConstants.ANONYMOUS_GROUP_AUTHORITY ) ) ) );
        try {
            return new DelegatingSecurityContextCallable<>( () -> getReport( date ), context ).call();
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    private WhatsNew getReport( Date date ) {
        WhatsNew wn = new WhatsNew( date );

        Collection<Auditable> updatedObjects = new HashSet<>();
        updatedObjects.addAll( auditEventService.getUpdatedSinceDate( ArrayDesign.class, date ) );
        updatedObjects.addAll( auditEventService.getUpdatedSinceDate( ExpressionExperiment.class, date ) );
        wn.setUpdatedObjects( updatedObjects );
        WhatsNewServiceImpl.log.info( wn.getUpdatedObjects().size() + " updated objects since " + date );

        Collection<Auditable> newObjects = new HashSet<>();
        newObjects.addAll( auditEventService.getNewSinceDate( ArrayDesign.class, date ) );
        newObjects.addAll( auditEventService.getNewSinceDate( ExpressionExperiment.class, date ) );
        wn.setNewObjects( newObjects );
        WhatsNewServiceImpl.log.info( wn.getNewObjects().size() + " new objects since " + date );

        Collection<ExpressionExperiment> updatedExpressionExperiments = this.getExpressionExperiments( updatedObjects );
        Collection<ExpressionExperiment> newExpressionExperiments = this.getExpressionExperiments( newObjects );
        Collection<ArrayDesign> updatedArrayDesigns = this.getArrayDesigns( updatedObjects );
        Collection<ArrayDesign> newArrayDesigns = this.getArrayDesigns( newObjects );

        // don't show things that are "new" as "updated" too (if they were updated after being loaded)
        updatedExpressionExperiments.removeAll( newExpressionExperiments );
        updatedArrayDesigns.removeAll( newArrayDesigns );

        // build total, new and updated counts by taxon to display in data summary widget on front page
        wn.setNewEEIdsPerTaxon( this.getExpressionExperimentIdsByTaxon( newExpressionExperiments ) );
        wn.setUpdatedEEIdsPerTaxon( this.getExpressionExperimentIdsByTaxon( updatedExpressionExperiments ) );

        wn.setNewBioMaterialCount( this.getBioMaterialCount( newExpressionExperiments ) );

        return wn;
    }

    /**
     * @param items a collection of objects that may include array designs
     * @return the array design subset of the collection passed in
     */
    private Collection<ArrayDesign> getArrayDesigns( Collection<Auditable> items ) {

        Collection<ArrayDesign> ads = new HashSet<>();
        for ( Auditable auditable : items ) {
            if ( auditable instanceof ArrayDesign ) {
                ads.add( ( ArrayDesign ) auditable );
            }
        }
        return ads;
    }

    /**
     * @param ees a collection of expression experiments
     * @return the number of biomaterials in all the expression experiments passed in
     */
    private long getBioMaterialCount( Collection<ExpressionExperiment> ees ) {
        long count = 0;
        for ( ExpressionExperiment ee : ees ) {
            count += this.expressionExperimentService.getBioMaterialCount( ee );
        }
        return count;
    }

    /**
     * Give breakdown by taxon. "Private" experiments are not included.
     */
    private Map<Taxon, Collection<Long>> getExpressionExperimentIdsByTaxon( Collection<ExpressionExperiment> ees ) {
        /*
         * Sort taxa by name.
         */
        SortedMap<Taxon, Collection<Long>> eesPerTaxon = new TreeMap<>( Comparator.comparing( Taxon::getScientificName, Comparator.nullsLast( Comparator.naturalOrder() ) ) );

        if ( ees.isEmpty() )
            return eesPerTaxon;

        Collection<ExpressionExperiment> publicEEs = securityService.choosePublic( ees );

        Map<ExpressionExperiment, Taxon> taxa = expressionExperimentService.getTaxa( publicEEs );

        // invert the map.
        for ( ExpressionExperiment ee : taxa.keySet() ) {
            Taxon t = taxa.get( ee );
            Collection<Long> ids;
            if ( eesPerTaxon.containsKey( t ) ) {
                ids = eesPerTaxon.get( t );
            } else {
                ids = new ArrayList<>();
            }
            ids.add( ee.getId() );
            eesPerTaxon.put( t, ids );
        }
        return eesPerTaxon;
    }

    /**
     * @param items a collection of objects that may include expression experiments
     * @return the expression experiment subset of the collection passed in
     */
    private Collection<ExpressionExperiment> getExpressionExperiments( Collection<Auditable> items ) {

        Collection<ExpressionExperiment> ees = new HashSet<>();
        for ( Auditable auditable : items ) {
            if ( auditable instanceof ExpressionExperiment ) {
                ees.add( ( ExpressionExperiment ) auditable );
            }
        }
        return ees;
    }

    private void initDirectories() {
        // check to see if the home directory exists. If it doesn't, create it.
        // check to see if the reports directory exists. If it doesn't, create it.
        FileTools.createDir( homeDir );
        FileTools.createDir( homeDir + File.separatorChar + WhatsNewServiceImpl.WHATS_NEW_DIR );
        File f = new File( homeDir + File.separatorChar + WhatsNewServiceImpl.WHATS_NEW_DIR );
        Collection<File> files = new ArrayList<>();
        File[] fileArray = f.listFiles();
        if ( fileArray != null ) {
            Collections.addAll( files, fileArray );
        }
        // clear out all files
        FileTools.deleteFiles( files );
    }

    private Collection<AuditableObject> loadAuditableObjects( File newObjects ) {
        try ( FileInputStream fis = new FileInputStream( newObjects );
                ObjectInputStream ois = new ObjectInputStream( fis ) ) {
            @SuppressWarnings("unchecked")
            Collection<AuditableObject> aos = ( Collection<AuditableObject> ) ois
                    .readObject();
            return aos;
        } catch ( IOException | ClassNotFoundException e ) {
            throw new RuntimeException( e );
        }
    }

    private void saveLatestWeeklyReport( WhatsNew wn, Date dateRetrieved ) {
        // remove file first
        File newOutput = Paths.get( homeDir, WhatsNewServiceImpl.WHATS_NEW_DIR, WhatsNewServiceImpl.WHATS_NEW_FILE + ".new" ).toFile();
        File updatedOutput = Paths.get( homeDir, WhatsNewServiceImpl.WHATS_NEW_DIR, WhatsNewServiceImpl.WHATS_NEW_FILE + ".updated" ).toFile();
        if ( newOutput.exists() && !newOutput.delete() ) {
            throw new RuntimeException( "Could not delete " + newOutput.getName() );
        }
        if ( updatedOutput.exists() && !updatedOutput.delete() ) {
            throw new RuntimeException( "Could not delete " + updatedOutput.getName() );
        }

        Collection<ArrayDesign> ads = wn.getNewArrayDesigns();
        Collection<ExpressionExperiment> ees = wn.getNewExpressionExperiments();
        // save the IDs for new Auditables
        Collection<AuditableObject> newObjects = new ArrayList<>();
        this.addAllADs( dateRetrieved, ads, newObjects );
        this.addAllEEs( dateRetrieved, ees, newObjects );

        // save the ids for updated Auditables
        ads = wn.getUpdatedArrayDesigns();
        ees = wn.getUpdatedExpressionExperiments();
        // save the IDs for new Auditables
        Collection<AuditableObject> updatedObjects = new ArrayList<>();
        this.addAllADs( dateRetrieved, ads, updatedObjects );
        this.addAllEEs( dateRetrieved, ees, updatedObjects );
        try ( FileOutputStream fos = new FileOutputStream( newOutput );
                ObjectOutputStream oos = new ObjectOutputStream( fos ) ) {
            oos.writeObject( newObjects );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        try ( FileOutputStream fos = new FileOutputStream( updatedOutput );
                ObjectOutputStream oos = new ObjectOutputStream( fos ) ) {
            oos.writeObject( updatedObjects );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        // set the last modified on the generated files, so that we can restore the report date when it's loaded at a
        // later point
        if ( !newOutput.setLastModified( dateRetrieved.getTime() ) || !updatedOutput.setLastModified( dateRetrieved.getTime() ) ) {
            log.warn( "Failed to set the last modified date on the WhatsNew files, the date might be inaccurate when loading the report." );
        }
    }

    private void addAllADs( Date date, Collection<ArrayDesign> ads, Collection<AuditableObject> updatedObjects ) {
        for ( ArrayDesign ad : ads ) {
            AuditableObject ao = new AuditableObject();
            ao.date = date;
            ao.type = "ArrayDesign";
            ao.id = ad.getId();
            updatedObjects.add( ao );
        }
    }

    private void addAllEEs( Date date, Collection<ExpressionExperiment> ees, Collection<AuditableObject> newObjects ) {
        for ( ExpressionExperiment ee : ees ) {
            AuditableObject ao = new AuditableObject();
            ao.date = date;
            ao.type = "ExpressionExperiment";
            ao.id = ee.getId();
            newObjects.add( ao );
        }
    }

    /**
     * Sets the date to the earliest update date of any object that has been retrieved so far.
     */
    private void updateDate( WhatsNew wn, AuditableObject object ) {
        if ( object.getDate() != null && ( wn.getDate() == null || wn.getDate().after( object.getDate() ) ) ) {
            wn.setDate( object.getDate() );
        }
    }

}

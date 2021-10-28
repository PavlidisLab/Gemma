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

import gemma.gsec.SecurityService;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.util.FileTools;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Settings;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to collect data on object that are new in the system.
 *
 * @author pavlidis
 */
@Component
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class WhatsNewServiceImpl implements WhatsNewService {

    private static final String WHATS_NEW_DIR = "WhatsNew";
    private static final String WHATS_NEW_FILE = "WhatsNew";
    private static final Log log = LogFactory.getLog( WhatsNewServiceImpl.class.getName() );
    private final String HOME_DIR = Settings.getString( "gemma.appdata.home" );
    @Autowired
    private ArrayDesignService arrayDesignService = null;
    @Autowired
    private AuditEventService auditEventService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService = null;
    @Autowired
    private SecurityService securityService = null;

    @Override
    public void generateWeeklyReport() {
        Calendar c = Calendar.getInstance();
        Date date = c.getTime();
        date = DateUtils.addDays( date, -7 );
        this.saveReport( date );
    }

    /**
     * save the report from the date specified. This will be the report that will be used by the WhatsNew box.
     */
    @Override
    public void saveReport( Date date ) {
        WhatsNew wn = this.getReport( date );
        this.initDirectories();
        this.saveFile( wn );
    }

    @Override
    public WhatsNew getReport() {
        Calendar c = Calendar.getInstance();
        Date date = c.getTime();
        date = DateUtils.addWeeks( date, -1 );
        return this.getReport( date );
    }

    @Override
    public WhatsNew getReport( Date date ) {
        WhatsNew wn = new WhatsNew( date );

        Collection<Auditable> updatedObjects = auditEventService.getUpdatedSinceDate( date );
        wn.setUpdatedObjects( updatedObjects );
        WhatsNewServiceImpl.log.info( wn.getUpdatedObjects().size() + " updated objects since " + date );

        Collection<Auditable> newObjects = auditEventService.getNewSinceDate( date );
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
     * Retrieve the latest WhatsNew report.
     *
     * @return WhatsNew the latest WhatsNew report cache.
     */
    @Override
    public WhatsNew retrieveReport() {
        WhatsNew wn = new WhatsNew();
        try {
            File newObjects = new File(
                    HOME_DIR + File.separatorChar + WhatsNewServiceImpl.WHATS_NEW_DIR + File.separatorChar
                            + WhatsNewServiceImpl.WHATS_NEW_FILE + ".new" );
            File updatedObjects = new File(
                    HOME_DIR + File.separatorChar + WhatsNewServiceImpl.WHATS_NEW_DIR + File.separatorChar
                            + WhatsNewServiceImpl.WHATS_NEW_FILE + ".updated" );
            if ( !newObjects.exists() && !updatedObjects.exists() ) {
                return null;
            }

            // load up all new objects
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

            // load up all updated objects
            if ( updatedObjects.exists() ) {
                Collection<AuditableObject> aos = this.loadAuditableObjects( updatedObjects );
                Map<String, List<AuditableObject>> aosByType = aos.stream()
                        .collect( Collectors.groupingBy( AuditableObject::getType, Collectors.toList() ) );
                for ( Map.Entry<String, List<AuditableObject>> entry : aosByType.entrySet() ) {
                    /*
                     * This call takes ~ 15-20 ms but it can be called many times if there are a lot of updated
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
            // build total, new and updated counts by taxon to display in data summary widget on front page
            wn.setNewEEIdsPerTaxon( this.getExpressionExperimentIdsByTaxon( wn.getNewExpressionExperiments() ) );
            wn.setUpdatedEEIdsPerTaxon(
                    this.getExpressionExperimentIdsByTaxon( wn.getUpdatedExpressionExperiments() ) );

        } catch ( Throwable e ) {
            WhatsNewServiceImpl.log.error( e, e );
            return null;
        }
        return wn;
    }

    /**
     * @param arrayDesignService the arrayDesignService to set
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    public void setAuditEventService( AuditEventService auditEventService ) {
        this.auditEventService = auditEventService;
    }

    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param securityService the securityService to set
     */
    public void setSecurityService( SecurityService securityService ) {
        this.securityService = securityService;
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
    private int getBioMaterialCount( Collection<ExpressionExperiment> ees ) {

        int count = 0;
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
        TreeMap<Taxon, Collection<Long>> eesPerTaxon = new TreeMap<>( new Comparator<Taxon>() {
            @Override
            public int compare( Taxon o1, Taxon o2 ) {
                if ( o1 == null ) {
                    return 1;
                } else if ( o2 == null ) {
                    return -1;
                } else {
                    return o1.getScientificName().compareTo( o2.getScientificName() );
                }
            }
        } );

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
        FileTools.createDir( HOME_DIR );
        FileTools.createDir( HOME_DIR + File.separatorChar + WhatsNewServiceImpl.WHATS_NEW_DIR );
        File f = new File( HOME_DIR + File.separatorChar + WhatsNewServiceImpl.WHATS_NEW_DIR );
        Collection<File> files = new ArrayList<>();
        File[] fileArray = f.listFiles();
        if ( fileArray != null ) {
            Collections.addAll( files, fileArray );
        }
        // clear out all files
        FileTools.deleteFiles( files );
    }

    private Collection<AuditableObject> loadAuditableObjects( File newObjects )
            throws IOException, ClassNotFoundException {
        try ( FileInputStream fis = new FileInputStream( newObjects );
                ObjectInputStream ois = new ObjectInputStream( fis ) ) {
            @SuppressWarnings("unchecked")
            Collection<AuditableObject> aos = ( Collection<AuditableObject> ) ois
                    .readObject();
            return aos;
        }
    }

    private void saveFile( WhatsNew wn ) {
        try {
            // remove file first
            File newOutput = new File(
                    HOME_DIR + File.separatorChar + WhatsNewServiceImpl.WHATS_NEW_DIR + File.separatorChar
                            + WhatsNewServiceImpl.WHATS_NEW_FILE + ".new" );
            File updatedOutput = new File(
                    HOME_DIR + File.separatorChar + WhatsNewServiceImpl.WHATS_NEW_DIR + File.separatorChar
                            + WhatsNewServiceImpl.WHATS_NEW_FILE + ".updated" );
            if ( newOutput.exists() ) {
                if ( !newOutput.delete() ) {
                    WhatsNewServiceImpl.log.error( "Could not delete " + newOutput.getName() );
                }
            }
            if ( updatedOutput.exists() ) {
                if ( !updatedOutput.delete() ) {
                    WhatsNewServiceImpl.log.error( "Could not delete " + updatedOutput.getName() );
                }
            }
            Calendar c = Calendar.getInstance();
            Date date = c.getTime();

            Collection<ArrayDesign> ads = wn.getNewArrayDesigns();
            Collection<ExpressionExperiment> ees = wn.getNewExpressionExperiments();
            // save the IDs for new Auditables
            Collection<AuditableObject> newObjects = new ArrayList<>();
            this.addAllADs( date, ads, newObjects );
            this.addAllEEs( date, ees, newObjects );

            // save the ids for updated Auditables
            ads = wn.getUpdatedArrayDesigns();
            ees = wn.getUpdatedExpressionExperiments();
            // save the IDs for new Auditables
            Collection<AuditableObject> updatedObjects = new ArrayList<>();
            this.addAllADs( date, ads, updatedObjects );
            this.addAllEEs( date, ees, updatedObjects );
            try ( FileOutputStream fos = new FileOutputStream( newOutput );
                    ObjectOutputStream oos = new ObjectOutputStream( fos ) ) {
                oos.writeObject( newObjects );
            }

            try ( FileOutputStream fos = new FileOutputStream( updatedOutput );
                    ObjectOutputStream oos = new ObjectOutputStream( fos ) ) {
                oos.writeObject( updatedObjects );
            }

        } catch ( Throwable e ) {
            log.error( "Error while saving the what's new file.", e );
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

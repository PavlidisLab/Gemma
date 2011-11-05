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
package ubic.gemma.analysis.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEventService; 
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.security.SecurityService;
import ubic.gemma.util.ConfigUtils;

/**
 * Service to collect data on object that are new in the system.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Service
public class WhatsNewServiceImpl implements InitializingBean, WhatsNewService {

    private static Log log = LogFactory.getLog( WhatsNewServiceImpl.class.getName() );

    @Autowired
    ArrayDesignService arrayDesignService = null;

    @Autowired
    AuditEventService auditEventService;

    @Autowired
    ExpressionExperimentService expressionExperimentService = null;
    
    @Autowired
    SecurityService securityService = null;

    @Autowired
    TaxonService taxonService = null;

    @Autowired
    private CacheManager cacheManager = null;

    private String HOME_DIR = ConfigUtils.getString( "gemma.appdata.home" );

    private static final String WHATS_NEW_CACHE = "WhatsNew";

    private static final String WHATS_NEW_DIR = "WhatsNew";

    private static final String WHATS_NEW_FILE = "WhatsNew";

    private Cache whatsNewCache;

    public void afterPropertiesSet() throws Exception {
        try {

            if ( cacheManager.cacheExists( WHATS_NEW_CACHE ) ) {
                return;
            }

            // last two values are timetolive and timetoidle.
            whatsNewCache = new Cache( WHATS_NEW_CACHE, 1500, false, false, 12 * 3600, 12 * 3600 );

            cacheManager.addCache( whatsNewCache );
            whatsNewCache = cacheManager.getCache( WHATS_NEW_CACHE );

        } catch ( CacheException e ) {
            throw new RuntimeException( e );
        }

    }

    /*
     * // for Quartz (non-Javadoc)
     * @see ubic.gemma.analysis.report.WhatsNewServiceI#generateWeeklyReport()
     */
    public void generateWeeklyReport() {
        Calendar c = Calendar.getInstance();
        Date date = c.getTime();
        date = DateUtils.addDays( date, -7 );
        saveReport( date );
    }

    /**
     * @param date
     * @return representing the updated or new objects.
     */
    public WhatsNew getReport( Date date ) {
        WhatsNew wn = new WhatsNew( date );
        Collection<Auditable> updatedObjects =  auditEventService.getUpdatedSinceDate( date );
        wn.setUpdatedObjects(updatedObjects);
        log.info( wn.getUpdatedObjects().size() + " updated objects since " + date );
        Collection<Auditable> newObjects =  auditEventService.getNewSinceDate( date );
        wn.setNewObjects(newObjects);
        log.info( wn.getNewObjects().size() + " new objects since " + date );
        
        Collection<ExpressionExperiment> updatedExpressionExperiments = getExpressionExperiments( updatedObjects );
        Collection<ExpressionExperiment> newExpressionExperiments = getExpressionExperiments( newObjects );
        Collection<ArrayDesign> updatedArrayDesigns = getArrayDesigns( updatedObjects );
        Collection<ArrayDesign> newArrayDesigns= getArrayDesigns( newObjects );
        
        // don't show things that are "new" as "updated" too (if they were updated after being loaded) 
        updatedExpressionExperiments.removeAll( newExpressionExperiments );
        updatedArrayDesigns.removeAll( newArrayDesigns );
        
        // build total, new and updated counts by taxon to display in data summary widget on front page
        wn.setNewEEIdsPerTaxon( getExpressionExperimentIdsByTaxon( newExpressionExperiments ) );
        wn.setUpdatedEEIdsPerTaxon( getExpressionExperimentIdsByTaxon( updatedExpressionExperiments) );
        
        wn.setNewAssayCount(getAssayCount( newExpressionExperiments ));
        
        return wn;
    }
    
    /**
     * 
     * @param items a collection of objects that may include expression experiments
     * @return the expression experiment subset of the collection passed in 
     */
    private Collection<ExpressionExperiment> getExpressionExperiments(Collection<Auditable> items){
        
        Collection<ExpressionExperiment> ees = new HashSet<ExpressionExperiment>();
        for ( Auditable auditable : items ) {
            if ( auditable instanceof ExpressionExperiment ) {
                ees.add( ( ExpressionExperiment ) auditable );
            }
        }
        return ees;
    }
    /**
     * 
     * @param items a collection of objects that may include array designs
     * @return the array design subset of the collection passed in 
     */
    private Collection<ArrayDesign> getArrayDesigns(Collection<Auditable> items){
        
        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        for ( Auditable auditable : items ) {
            if ( auditable instanceof ArrayDesign ) {
                ads.add( ( ArrayDesign ) auditable );
            }
        }
        return ads;
    }
    
    /**
     * 
     * @param ees a collection of expression experiments
     * @return the number of assays in all the expression experiments passed in
     */
    private int getAssayCount(Collection<ExpressionExperiment> ees){
        
        int count = 0;
        //for ( ExpressionExperiment ee : ees ) {
            //count += ee.getBioAssays().size(); // TODO trying to access bio assays causes LazyInitializationException
        //}
        return count;
    }
    
    private Map<Taxon, Long> getExpressionExperimentCountsByTaxon(Collection<ExpressionExperiment> ees){
        /*
         * Sort taxa by name.
         */
        TreeMap<Taxon, Long> eesPerTaxon = new TreeMap<Taxon, Long>( new Comparator<Taxon>() {
            @Override
            public int compare( Taxon o1, Taxon o2 ) {
                if(o1 == null){
                    return 1;
                }else if(o2 == null){
                    return -1;
                }else{
                   return o1.getScientificName().compareTo( o2.getScientificName() );  
                }                                   
            }
        } );
                
        ExpressionExperiment ee = null;
        Taxon t = null;

        // get counts of new experiments by taxon
        for ( Iterator<ExpressionExperiment> it = ees.iterator(); it.hasNext(); ) { 
            ee = it.next();
            t = expressionExperimentService.getTaxon(ee.getId());
            if(t!=null){
                eesPerTaxon.put(t, (eesPerTaxon.containsKey(t))? (eesPerTaxon.get(t)+1): 1);
            }
        }
        return eesPerTaxon;
    } 
    
    private Map<Taxon, Collection<Long>> getExpressionExperimentIdsByTaxon(Collection<ExpressionExperiment> ees){
        /*
         * Sort taxa by name.
         */
        TreeMap<Taxon, Collection<Long>> eesPerTaxon = new TreeMap<Taxon, Collection<Long>>( new Comparator<Taxon>() {
            @Override
            public int compare( Taxon o1, Taxon o2 ) {
                if(o1 == null){
                    return 1;
                }else if(o2 == null){
                    return -1;
                }else{
                   return o1.getScientificName().compareTo( o2.getScientificName() );  
                }                                   
            }
        } );
                
        ExpressionExperiment ee = null;
        Taxon t = null;

        Collection<Long> ids;
        // get counts of new experiments by taxon
        for ( Iterator<ExpressionExperiment> it = ees.iterator(); it.hasNext(); ) { 
            ee = it.next();
            t = expressionExperimentService.getTaxon(ee.getId());
            if(t!=null){
                if(eesPerTaxon.containsKey(t)){
                    ids = eesPerTaxon.get(t);
                }else{
                    ids = new ArrayList<Long>();
                }
                ids.add( ee.getId() );
                eesPerTaxon.put(t, ids);
            }
        }
        return eesPerTaxon;
    }    

    /**
     * Retrieve the latest WhatsNew report.
     * 
     * @return WhatsNew the latest WhatsNew report cache.
     */
    public WhatsNew retrieveReport() {     
        WhatsNew wn = new WhatsNew();
        try {
            File newObjects = new File( HOME_DIR + File.separatorChar + WHATS_NEW_DIR + File.separatorChar
                    + WHATS_NEW_FILE + ".new" );
            File updatedObjects = new File( HOME_DIR + File.separatorChar + WHATS_NEW_DIR + File.separatorChar
                    + WHATS_NEW_FILE + ".updated" );
            if ( !newObjects.exists() && !updatedObjects.exists() ) {
                return null;
            }

            // load up all new objects
            if ( newObjects.exists() ) {
                Collection<AuditableObject> aos = loadAuditableObjects( newObjects );

                for ( AuditableObject object : aos ) {
                    Auditable auditable = fetch( wn, object );

                    if ( auditable == null ) continue;

                    wn.addNewObjects( auditable );
                    updateDate( wn, object );
                }

            }

            // load up all updated objects
            if ( updatedObjects.exists() ) {
            
                Collection<AuditableObject> aos = loadAuditableObjects( updatedObjects );
                for ( AuditableObject object : aos ) {
                    
                    /*
                     *  This call takes ~ 15-20 ms but it can be called many times if there are a lot of 
                     *  updated experiments, meaning this loop can take >8500 ms (over tunnel for ~450 experiments).
                     *  
                     *  Loading objects could be avoided since we only need ids on the front end, but we would
                     *  need to refactor the cache, because object-type is used
                     *  to calculate counts for updated array design objects vs updated experiments
                     *  
                     *  This is probably not necessary because usually the number of updated or new experiments
                     *  will be much lower than 450.
                     */
                    
                    Auditable auditable = fetch( wn, object );

                    if ( auditable == null ) continue;

                    wn.addUpdatedObjects( auditable );

                    updateDate( wn, object );

                }
            }
            // build total, new and updated counts by taxon to display in data summary widget on front page
            wn.setNewEEIdsPerTaxon( getExpressionExperimentIdsByTaxon( wn.getNewExpressionExperiments() ) );
            wn.setUpdatedEEIdsPerTaxon( getExpressionExperimentIdsByTaxon( wn.getUpdatedExpressionExperiments()) );
        
        } catch ( Throwable e ) {
            log.error( e, e );
            return null;
        }
        return wn;
    }

    /**
     * save the report from the date specified. This will be the report that will be used by the WhatsNew box.
     * 
     * @param date
     */
    public void saveReport( Date date ) {
        WhatsNew wn = getReport( date );
        initDirectories( true );
        saveFile( wn );
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
     * @param cacheManager the cacheManager to set
     */
    public void setCacheManager( CacheManager cacheManager ) {
        this.cacheManager = cacheManager;
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

    /**
     * @param wn
     * @param object
     * @return
     */
    private Auditable fetch( WhatsNew wn, AuditableObject object ) {

        if ( object == null ) return null;

        Auditable auditable = null;
        Element element = this.whatsNewCache.get( object );
        if ( object.type.equalsIgnoreCase( "ArrayDesign" ) ) {
            if ( element != null ) {
                auditable = ( Auditable ) element.getValue();
            } else {

                try {
                    auditable = arrayDesignService.load( object.getId() );
                } catch ( AccessDeniedException e ) {
                    return null;
                }

                whatsNewCache.put( new Element( object, auditable ) );
            }

        } else if ( object.type.equalsIgnoreCase( "ExpressionExperiment" ) ) {
            if ( element != null ) {
                auditable = ( Auditable ) element.getValue();
            } else {
                // this is slower than loading them all at once but the cache saves even more time.

                try {
                    auditable = expressionExperimentService.load( object.getId() );
                } catch ( AccessDeniedException e ) {
                    return null;
                }

                if ( auditable == null ) {
                    return null;
                }

                whatsNewCache.put( new Element( object, auditable ) );
            }
        }
        return auditable;
    }

    /**
     * @param deleteFiles
     */
    private void initDirectories( boolean deleteFiles ) {
        // check to see if the home directory exists. If it doesn't, create it.
        // check to see if the reports directory exists. If it doesn't, create it.
        FileTools.createDir( HOME_DIR );
        FileTools.createDir( HOME_DIR + File.separatorChar + WHATS_NEW_DIR );
        File f = new File( HOME_DIR + File.separatorChar + WHATS_NEW_DIR );
        Collection<File> files = new ArrayList<File>();
        File[] fileArray = f.listFiles();
        for ( File file : fileArray ) {
            files.add( file );
        }
        // clear out all files
        if ( deleteFiles ) {
            FileTools.deleteFiles( files );
        }
    }

    /**
     * @param newObjects
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private Collection<AuditableObject> loadAuditableObjects( File newObjects ) throws FileNotFoundException,
            IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream( newObjects );
        ObjectInputStream ois = new ObjectInputStream( fis );
        Collection<AuditableObject> aos = ( Collection<AuditableObject> ) ois.readObject();
        ois.close();
        fis.close();
        return aos;
    }

    /**
     * @param wn
     * @return
     */
    private boolean saveFile( WhatsNew wn ) {
        try {
            // remove file first
            File newOutput = new File( HOME_DIR + File.separatorChar + WHATS_NEW_DIR + File.separatorChar
                    + WHATS_NEW_FILE + ".new" );
            File updatedOutput = new File( HOME_DIR + File.separatorChar + WHATS_NEW_DIR + File.separatorChar
                    + WHATS_NEW_FILE + ".updated" );
            if ( newOutput.exists() ) {
                newOutput.delete();
            }
            if ( updatedOutput.exists() ) {
                updatedOutput.delete();
            }
            Calendar c = Calendar.getInstance();
            Date date = c.getTime();

            Collection<ArrayDesign> ads = wn.getNewArrayDesigns();
            Collection<ExpressionExperiment> ees = wn.getNewExpressionExperiments();
            // save the IDs for new Auditables
            Collection<AuditableObject> newObjects = new ArrayList<AuditableObject>();
            for ( ArrayDesign ad : ads ) {
                AuditableObject ao = new AuditableObject();
                ao.date = date;
                ao.type = "ArrayDesign";
                ao.id = ad.getId();
                newObjects.add( ao );
            }
            for ( ExpressionExperiment ee : ees ) {
                AuditableObject ao = new AuditableObject();
                ao.date = date;
                ao.type = "ExpressionExperiment";
                ao.id = ee.getId();
                newObjects.add( ao );
            }

            // save the ids for updated Auditables
            ads = wn.getUpdatedArrayDesigns();
            ees = wn.getUpdatedExpressionExperiments();
            // save the IDs for new Auditables
            Collection<AuditableObject> updatedObjects = new ArrayList<AuditableObject>();
            for ( ArrayDesign ad : ads ) {
                AuditableObject ao = new AuditableObject();
                ao.date = date;
                ao.type = "ArrayDesign";
                ao.id = ad.getId();
                updatedObjects.add( ao );
            }
            for ( ExpressionExperiment ee : ees ) {
                AuditableObject ao = new AuditableObject();
                ao.date = date;
                ao.type = "ExpressionExperiment";
                ao.id = ee.getId();
                updatedObjects.add( ao );
            }
            FileOutputStream fos = new FileOutputStream( newOutput );
            ObjectOutputStream oos = new ObjectOutputStream( fos );
            oos.writeObject( newObjects );
            oos.flush();
            oos.close();

            fos = new FileOutputStream( updatedOutput );
            oos = new ObjectOutputStream( fos );
            oos.writeObject( updatedObjects );
            oos.flush();
            oos.close();
        } catch ( Throwable e ) {
            return false;
        }

        return true;
    }

    /**
     * Sets the date to the earliest update date of any object that has been retrieved so far.
     * 
     * @param wn
     * @param object
     */
    private void updateDate( WhatsNew wn, AuditableObject object ) {
        if ( object.getDate() != null && ( wn.getDate() == null || wn.getDate().after( object.getDate() ) ) ) {
            wn.setDate( object.getDate() );
        }
    }

}

/**
 * 
 */
package ubic.gemma.expression.experiment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.util.ConfigUtils;

/**
 * @author jsantos
 * @spring.bean name="expressionExperimentReportService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="probe2ProbeCoexpressionService" ref="probe2ProbeCoexpressionService"
 */
public class ExpressionExperimentReportService {
    private Log log = LogFactory.getLog( this.getClass() );
    
    private String EE_LINK_SUMMARY = "AllExpressionLinkSummary";
    private String EE_REPORT_DIR = "ExpressionExperimentReports";
    private String HOME_DIR = ConfigUtils.getString( "gemma.appdata.home" );
    private ExpressionExperimentService expressionExperimentService;
    private Probe2ProbeCoexpressionService probe2ProbeCoexpressionService;
    
    /**
     * @return the probe2ProbeCoexpressionService
     */
    public Probe2ProbeCoexpressionService getProbe2ProbeCoexpressionService() {
        return probe2ProbeCoexpressionService;
    }
    /**
     * @param probe2ProbeCoexpressionService the probe2ProbeCoexpressionService to set
     */
    public void setProbe2ProbeCoexpressionService( Probe2ProbeCoexpressionService probe2ProbeCoexpressionService ) {
        this.probe2ProbeCoexpressionService = probe2ProbeCoexpressionService;
    }
    /**
     * @return the expressionExperimentService
     */
    public ExpressionExperimentService getExpressionExperimentService() {
        return expressionExperimentService;
    }
    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }
    
    /**
     * generates a collection of value objects that contain summary information about links, biomaterials, and datavectors
     */
    public void generateSummaryObjects() {
        initDirectories(true);
        // first, load all expression experiment value objects
        // this will have no stats filled in
        
        // for each expression experiment, load in stats
        Collection vos = expressionExperimentService.loadAllValueObjects();
        getStats( vos );
        
        // save the collection
        
        saveValueObjects(vos);
    }
    
    /**
     * generates a collection of value objects that contain summary information about links, biomaterials, and datavectors
     */
    public void generateSummaryObjects(Collection ids) {
        initDirectories(false);
        // first, load all expression experiment value objects
        // this will have no stats filled in
        
        // for each expression experiment, load in stats
        Collection vos = expressionExperimentService.loadValueObjects( ids );
        getStats( vos );
        
        // save the collection
        
        saveValueObjects(vos);
    }
    
    /**
     * generates a collection of value objects that contain summary information about links, biomaterials, and datavectors
     */
    public void generateSummaryObject(Long id) {
        Collection ids = new ArrayList<Long>();
        ids.add( id );
        
        generateSummaryObjects(ids);
    }
    
    
    private void getStats( Collection vos ) {
        String timestamp = DateFormatUtils.format( new Date( System.currentTimeMillis() ), "yyyy.MM.dd hh:mm" );
        for ( Object object : vos ) {
            ExpressionExperimentValueObject eeVo = (ExpressionExperimentValueObject) object;
            ExpressionExperiment tempEe =  expressionExperimentService.findById( Long.parseLong( eeVo.getId() ) );

            eeVo.setBioMaterialCount( expressionExperimentService.getBioMaterialCount( tempEe ) );
            eeVo.setPreferredDesignElementDataVectorCount( expressionExperimentService.getPreferredDesignElementDataVectorCount( tempEe ) );
            eeVo.setCoexpressionLinkCount( probe2ProbeCoexpressionService.countLinks( tempEe ).longValue() );
            eeVo.setDateCached( timestamp );
            if ( tempEe.getAuditTrail() != null ) {                
                eeVo.setDateCreated( tempEe.getAuditTrail().getCreationEvent().getDate().toString() );
            }
            eeVo.setDateLastUpdated( tempEe.getAuditTrail().getLast().getDate().toString() );
            
        }
    }
    

    
    /**
     * retrieves a collection of cached value objects containing summary information
     * @return a collection of cached value objects
     */
    public Collection retrieveSummaryObjects() {
        return retrieveValueObjects();
    }
    
    /**
     * retrieves a collection of cached value objects containing summary information
     * @return a collection of cached value objects
     */
    public Collection retrieveSummaryObjects(Collection ids) {
        return retrieveValueObjects(ids);
    }
    
    /**
     * @param eeValueObjects the collection of Expression Experiment value objects to serialize 
     * @return true if successful, false otherwise
     * serialize value objects
     */
    private boolean saveValueObjects(Collection eeValueObjects) {   
        for ( Object object : eeValueObjects ) {
            ExpressionExperimentValueObject eeVo = ( ExpressionExperimentValueObject ) object;

            try {
                // remove file first
                File f = new File(HOME_DIR + "/" + EE_REPORT_DIR + "/" + EE_LINK_SUMMARY
                        + "." + eeVo.getId());
                if (f.exists()){
                    f.delete();
                }
                FileOutputStream fos = new FileOutputStream( HOME_DIR + "/" + EE_REPORT_DIR + "/" + EE_LINK_SUMMARY
                        + "." + eeVo.getId() );
                ObjectOutputStream oos = new ObjectOutputStream( fos );
                oos.writeObject( eeVo );
                oos.flush();
                oos.close();
            } catch ( Throwable e ) {
                return false;
            }
        }
        return true;
    }
    
    
    /**
     * @return the filled out value objects
     * fills the link statistics from the cache. If it is not in the cache, the values will be null. 
     */
    public void fillLinkStatsFromCache( Collection vos ) {
        for ( Object object : vos ) {
            ExpressionExperimentValueObject eeVo = ( ExpressionExperimentValueObject ) object;
            ExpressionExperimentValueObject cacheVo = retrieveValueObject( Long.parseLong( eeVo.getId() ) );
            if ( cacheVo != null ) {
                eeVo.setBioMaterialCount( cacheVo.getBioMaterialCount() );
                eeVo.setPreferredDesignElementDataVectorCount( cacheVo.getPreferredDesignElementDataVectorCount() );
                eeVo.setCoexpressionLinkCount( cacheVo.getCoexpressionLinkCount() );
                eeVo.setDateCached( cacheVo.getDateCached() );
                eeVo.setDateCreated( cacheVo.getDateCreated() );
                eeVo.setDateLastUpdated( cacheVo.getDateLastUpdated() );
            }
        }
    }
    
    /**
     * @return the serialized value objects
     *
     */
    private Collection retrieveValueObjects() {
        Collection eeValueObjects = null;
        // load all files that start with EE_LINK_SUMMARY
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept( File dir, String name ) {
                return ( name.startsWith( EE_LINK_SUMMARY + "." ) );
            }
        };
        File fDir = new File( HOME_DIR + "/" + EE_REPORT_DIR );
        String[] filenames = fDir.list( filter );
        for ( String objectFile : filenames ) {
            try {
                FileInputStream fis = new FileInputStream( HOME_DIR + "/" + EE_REPORT_DIR + "/" + objectFile );
                ObjectInputStream ois = new ObjectInputStream( fis );
                eeValueObjects = ( Collection ) ois.readObject();
                ois.close();
                fis.close();
            } catch ( Throwable e ) {
                return null;
            }
        }
        return eeValueObjects;
    }
    
    /**
     * @return the serialized value objects
     *
     */
    private Collection retrieveValueObjects(Collection ids) {
        Collection eeValueObjects = new ArrayList<ExpressionExperiment>();

        for ( Object object : ids ) {
            Long id = ( Long ) object;

            try {
                File f = new File(HOME_DIR + "/" + EE_REPORT_DIR + "/" + EE_LINK_SUMMARY + "."
                        + id);
                if (f.exists()) {
                FileInputStream fis = new FileInputStream( HOME_DIR + "/" + EE_REPORT_DIR + "/" + EE_LINK_SUMMARY + "."
                        + id );
                ObjectInputStream ois = new ObjectInputStream( fis );
                eeValueObjects.add( ( ExpressionExperimentValueObject ) ois.readObject());
                ois.close();
                fis.close();
                }
                else {
                    continue;
                }
            } catch ( Throwable e ) {
                return null;
            }
        }
        return eeValueObjects;
    }
    
    /**
     * @return the serialized value object
     *
     */
    private ExpressionExperimentValueObject retrieveValueObject(long id) {

        ExpressionExperimentValueObject eeVo = null;
        try {
            File f = new File( HOME_DIR + "/" + EE_REPORT_DIR + "/" + EE_LINK_SUMMARY + "." + id );
            if ( f.exists() ) {
                FileInputStream fis = new FileInputStream( HOME_DIR + "/" + EE_REPORT_DIR + "/" + EE_LINK_SUMMARY + "."
                        + id );
                ObjectInputStream ois = new ObjectInputStream( fis );
                eeVo = ( ExpressionExperimentValueObject ) ois.readObject();
                ois.close();
                fis.close();
            }
        } catch ( Throwable e ) {
            return null;
        }
        return eeVo;
    }
    
    
    private void initDirectories(boolean deleteFiles) {
        // check to see if the home directory exists. If it doesn't, create it.
        // check to see if the reports directory exists. If it doesn't, create it.
        FileTools.createDir( HOME_DIR );
        FileTools.createDir( HOME_DIR + "/" + EE_REPORT_DIR );
        File f = new File(HOME_DIR + "/" + EE_REPORT_DIR);
        Collection<File> files = new ArrayList<File>();
        File[] fileArray = f.listFiles();
        for ( File file : fileArray ) {
            files.add( file );
        }
        // clear out all files
        if (deleteFiles) {
            FileTools.deleteFiles( files );
        }
    }
    
    
}

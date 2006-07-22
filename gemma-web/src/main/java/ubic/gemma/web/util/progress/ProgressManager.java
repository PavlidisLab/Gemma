/* Copyright (c) 2006 University of British Columbia
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



package ubic.gemma.web.util.progress;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ProgressManager {

    protected static final Log logger = LogFactory.getLog( ProgressManager.class );

    protected static Hashtable<String, Vector> progressJobs = new Hashtable<String, Vector>();
    protected static Hashtable<ProgressJob, Vector> notificationListByJob = new Hashtable<ProgressJob, Vector>();
    protected static Hashtable<String, Vector> notificationListByUser = new Hashtable<String, Vector>();

    public static ProgressJob CreateProgressJob( String userName, String description ) {

        Vector usersJobs;
        ProgressJob newJob;

        if ( !progressJobs.containsKey( userName ) ) progressJobs.put( userName, new Vector<ProgressJobImpl>() );

        usersJobs = progressJobs.get( userName );
        newJob = new ProgressJobImpl( userName, description );
        usersJobs.add( newJob );
        
        return newJob;

    }

    public static boolean addToNotification(String username, ProgressObserver po) {
            
            if (!notificationListByUser.containsKey(username))
                return false;
                        
            notificationListByUser.get(username).add(po);
            return true;
        }
    
    public static boolean addToNotification(ProgressJob pj, ProgressObserver po) {
            
            if (!notificationListByJob.containsKey(pj))
                return false;
                    
        notificationListByJob.get(pj).add(po);
            return true;
        }
    
    
//    static boolean Notify( String UserName ) {
//        
//        if (!notificationListByJob.containsKey(pj))
//            return false;
//        
//        for ( Iterator iter = notificationListByJob.get(UserName)).iterator(); iter.hasNext();) { 
//            ((ProgressObserver) iter.next()).progressUpdate());
//        
//        }
    
    static boolean Notify(ProgressJob pj ) {
        
        if (!notificationListByJob.containsKey(pj))
          return false;
      
      for ( Iterator iter = notificationListByJob.get(pj).iterator(); iter.hasNext();)
          ((ProgressObserver) iter.next()).progressUpdate(pj.getProgressData());
      
      return true;
      
    }
    

    static boolean DestroyProgressJob( ProgressJob ajob ) {
        
        if (notificationListByJob.containsKey(ajob))
            notificationListByJob.remove(ajob);
        
        if (notificationListByUser.containsKey(ajob.getUser()))
            notificationListByUser.remove(ajob.getUser());
                
        if (progressJobs.containsKey(ajob.getUser())){
            Vector jobs = progressJobs.get(ajob.getUser());
            jobs.remove(ajob);
        }        
        
        return true;
    }

    public ProgressManager() {

    }

}

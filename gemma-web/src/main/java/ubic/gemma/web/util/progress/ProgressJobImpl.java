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

/**
 * 
 *
 * <hr>
 * <p>Copyright (c) 2006 UBC Pavlab
 * @author klc
 * @version $Id$
 */
/**
 * 
 *
 * <hr>
 * <p>Copyright (c) 2006 UBC Pavlab
 * @author klc
 * @version $Id$
 */
/**
 * 
 *
 * <hr>
 * <p>Copyright (c) 2006 UBC Pavlab
 * @author klc
 * @version $Id$
 */
/**
 * 
 *
 * <hr>
 * <p>Copyright (c) 2006 UBC Pavlab
 * @author klc
 * @version $Id$
 */
public class ProgressJobImpl implements ProgressJob{

    public static final int DOWNLOAD_PROGRESS = 0;
    public static final int COMPUTATIONAL_PROGRESS = 1;
    public static final int DATABASE_PROGRESS = 2;
    public static final int PARSING_PROGRESS = 3;
    
    protected ProgressData pData;
    protected boolean runningStatus;
    protected int progressType;
    protected String failedMessage;
    protected String ownerId;

    
   //Make constructors default to limit creation to this package.
    ProgressJobImpl( String ownerId, String description) {
        this.ownerId = ownerId;
        this.pData = new ProgressData(0,description, false);
    }

     /**
     * @return Returns the pData.
     */
    public ProgressData getProgressData() {
        return pData;
    }

    /**
     * @param data The pData to set.
     */
    public void setProgressData( ProgressData data ) {
        pData = data;
    }


    /**
     * @return Returns the runningStatus.
     */
    public boolean isRunningStatus() {
        return runningStatus;
    }

    /**
     * @param runningStatus The runningStatus to set.
     */
    public void setRunningStatus( boolean runningStatus ) {
        this.runningStatus = runningStatus;
        if ( this.runningStatus && pData.isDone()  ) this.pData.setDone(false);
    }

    /**
     * @return Returns the progressType.
     */
    /* (non-Javadoc)
     * @see ubic.gemma.web.util.progress.ProgressJob#getProgressType()
     */
    public int getProgressType() {
        return progressType;
    }

    /**
     * @param progressType The progressType to set.
     */
    public void setProgressType( int progressType ) {
        this.progressType = progressType;
    }
    
    
  public String getUser() {
       
       return this.ownerId;
   
   }
   
   

}

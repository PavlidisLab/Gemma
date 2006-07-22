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

public interface ProgressJob {

    /**
     * @return Returns the pData.
     */
    public abstract ProgressData getProgressData();

    /**
     * @param data The pData to set.
     */
    public abstract void setProgressData( ProgressData data );

    /**
     * @return Returns the runningStatus.
     */
    public abstract boolean isRunningStatus();

    /**
     * @param runningStatus The runningStatus to set.
     */
    public abstract void setRunningStatus( boolean runningStatus );

    /**
     * @return Returns the progressType.
     */
    public abstract int getProgressType();

    /**
     * @param progressType The progressType to set.
     */
    public abstract void setProgressType( int progressType );
    
    
    public abstract String getUser();

}
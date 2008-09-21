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
package ubic.gemma.grid.javaspaces;

import org.springframework.context.ApplicationContext;

import ubic.gemma.util.grid.javaspaces.SpacesUtil;

/**
 * A helper class containing methods that cannot be accessed from gemma-util. All utility methods exist in
 * {@link SpacesUtil}.
 * 
 * @author keshav
 * @version $Id$
 */
public class SpacesHelper {

    /**
     * @param updatedContext ApplicationContext that has previously been updated to include gigaspaces beans.
     * @param taskName The task name.
     * @return
     */
    public static String getTaskIdFromTask( ApplicationContext updatedContext, String taskName ) {
        if ( !updatedContext.containsBean( "gigaspacesTemplate" ) ) {
            throw new IllegalArgumentException(
                    "Incorrect usage.  ApplicationContext must contain \"spaces\" beans.  Update the context to contain these beans before invoking." );
        }

        String[] customDelegatingWorkerBeanNames = updatedContext.getBeanNamesForType( CustomDelegatingWorker.class );

        for ( String customDelegatingWorkerBeanName : customDelegatingWorkerBeanNames ) {
            CustomDelegatingWorker customDelegatingWorker = ( CustomDelegatingWorker ) updatedContext
                    .getBean( customDelegatingWorkerBeanName );

            Class businessInterface = customDelegatingWorker.getBusinessInterface();
            if ( taskName == businessInterface.getName() ) {
                SpacesTask task = ( SpacesTask ) customDelegatingWorker.getDelegate();
                return task.getTaskId();
            }
        }
        return null;
    }

}

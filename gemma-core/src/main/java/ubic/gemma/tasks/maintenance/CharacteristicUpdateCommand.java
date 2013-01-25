/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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

package ubic.gemma.tasks.maintenance;

import java.util.Collection;

import ubic.gemma.job.TaskCommand;
import ubic.gemma.model.common.description.AnnotationValueObject;

/**
 * @author paul
 * @version $Id$
 */
public class CharacteristicUpdateCommand extends TaskCommand {
 
    private static final long serialVersionUID = 1L;

    private Collection<AnnotationValueObject> annotationValueObjects;

    /**
     * If set to true, the annotations passed in will be deleted.
     */
    private boolean remove = false;

    /**
     * @param annotationValueObjects the annotationValueObjects to set
     */
    public void setAnnotationValueObjects( Collection<AnnotationValueObject> annotationValueObjects ) {
        this.annotationValueObjects = annotationValueObjects;
    }

    /**
     * @return the annotationValueObjects
     */
    public Collection<AnnotationValueObject> getAnnotationValueObjects() {
        return annotationValueObjects;
    }

    /**
     * @param remove the remove to set
     */
    public void setRemove( boolean remove ) {
        this.remove = remove;
    }

    /**
     * @return the remove
     */
    public boolean isRemove() {
        return remove;
    }

    @Override
    public Class getTaskClass() {
        return CharacteristicUpdateTask.class;
    }
}

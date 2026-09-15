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

package ubic.gemma.core.tasks.maintenance;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.Task;
import ubic.gemma.model.common.description.AnnotationValueObject;

import java.util.Collection;

/**
 * @author paul
 */
@Getter
@Setter
public class CharacteristicUpdateCommand extends TaskCommand {

    private static final long serialVersionUID = 1L;

    private Collection<AnnotationValueObject> annotationValueObjects;

    /**
     * If set to true, the annotations passed in will be deleted.
     */
    private boolean remove = false;

    @Override
    public Class<? extends Task<? extends TaskCommand>> getTaskClass() {
        return CharacteristicUpdateTask.class;
    }
}

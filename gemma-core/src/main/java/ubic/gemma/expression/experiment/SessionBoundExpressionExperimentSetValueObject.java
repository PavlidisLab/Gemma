/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */
/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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

package ubic.gemma.expression.experiment;

import java.util.Collection;

import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.session.GemmaSessionBackedValueObject;

/**
 * TODO Document Me
 * 
 * @author tvrossum
 * @version $Id$
 */
public class SessionBoundExpressionExperimentSetValueObject extends ExpressionExperimentSetValueObject implements
        GemmaSessionBackedValueObject {

    private static final long serialVersionUID = 2068650886972222818L;
    private boolean modified;

    /**
     * default constructor to satisfy java bean contract
     */
    public SessionBoundExpressionExperimentSetValueObject() {
        super();
    }

    /*
     * can't implement this because sessionListManager in is 'web' package
     * 
     * @Override public Object loadEntity() { return sessionListManager.getExperimentSetById( this.getId() ); }
     */
    @Override
    public boolean isModified() {
        return this.modified;
    }

    @Override
    public void setModified( boolean modified ) {
        this.modified = modified;
    }

    /**
     * two value objects are equal if their types are the same and their ids are the same
     */
    @Override
    public boolean equals( GemmaSessionBackedValueObject ervo ) {
        if ( ervo.getClass().equals( this.getClass() ) && ervo.getId().equals( this.getId() ) ) {
            return true;
        }
        return false;
    }

    @Override
    public Collection<Long> getMemberIds() {
        return this.getExpressionExperimentIds();
    }

}

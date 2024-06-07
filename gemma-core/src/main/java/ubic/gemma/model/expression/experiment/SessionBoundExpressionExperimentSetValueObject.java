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

package ubic.gemma.model.expression.experiment;

import ubic.gemma.model.common.GemmaSessionBackedValueObject;

/**
 * @author tvrossum
 */
public class SessionBoundExpressionExperimentSetValueObject extends ExpressionExperimentSetValueObject
        implements GemmaSessionBackedValueObject {

    private static final long serialVersionUID = 2068650886972222818L;
    private boolean modified;

    /**
     * Required when using the class as a spring bean.
     */
    @SuppressWarnings("WeakerAccess") // Required by Spring
    public SessionBoundExpressionExperimentSetValueObject() {
        super();
    }

    /**
     * default constructor to satisfy java bean contract
     *
     * @param id id
     */
    @SuppressWarnings("WeakerAccess")
    public SessionBoundExpressionExperimentSetValueObject( Long id ) {
        super( id );
    }

    @Override
    public boolean isModified() {
        return this.modified;
    }

    @Override
    public void setModified( boolean modified ) {
        this.modified = modified;
    }

}

/*
 * The gemma-mda project
 * 
 * Copyright (c) 2014 University of British Columbia
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

package ubic.gemma.web.controller.expression.experiment;

import java.util.Collection;

import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

/**
 * TODO Document Me
 * 
 * @author paul
 * @version $Id$
 */
public class ExpressionExperimentEditValueObject extends ExpressionExperimentDetailsValueObject {

    /**
     * 
     */
    private static final long serialVersionUID = 1630521876359566915L;
    private Collection<QuantitationTypeValueObject> quantitationTypes;

    /**
     * @param eevo
     */
    public ExpressionExperimentEditValueObject( ExpressionExperimentValueObject eevo ) {
        super( eevo );
    }

    /**
     * 
     */
    public ExpressionExperimentEditValueObject() {
        super();
    }

    /**
     * @return
     */
    public Collection<QuantitationTypeValueObject> getQuantitationTypes() {
        return this.quantitationTypes;
    }

    /**
     * @param qts
     */
    public void setQuantitationTypes( Collection<QuantitationTypeValueObject> qts ) {
        this.quantitationTypes = qts;
    }

    private Collection<BioAssayValueObject> bioAssays;

    public Collection<BioAssayValueObject> getBioAssays() {
        return bioAssays;
    }

    public void setBioAssays( Collection<BioAssayValueObject> bioAssays ) {
        this.bioAssays = bioAssays;
    }

}

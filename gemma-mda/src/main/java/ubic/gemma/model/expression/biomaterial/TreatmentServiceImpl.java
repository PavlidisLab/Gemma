/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.expression.biomaterial;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.biomaterial.TreatmentService
 */
@Service
public class TreatmentServiceImpl extends ubic.gemma.model.expression.biomaterial.TreatmentServiceBase {

    /**
     * @see ubic.gemma.model.expression.biomaterial.TreatmentService#getTreatments()
     */
    @Override 
    protected java.util.List handleGetTreatments() throws java.lang.Exception {
        List<Treatment> result = new ArrayList<Treatment>();
        result.addAll( this.getTreatmentDao().loadAll() );
        return result;
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.TreatmentService#saveTreatment(ubic.gemma.model.expression.biomaterial.Treatment)
     */
    @Override
    protected Treatment handleSaveTreatment( ubic.gemma.model.expression.biomaterial.Treatment treatment )
            throws java.lang.Exception {
        return this.getTreatmentDao().create( treatment );
    }

}
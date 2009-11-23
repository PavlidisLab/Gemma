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

package ubic.gemma.web.taglib.displaytag;

import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

/**
 * @author klc
 *
 *  compares 2 ExpressionExperimentValueObjects by the date they where created
 */
public class ExpressionExperimentValueObjectComparator extends DateStringComparator {

    
    @Override
    public int compare( Object arg0, Object arg1 ) {
        
       ExpressionExperimentValueObject eevo0 = (ExpressionExperimentValueObject) arg0;
       ExpressionExperimentValueObject eevo1 = (ExpressionExperimentValueObject) arg1;
       
       return super.compare( eevo1.getDateCreated(), eevo0.getDateCreated() );
    }
    
}

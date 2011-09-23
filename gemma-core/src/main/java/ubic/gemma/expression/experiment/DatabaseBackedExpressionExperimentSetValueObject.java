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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetService;

/**
 * TODO Document Me
 * 
 * @author tvrossum
 * @version $Id$
 */

public class DatabaseBackedExpressionExperimentSetValueObject extends ExpressionExperimentSetValueObject {

    private static final long serialVersionUID = -1075910242491981481L;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;
    
    public static Collection<DatabaseBackedExpressionExperimentSetValueObject> makeValueObjects(
            Collection<ExpressionExperimentSet> entities ) {
        Collection<DatabaseBackedExpressionExperimentSetValueObject> results = new HashSet<DatabaseBackedExpressionExperimentSetValueObject>();

        for ( ExpressionExperimentSet eeset : entities ) {
            results.add( new DatabaseBackedExpressionExperimentSetValueObject( eeset ) );
        }

        return results;
    }
    /**
     * default constructor to satisfy java bean contract
     */
    public DatabaseBackedExpressionExperimentSetValueObject() {
        super();
    }
    
    public DatabaseBackedExpressionExperimentSetValueObject(ExpressionExperimentSet eeset) {
        super(eeset);
    }

   //@Override
    public Object loadEntity() {
        return expressionExperimentSetService.load( this.getId() );
    }

    
    public String testMethod(){
        System.out.println("ran method in DatabaseBackedExpressionExperimentSetValueObject");
        return "Tested.";
    }

    public static DatabaseBackedExpressionExperimentSetValueObject factoryMethod() {
        return new DatabaseBackedExpressionExperimentSetValueObject();
    }
    
    public static Collection<? extends ExpressionExperimentSetValueObject> convert2ValueObjects(
            Collection<ExpressionExperimentSet> eesets, boolean includeOnesWithoutExperiments ) {
        List<DatabaseBackedExpressionExperimentSetValueObject> results = new ArrayList<DatabaseBackedExpressionExperimentSetValueObject>();

        for ( ExpressionExperimentSet es : eesets ) {
            if ( !includeOnesWithoutExperiments && es.getExperiments().isEmpty() ) {
                continue;
            }

            if ( es.getId() == null ) {
                /*
                 * GO terms, for example. We need a unique ID that also is different from IDs of things in the database.
                 * This isn't an entirely satisfactory implementation, it should be made bulletproof.
                 */
                es.setId( Long.parseLong( RandomStringUtils.randomNumeric( 16 ) ) + 100000L );
            }

            results.add( new DatabaseBackedExpressionExperimentSetValueObject( es ) );
        }

        Collections.sort( results, new Comparator<DatabaseBackedExpressionExperimentSetValueObject>() {
            @Override
            public int compare( DatabaseBackedExpressionExperimentSetValueObject o1, DatabaseBackedExpressionExperimentSetValueObject o2 ) {
                return -o1.getNumExperiments().compareTo( o2.getNumExperiments() );
            }
        } );
        return results;
    }
    
}

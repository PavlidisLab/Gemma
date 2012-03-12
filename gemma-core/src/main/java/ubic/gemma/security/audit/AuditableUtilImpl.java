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
package ubic.gemma.security.audit;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.util.EntityUtils;

/**
 * A few utility methods to filter collections
 * 
 * @author paul
 * @version $Id$
 */
@Component
public class AuditableUtilImpl implements AuditableUtil {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.audit.AuditableUtil#removeTroubledArrayDesigns(java.util.Collection)
     */
    @Override
    public void removeTroubledArrayDesigns( Collection<ArrayDesignValueObject> valueObjects ) {
        if ( valueObjects == null || valueObjects.size() == 0 ) {
            return;
        }

        Collection<Long> ids = new HashSet<Long>();
        for ( ArrayDesignValueObject advo : valueObjects ) {
            ids.add( advo.getId() );
        }

        CollectionUtils.filter( valueObjects, new Predicate() {
            @Override
            public boolean evaluate( Object vo ) {
                return !( ( ArrayDesignValueObject ) vo ).getTroubled();
            }
        } );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.audit.AuditableUtil#removeTroubledEes(java.util.Collection)
     */
    @Override
    public void removeTroubledEes( Collection<ExpressionExperimentValueObject> eevos ) {
        if ( eevos == null || eevos.size() == 0 ) {
            return;
        }

        final Collection<Long> untroubled = expressionExperimentService.getUntroubled( EntityUtils.getIds( eevos ) );

        CollectionUtils.filter( eevos, new Predicate() {
            @Override
            public boolean evaluate( Object e ) {
                boolean ok = untroubled.contains( ( ( ExpressionExperimentValueObject ) e ).getId() );
                return ok;
            }
        } );
    }

}

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
package ubic.gemma.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

/**
 * A few utility methods to filter collections
 * 
 * @spring.bean id="auditableUtil"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "arrayDesignService" ref="arrayDesignService"
 * @author paul
 * @version $Id$
 */
public class AuditableUtil {
    ArrayDesignService arrayDesignService;

    /**
     * @param arrayDesignService the arrayDesignService to set
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    ExpressionExperimentService expressionExperimentService;

    /**
     * @param valueObjects
     */
    public void removeTroubledArrayDesigns( Collection<ArrayDesignValueObject> valueObjects ) {
        if ( valueObjects == null || valueObjects.size() == 0 ) {
            return;
        }

        Collection<Long> ids = new HashSet<Long>();
        for ( ArrayDesignValueObject advo : valueObjects ) {
            ids.add( advo.getId() );
        }

        int size = valueObjects.size();
        final Map<Long, AuditEvent> trouble = arrayDesignService.getLastTroubleEvent( ids );

        CollectionUtils.filter( valueObjects, new Predicate() {
            public boolean evaluate( Object vo ) {
                boolean hasTrouble = trouble.get( ( ( ArrayDesignValueObject ) vo ).getId() ) != null;
                return !hasTrouble;
            }
        } );
        int newSize = valueObjects.size();
        if ( newSize != size ) {
            assert newSize < size;
        }
    }

    /**
     * @param ees
     */
    @SuppressWarnings("unchecked")
    public void removeTroubledEes( Collection<ExpressionExperimentValueObject> eevos ) {
        if ( eevos == null || eevos.size() == 0 ) {
            return;
        }

        Collection<Long> ees = new HashSet<Long>();
        for ( ExpressionExperimentValueObject eevo : eevos ) {
            ees.add( eevo.getId() );
        }

        int size = ees.size();
        final Map<Long, AuditEvent> trouble = expressionExperimentService.getLastTroubleEvent( ees );
        CollectionUtils.filter( eevos, new Predicate() {
            public boolean evaluate( Object e ) {
                boolean hasTrouble = trouble.get( ( ( ExpressionExperimentValueObject ) e ).getId() ) != null;
                return !hasTrouble;
            }
        } );
        int newSize = ees.size();
        if ( newSize != size ) {
            assert newSize < size;
        }
    }

}

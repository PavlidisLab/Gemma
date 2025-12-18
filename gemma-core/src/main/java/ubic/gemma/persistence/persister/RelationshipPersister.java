/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.persistence.persister;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.ExpressionExperimentSetDao;
import ubic.gemma.persistence.service.analysis.expression.coexpression.CoexpressionAnalysisDao;
import ubic.gemma.persistence.service.association.Gene2GOAssociationDao;

import java.util.Collection;
import java.util.HashSet;

/**
 * Persist objects like Gene2GOAssociation.
 *
 * @author pavlidis
 */
public abstract class RelationshipPersister extends ExpressionPersister {

    @Autowired
    private Gene2GOAssociationDao gene2GoAssociationDao;

    @Autowired
    private CoexpressionAnalysisDao coexpressionAnalysisDao;

    @Autowired
    private ExpressionExperimentSetDao expressionExperimentSetDao;

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Identifiable> T doPersist( T entity, Caches caches ) {
        if ( entity instanceof Gene2GOAssociation ) {
            return ( T ) this.persistGene2GOAssociation( ( Gene2GOAssociation ) entity, caches );
        } else if ( entity instanceof CoexpressionAnalysis ) {
            return ( T ) this.persistCoexpressionAnalysis( ( CoexpressionAnalysis ) entity );
        } else if ( entity instanceof ExpressionExperimentSet ) {
            return ( T ) this.persistExpressionExperimentSet( ( ExpressionExperimentSet ) entity, caches );
        } else {
            return super.doPersist( entity, caches );
        }
    }

    private ExpressionExperimentSet persistExpressionExperimentSet( ExpressionExperimentSet entity, Caches caches ) {
        Collection<ExpressionExperiment> setMembers = new HashSet<>();

        for ( ExpressionExperiment baSet : entity.getExperiments() ) {
            if ( baSet.getId() == null ) {
                baSet = this.doPersist( baSet, caches );
            }
            setMembers.add( baSet );
        }
        entity.getExperiments().clear();
        entity.getExperiments().addAll( setMembers );

        return expressionExperimentSetDao.create( entity );
    }

    private Gene2GOAssociation persistGene2GOAssociation( Gene2GOAssociation association, Caches caches ) {
        try {
            FieldUtils.writeField( association, "gene", this.persistGene( association.getGene(), caches ), true );
        } catch ( IllegalAccessException e ) {
            throw new RuntimeException( e );
        }
        return gene2GoAssociationDao.create( association );
    }

    private CoexpressionAnalysis persistCoexpressionAnalysis( CoexpressionAnalysis entity ) {
        if ( entity.getProtocol() != null ) {
            entity.setProtocol( this.persistProtocol( entity.getProtocol() ) );
        }
        if ( entity.getExperimentAnalyzed().getId() == null ) {
            throw new IllegalArgumentException( "Persist the experiment before running analyses on it" );
        }
        return coexpressionAnalysisDao.create( entity );
    }

}

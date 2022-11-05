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
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.service.analysis.expression.ExpressionExperimentSetDao;
import ubic.gemma.persistence.service.analysis.expression.coexpression.CoexpressionAnalysisDao;
import ubic.gemma.persistence.service.association.Gene2GOAssociationDao;

import javax.persistence.PersistenceUnitUtil;
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
    @Transactional
    public Object persist( Object entity ) {
        if ( entity == null )
            return null;

        if ( entity instanceof Gene2GOAssociation ) {
            return this.persistGene2GOAssociation( ( Gene2GOAssociation ) entity );
        } else if ( entity instanceof CoexpressionAnalysis ) {
            return this.persistCoexpressionAnalysis( ( CoexpressionAnalysis ) entity );
        } else if ( entity instanceof ExpressionExperimentSet ) {
            return this.persistExpressionExperimentSet( ( ExpressionExperimentSet ) entity );
        }
        return super.persist( entity );

    }

    @Override
    @Transactional
    public Object persistOrUpdate( Object entity ) {
        if ( entity == null )
            return null;
        return super.persistOrUpdate( entity );
    }

    private ExpressionExperimentSet persistExpressionExperimentSet( ExpressionExperimentSet entity ) {
        if ( !this.isTransient( entity ) )
            return entity;

        Collection<BioAssaySet> setMembers = new HashSet<>();

        for ( BioAssaySet baSet : entity.getExperiments() ) {
            if ( this.isTransient( baSet ) ) {
                baSet = ( BioAssaySet ) this.persist( baSet );
            }
            setMembers.add( baSet );
        }
        entity.getExperiments().clear();
        entity.getExperiments().addAll( setMembers );

        expressionExperimentSetDao.create( entity );

        return entity;
    }

    private Gene2GOAssociation persistGene2GOAssociation( Gene2GOAssociation association ) {
        if ( association == null )
            return null;
        if ( !this.isTransient( association ) )
            return association;
        try {
            FieldUtils.writeField( association, "gene", this.persistGene( association.getGene() ), true );
        } catch ( IllegalAccessException e ) {
            e.printStackTrace();
        }
        gene2GoAssociationDao.create( association );
        return association;
    }

    private CoexpressionAnalysis persistCoexpressionAnalysis( CoexpressionAnalysis entity ) {
        if ( entity == null )
            return null;
        if ( !this.isTransient( entity ) )
            return entity;
        entity.setProtocol( this.persistProtocol( entity.getProtocol() ) );
        if ( this.isTransient( entity.getExperimentAnalyzed() ) ) {
            throw new IllegalArgumentException( "Persist the experiment before running analyses on it" );
        }

        coexpressionAnalysisDao.create( entity );
        return entity;
    }

}

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
package ubic.gemma.persistence.service.analysis.expression.pca;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis;
import ubic.gemma.model.analysis.expression.pca.ProbeLoading;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * @author paul
 */
@Repository
public class PrincipalComponentAnalysisDaoImpl extends AbstractDao<PrincipalComponentAnalysis>
        implements PrincipalComponentAnalysisDao {

    @Autowired
    public PrincipalComponentAnalysisDaoImpl( SessionFactory sessionFactory ) {
        super( PrincipalComponentAnalysis.class, sessionFactory );
    }

    @Override
    public Collection<PrincipalComponentAnalysis> findByExperiment( ExpressionExperiment ee ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select p from PrincipalComponentAnalysis as p fetch all properties where p.experimentAnalyzed = :ee" )
                .setParameter( "ee", ee ).list();
    }

    @Override
    public List<ProbeLoading> getTopLoadedProbes( ExpressionExperiment ee, int component, int count ) {
        if ( ee == null || ee.getId() == null )
            return Collections.emptyList();

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( "select pr from PrincipalComponentAnalysis p join p.probeLoadings pr"
                + " where p.experimentAnalyzed = :ee and pr.componentNumber = :cmp order by pr.loadingRank " )
                .setParameter( "ee", ee ).setParameter( "cmp", component ).setMaxResults( count ).list();
    }

    @Override
    public void removeForExperiment( ExpressionExperiment ee ) {
        this.remove( this.findByProperty( "experimentAnalyzed", ee ) );
    }

    @Override
    public void remove( PrincipalComponentAnalysis entity ) {
        // detach the entity because we're going to do some manual removal
        getSessionFactory().getCurrentSession().evict( entity );

        getSessionFactory().getCurrentSession()
                .createSQLQuery( "delete ev from EIGENVALUE ev where ev.PRINCIPAL_COMPONENT_ANALYSIS_FK = :id" )
                .setParameter( "id", entity.getId() )
                .executeUpdate();
        entity.setEigenValues( new HashSet<>() );

        getSessionFactory().getCurrentSession()
                .createSQLQuery( "delete ev from EIGENVECTOR ev where ev.PRINCIPAL_COMPONENT_ANALYSIS_FK = :id" )
                .setParameter( "id", entity.getId() )
                .executeUpdate();
        entity.setEigenVectors( new HashSet<>() );

        getSessionFactory().getCurrentSession()
                .createSQLQuery( "delete pl from PROBE_LOADING pl where pl.PRINCIPAL_COMPONENT_ANALYSIS_FK = :id" )
                .setParameter( "id", entity.getId() )
                .executeUpdate();
        entity.setProbeLoadings( new HashSet<>() );

        super.remove( entity );
    }
}

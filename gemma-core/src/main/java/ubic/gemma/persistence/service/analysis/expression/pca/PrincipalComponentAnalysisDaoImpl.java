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
        // Join-fetch the BAD assays + per-assay sampleUsed proxy to close the cold-path N+1 in
        // SVDResult.samplesFromPca (line 132, BioAssay::getSampleUsed). BAD itself is already
        // join-fetched via PCA's eager mapping; eigenValues/eigenVectors stay on their separate
        // read-only L2 cached selects so no MultipleBagFetchException risk. distinct collapses
        // the N-row cartesian (1 PCA × #BAs) back to one PCA. See RECCE_PCA_SVD_NPLUS1.md #3.
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select distinct p from PrincipalComponentAnalysis as p "
                        + "join fetch p.bioAssayDimension bad "
                        + "left join fetch bad.bioAssays b "
                        + "left join fetch b.sampleUsed "
                        + "where p.experimentAnalyzed = :ee" )
                .setParameter( "ee", ee ).list();
    }

    @Override
    public boolean existsByExperiment( ExpressionExperiment ee ) {
        return ( Boolean ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select count(*) > 0 from PrincipalComponentAnalysis as p where p.experimentAnalyzed = :ee" )
                .setParameter( "ee", ee )
                .uniqueResult();
    }

    @Override
    public List<ProbeLoading> getTopLoadedProbes( ExpressionExperiment ee, int component, int count ) {
        if ( ee == null || ee.getId() == null )
            return Collections.emptyList();

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( "select pr from PrincipalComponentAnalysis p join p.probeLoadings pr"
                + " where p.experimentAnalyzed = :ee and pr.componentNumber = :cmp order by pr.loadingRank " )
                .setParameter( "ee", ee ).setParameter( "cmp", component )
                // HB6 rejects setMaxResults(<0); treat <=0 as "no limit".
                .setMaxResults( count > 0 ? count : Integer.MAX_VALUE ).list();
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
                .createNativeQuery( "delete ev from EIGENVALUE ev where ev.PRINCIPAL_COMPONENT_ANALYSIS_FK = :id" )
                .setParameter( "id", entity.getId() )
                .executeUpdate();
        entity.setEigenValues( new HashSet<>() );

        getSessionFactory().getCurrentSession()
                .createNativeQuery( "delete ev from EIGENVECTOR ev where ev.PRINCIPAL_COMPONENT_ANALYSIS_FK = :id" )
                .setParameter( "id", entity.getId() )
                .executeUpdate();
        entity.setEigenVectors( new HashSet<>() );

        getSessionFactory().getCurrentSession()
                .createNativeQuery( "delete pl from PROBE_LOADING pl where pl.PRINCIPAL_COMPONENT_ANALYSIS_FK = :id" )
                .setParameter( "id", entity.getId() )
                .executeUpdate();
        entity.setProbeLoadings( new HashSet<>() );

        super.remove( entity );
    }
}

/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.analysis;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import ubic.gemma.model.analysis.SingleExperimentAnalysis;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type {@link SingleExperimentAnalysis}.
 * <p>
 * The single experiment analysis has a related {@link BioAssaySet} which is typically either a {@link ubic.gemma.model.expression.experiment.ExpressionExperiment}
 * or a {@link ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet}.
 *
 * @see SingleExperimentAnalysis
 */
public abstract class SingleExperimentAnalysisDaoBase<T extends SingleExperimentAnalysis> extends AbstractDao<T> implements SingleExperimentAnalysisDao<T> {

    private final Class<T> analysisClass;

    protected SingleExperimentAnalysisDaoBase( Class<T> elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
        this.analysisClass = elementClass;
    }

    @Override
    public Collection<T> findByExperiment( final BioAssaySet experiment ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createCriteria( this.analysisClass )
                .add( Restrictions.eq( "experimentAnalyzed", experiment ) )
                .list();
    }

    @Override
    public Map<BioAssaySet, Collection<T>> findByExperiments( final Collection<? extends BioAssaySet> experiments ) {
        //noinspection unchecked
        List<T> results = ( List<T> ) this.getSessionFactory().getCurrentSession().createCriteria( this.analysisClass )
                .add( Restrictions.in( "experimentAnalyzed", experiments ) )
                .list();
        return results.stream().collect( Collectors.groupingBy( SingleExperimentAnalysis::getExperimentAnalyzed, Collectors.toCollection( ArrayList::new ) ) );
    }

    @Override
    public Collection<T> findByTaxon( final Taxon taxon ) {
        //noinspection unchecked
        return ( List<T> ) this.getSessionFactory().getCurrentSession().createCriteria( this.analysisClass )
                .createAlias( "experimentAnalyzed", "ee" )
                .createAlias( "ee.bioAssays", "ba" )
                .createAlias( "ba.sampleUsed", "sample" )
                .add( Restrictions.eq( "sample.sourceTaxon", taxon ) )
                .list();
    }

    @Override
    public Collection<T> findByName( String name ) {
        return this.findByProperty( "name", name );
    }
}
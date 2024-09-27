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
package ubic.gemma.persistence.service.analysis.expression.diff;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.math.distribution.Histogram;
import ubic.gemma.core.tasks.analysis.diffex.DifferentialExpressionAnalysisTask;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.*;

import static ubic.gemma.persistence.service.expression.biomaterial.BioMaterialUtils.visitBioMaterials;

/**
 * @author paul
 * @author keshav
 * @see DifferentialExpressionAnalysisService
 */
@Service
public class DifferentialExpressionAnalysisServiceImpl extends AbstractService<DifferentialExpressionAnalysis> implements DifferentialExpressionAnalysisService {

    private static final Log log = LogFactory.getLog( DifferentialExpressionAnalysisTask.class.getName() );

    private final DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;

    @Autowired
    private ExpressionAnalysisResultSetDao expressionAnalysisResultSetDao;
    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;
    @Autowired
    private GeneDiffExMetaAnalysisDao geneDiffExMetaAnalysisDao;

    @Autowired
    public DifferentialExpressionAnalysisServiceImpl( DifferentialExpressionAnalysisDao mainDao ) {
        super( mainDao );
        this.differentialExpressionAnalysisDao = mainDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> find( Gene gene, ExpressionAnalysisResultSet resultSet,
            double threshold ) {
        return this.differentialExpressionAnalysisDao.find( gene, resultSet, threshold );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef ) {
        return this.differentialExpressionAnalysisDao.findByFactor( ef );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Collection<DifferentialExpressionAnalysis>> findByExperimentIds(
            Collection<Long> experimentIds ) {
        return this.differentialExpressionAnalysisDao.findByExperimentIds( experimentIds );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> findByTaxon( Taxon taxon ) {
        return this.differentialExpressionAnalysisDao.findByTaxon( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssaySet> findExperimentsWithAnalyses( Gene gene ) {
        return this.differentialExpressionAnalysisDao.findExperimentsWithAnalyses( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> getAnalyses( BioAssaySet expressionExperiment ) {
        return this.differentialExpressionAnalysisDao.findByExperiment( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> getAnalyses(
            Collection<? extends BioAssaySet> expressionExperiments ) {
        return this.differentialExpressionAnalysisDao.getAnalyses( expressionExperiments );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> thaw( Collection<DifferentialExpressionAnalysis> expressionAnalyses ) {
        HashSet<DifferentialExpressionAnalysis> results = new HashSet<>();
        for ( DifferentialExpressionAnalysis ea : expressionAnalyses ) {
            results.add( this.thaw( ea ) );
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public DifferentialExpressionAnalysis thaw( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        StopWatch timer = new StopWatch();
        timer.start();

        differentialExpressionAnalysis = ensureInSession( differentialExpressionAnalysis );

        Hibernate.initialize( differentialExpressionAnalysis );
        Hibernate.initialize( differentialExpressionAnalysis.getExperimentAnalyzed() );
        Hibernate.initialize( differentialExpressionAnalysis.getExperimentAnalyzed().getBioAssays() );
        for ( BioAssay bm : differentialExpressionAnalysis.getExperimentAnalyzed().getBioAssays() ) {
            visitBioMaterials( bm.getSampleUsed(), b -> {
                for ( FactorValue fv : b.getFactorValues() ) {
                    Hibernate.initialize( fv.getExperimentalFactor() );
                }
            } );
        }

        Hibernate.initialize( differentialExpressionAnalysis.getProtocol() );

        if ( differentialExpressionAnalysis.getSubsetFactorValue() != null ) {
            Hibernate.initialize( differentialExpressionAnalysis.getSubsetFactorValue() );
        }

        Collection<ExpressionAnalysisResultSet> ears = differentialExpressionAnalysis.getResultSets();
        Hibernate.initialize( ears );
        for ( ExpressionAnalysisResultSet ear : ears ) {
            Hibernate.initialize( ear );
            Hibernate.initialize( ear.getExperimentalFactors() );
        }
        if ( timer.getTime() > 1000 ) {
            log.info( "Thaw: " + timer.getTime() + "ms" );
        }

        return differentialExpressionAnalysis;
    }

    @Override
    @Transactional(readOnly = true)
    public DifferentialExpressionAnalysis thawFully( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        differentialExpressionAnalysis = thaw( differentialExpressionAnalysis );
        // just loading the entities in the session is sufficient for thawing them
        for ( ExpressionAnalysisResultSet dears : differentialExpressionAnalysis.getResultSets() ) {
            expressionAnalysisResultSetDao.loadWithResultsAndContrasts( dears.getId() );
        }
        return differentialExpressionAnalysis;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canDelete( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        return this.expressionAnalysisResultSetDao.canDelete( differentialExpressionAnalysis );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperimentDetailsValueObject, List<DifferentialExpressionAnalysisValueObject>> getAnalysesByExperiment(
            Collection<Long> ids ) {
        return this.getAnalysesByExperiment( ids, 0, -1 );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperimentDetailsValueObject, List<DifferentialExpressionAnalysisValueObject>> getAnalysesByExperiment(
            Collection<Long> ids, int offset, int limit ) {
        Map<Long, List<DifferentialExpressionAnalysisValueObject>> analysesByExperimentIds = this.differentialExpressionAnalysisDao
                .getAnalysesByExperimentIds( ids, offset, limit );

        Map<Long, ExpressionExperimentDetailsValueObject> idMap = EntityUtils.getIdMap( expressionExperimentDao
                .loadDetailsValueObjectsByIds( analysesByExperimentIds.keySet() ) );

        Map<ExpressionExperimentDetailsValueObject, List<DifferentialExpressionAnalysisValueObject>> result = new HashMap<>();

        for ( Long id : analysesByExperimentIds.keySet() ) {
            if ( !idMap.containsKey( id ) )
                continue; // defensive....
            result.put( idMap.get( id ), analysesByExperimentIds.get( id ) );
        }
        return result;
    }

    @Override
    @Transactional
    public void remove( DifferentialExpressionAnalysis toDelete ) {
        toDelete = ensureInSession( toDelete );

        log.info( "Removing " + toDelete + "..." );

        // Remove meta analyses that use the analyzed experiment
        Collection<GeneDifferentialExpressionMetaAnalysis> metas = this.geneDiffExMetaAnalysisDao
                .findByExperiment( toDelete.getExperimentAnalyzed() );
        if ( !metas.isEmpty() ) {
            log.info( "Removing " + metas.size() + " meta analyses with this experiment..." );
            geneDiffExMetaAnalysisDao.remove( metas );
        }

        // Remove the DEA
        super.remove( toDelete );
    }

    @Override
    public void remove( Collection<DifferentialExpressionAnalysis> entities ) {
        entities.forEach( this::remove );
    }

    @Override
    public void remove( Long id ) {
        throw new UnsupportedOperationException( "Removing an analysis by ID is not supported, use remove() with an entity instead." );
    }

    @Override
    @Transactional
    public void removeForExperiment( BioAssaySet ee ) {
        Collection<DifferentialExpressionAnalysis> diffAnalyses = this.differentialExpressionAnalysisDao
                .findByExperiment( ee );
        this.remove( diffAnalyses );
    }

    @Override
    @Transactional
    public int removeForExperimentalFactor( ExperimentalFactor experimentalFactor ) {
        Collection<DifferentialExpressionAnalysis> found = differentialExpressionAnalysisDao.findByFactor( experimentalFactor );
        this.remove( found );
        return found.size();
    }

    @Override
    @Transactional
    public int removeForExperimentalFactors( Collection<ExperimentalFactor> experimentalFactors ) {
        Collection<DifferentialExpressionAnalysis> found = differentialExpressionAnalysisDao.findByFactors( experimentalFactors );
        this.remove( found );
        return found.size();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> findByExperiment( BioAssaySet experiment ) {
        return this.differentialExpressionAnalysisDao.findByExperiment( experiment );
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Map<BioAssaySet, Collection<DifferentialExpressionAnalysis>> findByExperiments(
            Collection<BioAssaySet> experiments ) {
        return this.differentialExpressionAnalysisDao
                .findByExperiments( experiments );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> findByName( String name ) {
        return this.differentialExpressionAnalysisDao.findByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> getExperimentsWithAnalysis( Collection<Long> idsToFilter ) {
        return this.differentialExpressionAnalysisDao.getExperimentsWithAnalysis( idsToFilter );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> getExperimentsWithAnalysis( Taxon taxon ) {
        return this.differentialExpressionAnalysisDao.getExperimentsWithAnalysis( taxon );
    }
}
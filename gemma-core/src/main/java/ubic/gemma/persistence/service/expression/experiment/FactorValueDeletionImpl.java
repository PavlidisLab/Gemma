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

package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExperimentalDesignUpdatedEvent;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueUtils;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles deletions of a factor values. This process includes: 1. Determining if there are any biomaterials that use
 * the factor value 2. If so, remove any differential expression analysis results that use this factor 3. remove the
 * factor value
 *
 * @author tvrossum
 */
@Service
public class FactorValueDeletionImpl implements FactorValueDeletion {

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private ExperimentalFactorService experimentalFactorService = null;

    @Autowired
    private FactorValueService factorValueService = null;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Override
    @Transactional(readOnly = true)
    public List<Long> resolveFactorValueIds( ExpressionExperiment ee, String factor, Collection<String> values ) {
        ExperimentalDesign design = loadDesign( ee );
        List<ExperimentalFactor> factors = design.getExperimentalFactors().stream()
                .filter( ef -> factor.equals( String.valueOf( ef.getId() ) ) || factor.equalsIgnoreCase( ef.getName() ) )
                .collect( Collectors.toList() );
        if ( factors.size() != 1 ) {
            throw new IllegalArgumentException( String.format( "%s has %s factor named %s; its factors are: %s.", ee.getShortName(),
                    factors.isEmpty() ? "no" : "more than one", factor, design.getExperimentalFactors().stream()
                            .map( ef -> ef.getId() + " " + ef.getName() ).sorted().collect( Collectors.joining( ", " ) ) ) );
        }
        ExperimentalFactor ef = factors.get( 0 );
        List<Long> ids = new ArrayList<>();
        for ( String value : values ) {
            List<FactorValue> matches = ef.getFactorValues().stream()
                    .filter( fv -> value.equalsIgnoreCase( FactorValueUtils.getValue( fv, ", " ) ) )
                    .collect( Collectors.toList() );
            if ( matches.size() != 1 ) {
                throw new IllegalArgumentException( String.format( "%s %s matches %d factor values of %s; select by ID instead. Its factor values are: %s.",
                        ef.getName(), value, matches.size(), ee.getShortName(), ef.getFactorValues().stream()
                                .map( fv -> fv.getId() + " " + FactorValueUtils.getSummaryString( fv ) ).sorted().collect( Collectors.joining( "; " ) ) ) );
            }
            ids.add( matches.get( 0 ).getId() );
        }
        return ids;
    }

    @Override
    @Transactional(readOnly = true)
    public String describeDeletion( ExpressionExperiment ee, Collection<Long> factorValueIds ) {
        Collection<FactorValue> fvs = loadOwnFactorValues( ee, factorValueIds );
        ExpressionExperiment loaded = expressionExperimentService.loadOrFail( ee.getId() );
        StringBuilder sb = new StringBuilder();
        Set<ExperimentalFactor> factors = new HashSet<>();
        for ( FactorValue fv : fvs ) {
            long samples = loaded.getBioAssays().stream()
                    .filter( ba -> ba.getSampleUsed() != null && ba.getSampleUsed().getAllFactorValues().contains( fv ) )
                    .count();
            sb.append( String.format( "Factor value %d of %s: %s, assigned to %d sample(s)%n", fv.getId(),
                    fv.getExperimentalFactor().getName(), FactorValueUtils.getSummaryString( fv ), samples ) );
            factors.add( fv.getExperimentalFactor() );
        }
        for ( ExperimentalFactor ef : factors ) {
            Collection<DifferentialExpressionAnalysis> analyses = differentialExpressionAnalysisService.findByFactor( ef );
            sb.append( String.format( "%d differential expression analyses use %s and will be deleted%s%n", analyses.size(), ef.getName(),
                    analyses.isEmpty() ? "" : ": " + analyses.stream().map( a -> String.valueOf( a.getId() ) ).sorted().collect( Collectors.joining( ", " ) ) ) );
        }
        return sb.toString();
    }

    @Override
    @Transactional
    @Audited(value = ExperimentalDesignUpdatedEvent.class,
            messageSpel = "'Deleted factor values ' + #factorValueIds + ' and the differential expression analyses using their factor'")
    public void deleteFactorValues( ExpressionExperiment ee, Collection<Long> factorValueIds ) {
        loadOwnFactorValues( ee, factorValueIds );
        deleteFactorValues( factorValueIds );
    }

    private ExperimentalDesign loadDesign( ExpressionExperiment ee ) {
        ExpressionExperiment loaded = expressionExperimentService.loadOrFail( ee.getId() );
        if ( loaded.getExperimentalDesign() == null ) {
            throw new IllegalArgumentException( ee.getShortName() + " has no experimental design." );
        }
        return loaded.getExperimentalDesign();
    }

    /**
     * Load the factor values, refusing any that is missing or belongs to another experiment's design.
     */
    private Collection<FactorValue> loadOwnFactorValues( ExpressionExperiment ee, Collection<Long> factorValueIds ) {
        Long designId = loadDesign( ee ).getId();
        List<FactorValue> fvs = new ArrayList<>();
        for ( Long id : factorValueIds ) {
            FactorValue fv = factorValueService.load( id );
            if ( fv == null ) {
                throw new IllegalArgumentException( "No factor value with ID " + id + "." );
            }
            if ( fv.getExperimentalFactor() == null || !designId.equals( fv.getExperimentalFactor().getExperimentalDesign().getId() ) ) {
                throw new IllegalArgumentException( "Factor value " + id + " does not belong to the design of " + ee.getShortName() + "." );
            }
            fvs.add( fv );
        }
        return fvs;
    }

    @Override
    @Transactional
    public void deleteFactorValues( Collection<Long> fvIds ) {

        Collection<FactorValue> fvsToDelete = new ArrayList<>();

        for ( Long fvId : fvIds ) {

            FactorValue fv = factorValueService.load( fvId );

            if ( fv == null ) {
                throw new IllegalArgumentException( "No factor value with id=" + fvId + " could be loaded" );
            }

            if ( fv.getExperimentalFactor() == null ) {
                throw new IllegalStateException( "No experimental factor for factor value " + fv.getId() );
            }

            /*
             * Delete any diff ex analyses that use this factor.
             */
            ExperimentalFactor ef = experimentalFactorService.loadOrFail( fv.getExperimentalFactor().getId() );
            Collection<DifferentialExpressionAnalysis> analyses = differentialExpressionAnalysisService
                    .findByFactor( ef );
            // Warning: slow.
            for ( DifferentialExpressionAnalysis a : analyses ) {
                differentialExpressionAnalysisService.remove( a );
            }

            ef.getFactorValues().remove( fv ); // this gets done by the factorValueService as well, but can't hurt.
            fvsToDelete.add( fv );

        }

        for ( FactorValue fv : fvsToDelete ) {
            factorValueService.remove( fv );
        }

    }

}

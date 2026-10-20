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
package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.persistence.service.AbstractVoEnabledService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;

import java.util.Collection;

/**
 * @author pavlidis
 * @see ExperimentalFactorService
 */
@Service
public class ExperimentalFactorServiceImpl
        extends AbstractVoEnabledService<ExperimentalFactor, ExperimentalFactorValueObject>
        implements ExperimentalFactorService {

    private final DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    private final BioMaterialService bioMaterialService;

    @Autowired
    private ExperimentalFactorReadService readService;

    @Autowired
    public ExperimentalFactorServiceImpl( ExperimentalFactorDao experimentalFactorDao,
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService, BioMaterialService bioMaterialService ) {
        super( experimentalFactorDao );
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
        this.bioMaterialService = bioMaterialService;
    }

    @Override
    @Transactional
    public void remove( ExperimentalFactor experimentalFactor ) {
        experimentalFactor = ensureInSession( experimentalFactor );

        log.info( "Removing factor " + experimentalFactor + "..." );
        // First, check to see if there are any diff results that use this factor.
        int removedAnalysis = differentialExpressionAnalysisService.removeForExperimentalFactor( experimentalFactor );
        if ( removedAnalysis > 0 ) {
            log.info( String.format( "Removed %d analyses associated to factor %s", removedAnalysis, experimentalFactor ) );
        }

        // remove associations with the experimental factor values in related expression experiments
        //
        // 🛑 This runs BEFORE the detach below, and the order is not incidental.
        // BioMaterialService#findByFactor is @Secured({"IS_AUTHENTICATED_ANONYMOUSLY","ACL_SECURABLE_READ"}),
        // so an interceptor resolves the factor's ACL parent here. An ExperimentalFactor is a SecuredChild and
        // ParentIdentityRetrievalStrategyImpl resolves it via ExpressionExperimentDao.findIdByFactor, whose HQL
        // joins ed.experimentalFactors. Detaching first makes that query auto-flush the pending removal, match no
        // row, and return a null parent identity -- the ACL cannot inherit from the experiment and the vote
        // denies with "Access is denied", for an administrator, because it is a lookup that found nothing rather
        // than a permission that was refused.
        Collection<BioMaterial> bioMaterials = bioMaterialService.findByFactor( experimentalFactor );
        for ( BioMaterial bm : bioMaterials ) {
            if ( bm.getFactorValues().removeAll( experimentalFactor.getFactorValues() ) ) {
                log.info( "Removed factor value(s) of " + experimentalFactor + " from " + bm );
            }
        }

        // detach the experimental factor from its experimental design, otherwise it will be re-saved in cascade
        //
        // Deliberately the last thing before the delete: super.remove is a self-invocation and so passes through
        // no proxy, which makes this the only point where nothing further needs the ACL lookup the detach breaks.
        ExperimentalDesign ed = experimentalFactor.getExperimentalDesign();
        ed.getExperimentalFactors().remove( experimentalFactor );

        super.remove( experimentalFactor );
    }

    @Override
    @Transactional
    public void remove( Collection<ExperimentalFactor> experimentalFactors ) {
        experimentalFactors = ensureInSession( experimentalFactors );
        // First, check to see if there are any diff results that use this factor.
        int removedAnalysis = differentialExpressionAnalysisService.removeForExperimentalFactors( experimentalFactors );
        if ( removedAnalysis > 0 ) {
            log.info( String.format( "Removed %d analyses associated to %d factors", removedAnalysis, experimentalFactors.size() ) );
        }
        // detach each factor from its experimental design (mirrors the single-arg remove()):
        // HB6 merge() on a detached EE would otherwise cascade through the design's
        // experimentalFactors PersistentSet and trip EntityNotFoundException on the just-deleted
        // rows. Removing now keeps the in-session collection consistent with what's about to be
        // deleted, and parallel removals from BioMaterials are handled by per-factor cascade in
        // the DAO layer.
        for ( ExperimentalFactor ef : experimentalFactors ) {
            // 🛑 findByFactor BEFORE the detach, for the reason spelled out in the single-argument remove above:
            // it is a secured call whose ACL parent lookup joins ed.experimentalFactors, so detaching first makes
            // that lookup find nothing and the vote deny. Same defect, second overload -- fixing one and leaving
            // the other is how this comes back.
            Collection<BioMaterial> bioMaterials = bioMaterialService.findByFactor( ef );
            for ( BioMaterial bm : bioMaterials ) {
                if ( bm.getFactorValues().removeAll( ef.getFactorValues() ) ) {
                    log.info( "Removed factor value(s) of " + ef + " from " + bm );
                }
            }
            ExperimentalDesign ed = ef.getExperimentalDesign();
            if ( ed != null ) {
                ed.getExperimentalFactors().remove( ef );
            }
        }
        super.remove( experimentalFactors );
    }

    // =====================================================================
    // Read methods -- delegate to ExperimentalFactorReadService.
    // ACL @Secured annotations live on the ExperimentalFactorService
    // interface and apply at the facade proxy boundary.
    // =====================================================================

    @Override
    public ExperimentalFactor thaw( ExperimentalFactor ef ) {
        return readService.thaw( ef );
    }

}
/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.Compound;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialDao;
import ubic.gemma.persistence.service.expression.biomaterial.CompoundDao;

/**
 * Phase 3 ExpressionPersister retirement: skeleton implementation.
 * <p>
 * Each method here is the "find-or-create primitive" of one of the trivial
 * persist methods that used to live on {@code ExpressionPersister}. The
 * persister still owns the surrounding orchestration (taxon resolution,
 * external-accession fill-in, parent-EF fill-in, etc.) and calls into this
 * service for the actual BK lookup / DAO create.
 * <p>
 * The DAO {@code findOrCreate(entity)} methods already wrap
 * {@link ubic.gemma.persistence.util.BusinessKey#find} (see
 * {@code BioMaterialDaoImpl.find}, {@code FactorValueDaoImpl.find},
 * {@code ExpressionExperimentSubSetDaoImpl.find}, and
 * {@code CompoundDaoImpl.find}). Delegating preserves the BK semantics
 * including the retry loop in {@code BioMaterialDaoImpl}.
 *
 * @author pavlidis
 */
@Service
public class EeWriteServiceImpl implements EeWriteService {

    @Autowired
    private BioMaterialDao bioMaterialDao;

    @Autowired
    private CompoundDao compoundDao;

    @Autowired
    private ExperimentalFactorDao experimentalFactorDao;

    @Autowired
    private ExpressionExperimentSubSetDao expressionExperimentSubSetDao;

    @Autowired
    private FactorValueDao factorValueDao;

    @Override
    public Compound findOrCreate( Compound compound ) {
        return compoundDao.findOrCreate( compound );
    }

    @Override
    public BioMaterial findOrCreate( BioMaterial bioMaterial ) {
        return bioMaterialDao.findOrCreate( bioMaterial );
    }

    @Override
    public FactorValue findOrCreate( FactorValue factorValue ) {
        return factorValueDao.findOrCreate( factorValue );
    }

    @Override
    public ExperimentalFactor create( ExperimentalFactor experimentalFactor ) {
        // ExperimentalFactor is composition-owned by ExperimentalDesign.
        // Pre-Phase-3 comment: "uses 'create', not 'findOrCreate'".
        return experimentalFactorDao.create( experimentalFactor );
    }

    @Override
    public ExpressionExperimentSubSet findOrCreate( ExpressionExperimentSubSet subSet ) {
        return expressionExperimentSubSetDao.findOrCreate( subSet );
    }
}

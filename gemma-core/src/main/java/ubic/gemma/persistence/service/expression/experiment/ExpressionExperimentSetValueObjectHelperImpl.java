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
 * Copyright (c) 2012 University of British Columbia
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

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This class will handle population of ExpressionExperimentSetValueObjects. Services need to be accessed in order to
 * fill size, experiment ids, and publik/private fields.
 *
 * @author tvrossum
 *
 */
@Component
@CommonsLog
public class ExpressionExperimentSetValueObjectHelperImpl implements ExpressionExperimentSetValueObjectHelper {

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private TaxonService taxonService;

    /*
     * @see
     * ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetValueObjectHelper#convertToLightValueObject(ubic.gemma
     * .model.analysis.expression.ExpressionExperimentSet)
     */
    @Override
    public ExpressionExperimentSet convertToEntity( ExpressionExperimentSetValueObject setVO ) {
        if ( setVO == null ) {
            return null;
        }
        ExpressionExperimentSet entity;
        if ( setVO.getId() == null || setVO.getId() < 0 ) {
            entity = ExpressionExperimentSet.Factory.newInstance();
            entity.setId( null );
        } else {
            entity = expressionExperimentSetService.loadOrFail( setVO.getId() );
        }

        entity.setDescription( setVO.getDescription() );
        Collection<ExpressionExperiment> experiments = expressionExperimentService
                .load( setVO.getExpressionExperimentIds() );

        if ( experiments.isEmpty() ) {
            throw new IllegalArgumentException(
                    "The value object must have some experiments associated before it can be converted and persisted" );
        }

        Set<BioAssaySet> bas = new HashSet<BioAssaySet>( experiments );
        entity.setExperiments( bas );
        entity.setName( setVO.getName() );

        if ( setVO.getTaxonId() != null && setVO.getTaxonId() >= 0 ) {
            Taxon tax = taxonService.load( setVO.getTaxonId() );
            entity.setTaxon( tax );
        } else {
            log.debug( "Trying to convert DatabaseBackedExpressionExperimentSetValueObject with id =" + setVO.getId()
                    + " to ExpressionExperimentSet entity. Unmatched ValueObject.getTaxonId() was :" + setVO
                    .getTaxonId() );
        }

        return entity;
    }

}

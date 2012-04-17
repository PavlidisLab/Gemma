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

package ubic.gemma.expression.experiment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.jfree.util.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSetService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.security.SecurityService;

/**
 * This class will handle population of ExpressionExperimentSetValueObjects. Services need to be accessed in order to
 * fill size, experiment ids, and publik/private fields.
 * 
 * @author tvrossum
 * @version $Id$
 */
@Component
public class ExpressionExperimentSetValueObjectHelperImpl implements ExpressionExperimentSetValueObjectHelper {

    @Autowired
    private ExpressionExperimentSetDao expressionExperimentSetDao = null;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private TaxonService taxonService;

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.expression.experiment.ExpressionExperimentSetValueObjectHelper#convertToValueObject(ubic.gemma.model
     * .analysis.expression.ExpressionExperimentSet)
     */
    @Override
    public DatabaseBackedExpressionExperimentSetValueObject convertToValueObject( ExpressionExperimentSet set ) {
        if ( set == null ) {
            return null;
        }

        DatabaseBackedExpressionExperimentSetValueObject vo = this.convertToLightValueObject( set );

        /*
         * getExperimentsInSet to get the security-filtered ees.
         */
        vo.setExpressionExperimentIds( this.expressionExperimentSetDao.getExperimentIds( set.getId() ) );

        return vo;
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.expression.experiment.ExpressionExperimentSetValueObjectHelper#convertToValueObjects(java.util.Collection
     * )
     */
    @Override
    public Collection<DatabaseBackedExpressionExperimentSetValueObject> convertToValueObjects(
            Collection<ExpressionExperimentSet> sets ) {
        Collection<DatabaseBackedExpressionExperimentSetValueObject> vos = new ArrayList<DatabaseBackedExpressionExperimentSetValueObject>();
        java.util.Iterator<ExpressionExperimentSet> iter = sets.iterator();
        while ( iter.hasNext() ) {
            vos.add( convertToValueObject( iter.next() ) );
        }
        return vos;
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.expression.experiment.ExpressionExperimentSetValueObjectHelper#convertToLightValueObject(ubic.gemma
     * .model.analysis.expression.ExpressionExperimentSet)
     */
    @Override
    public DatabaseBackedExpressionExperimentSetValueObject convertToLightValueObject( ExpressionExperimentSet set ) {
        if ( set == null ) {
            return null;
        }
        int size = expressionExperimentSetDao.getExperimentCount( set.getId() );
        // assert size > 1; // should be due to the query.

        DatabaseBackedExpressionExperimentSetValueObject vo = new DatabaseBackedExpressionExperimentSetValueObject();

        vo.setNumExperiments( size );
        vo.setName( set.getName() );
        vo.setId( set.getId() );
        vo.setDescription( set.getDescription() == null ? "" : set.getDescription() );
        Taxon taxon = expressionExperimentSetDao.getTaxon( set.getId() );
        if ( taxon == null ) {
            // happens in test databases that aren't properly populated.
            // log.debug( "No taxon provided" );
        } else {
            vo.setTaxonId( taxon.getId() );
            vo.setTaxonName( taxon.getCommonName() );
        }

        vo.setCurrentUserHasWritePermission( securityService.isEditable( set ) );
        vo.setCurrentUserIsOwner( securityService.isOwnedByCurrentUser( set ) );
        vo.setPublik( securityService.isPublic( set ) );
        vo.setShared( securityService.isShared( set ) );

        // if the set is used in an analysis, it should not be modifiable
        if ( expressionExperimentSetService.getAnalyses( set ).size() > 0 ) {
            vo.setModifiable( false );
        } else {
            vo.setModifiable( true );
        }

        return vo;
    }

    /*
     * @see
     * ubic.gemma.expression.experiment.ExpressionExperimentSetValueObjectHelper#convertToLightValueObject(ubic.gemma
     * .model.analysis.expression.ExpressionExperimentSet)
     */
    @Override
    public ExpressionExperimentSet convertToEntity( DatabaseBackedExpressionExperimentSetValueObject setVO ) {
        if ( setVO == null ) {
            return null;
        }
        ExpressionExperimentSet entity;
        if ( setVO.getId() == null || setVO.getId() < 0 ) {
            entity = ExpressionExperimentSet.Factory.newInstance();
            entity.setId( null );
        } else {
            entity = expressionExperimentSetDao.load( setVO.getId() );
        }

        entity.setDescription( setVO.getDescription() );
        Collection<ExpressionExperiment> experiments = expressionExperimentService.loadMultiple( setVO
                .getExpressionExperimentIds() );
        Collection<BioAssaySet> bas = new HashSet<BioAssaySet>();
        bas.addAll( experiments );
        entity.setExperiments( bas );
        entity.setName( setVO.getName() );

        if ( setVO.getTaxonId() != null && setVO.getTaxonId() >= 0 ) {
            Taxon tax = taxonService.load( setVO.getTaxonId() );
            entity.setTaxon( tax );
        } else {
            Log.debug( "Trying to convert DatabaseBackedExpressionExperimentSetValueObject with id =" + setVO.getId()
                    + " to ExpressionExperimentSet entity. Unmatched ValueObject.getTaxonId() was :"+setVO.getTaxonId() );
        }

        return entity;
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.expression.experiment.ExpressionExperimentSetValueObjectHelper#convertToLightValueObjects(java.util
     * .Collection)
     */
    @Override
    public Collection<DatabaseBackedExpressionExperimentSetValueObject> convertToLightValueObjects(
            Collection<ExpressionExperimentSet> sets ) {
        Collection<DatabaseBackedExpressionExperimentSetValueObject> vos = new ArrayList<DatabaseBackedExpressionExperimentSetValueObject>();
        java.util.Iterator<ExpressionExperimentSet> iter = sets.iterator();
        while ( iter.hasNext() ) {
            vos.add( convertToLightValueObject( iter.next() ) );
        }
        return vos;
    }

}

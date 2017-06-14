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
package ubic.gemma.persistence.service.expression.biomaterial;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.BaseVoEnabledService;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * @author kelsey
 */
public interface BioMaterialService extends BaseVoEnabledService<BioMaterial, BioMaterialValueObject> {

    /**
     * Copies a bioMaterial.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    BioMaterial copy( BioMaterial bioMaterial );

    @Secured({ "GROUP_USER" })
    BioMaterial create( BioMaterial bioMaterial );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE__READ" })
    Collection<BioMaterial> findByExperiment( ExpressionExperiment experiment );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<BioMaterial> findByFactorValue( FactorValue fv );

    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    BioMaterial findOrCreate( BioMaterial bioMaterial );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExpressionExperiment getExpressionExperiment( Long id );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    BioMaterial load( Long id );

    @Secured({ "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    Collection<BioMaterial> loadAll();

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<BioMaterial> load( Collection<Long> ids );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void remove( BioMaterial bioMaterial );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE__READ" })
    void thaw( BioMaterial bioMaterial );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    void thaw( Collection<BioMaterial> bioMaterials );

    /**
     * Updates the given biomaterial to the database.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( BioMaterial bioMaterial );

    /**
     * Update the biomaterials that are described by the given valueObjects. This is used to update experimental designs
     * in particular.
     *
     * @return the biomaterials that were modified.
     */
    @Secured({ "GROUP_USER" })
    Collection<BioMaterial> updateBioMaterials( Collection<BioMaterialValueObject> valueObjects );

    /**
     * Associate dates with bioassays and any new factors with the biomaterials. Note we can have missing values.
     */
    @Secured({ "GROUP_ADMIN" })
    void associateBatchFactor( Map<BioMaterial, Date> dates, Map<Date, FactorValue> d2fv );

    String getBioMaterialIdList( Collection<BioMaterial> bioMaterials );

}

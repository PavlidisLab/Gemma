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
package ubic.gemma.persistence.service.expression.bioAssay;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.persistence.service.*;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import javax.annotation.CheckReturnValue;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author kelsey
 */
@Service
public interface BioAssayService extends BaseService<BioAssay>, FilteringVoEnabledService<BioAssay, BioAssayValueObject> {

    /**
     * Associates a bioMaterial with a specified bioAssay.
     *
     * @param bioAssay    bio assay
     * @param bioMaterial bio material
     */
    @PreAuthorize("hasPermission(#bioAssay, 'write') or hasPermission(#bioAssay, 'administration')")
    void addBioMaterialAssociation( BioAssay bioAssay, BioMaterial bioMaterial );

    /**
     * Locate all BioAssayDimensions in which the selected BioAssay occurs
     *
     * @param bioAssay bio assay
     * @return bio assay dimensions
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<BioAssayDimension> findBioAssayDimensions( BioAssay bioAssay );

    /**
     * @param accession eg GSM12345.
     * @return BioAssays that match based on the plain accession (unconstrained by ExternalDatabase).
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<BioAssay> findByAccession( String accession );

    @Override
    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    BioAssay findOrCreate( BioAssay bioAssay );

    @Override
    @Secured({ "GROUP_USER" })
    BioAssay create( BioAssay bioAssay );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<BioAssay> load( Collection<Long> ids );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    BioAssay load( Long id );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<BioAssay> loadAll();

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void remove( BioAssay bioAssay );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( BioAssay bioAssay );

    /**
     * Removes the association between a specific bioMaterial and a bioAssay.
     *
     * @param bioAssay    bio assay
     * @param bioMaterial bio material
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void removeBioMaterialAssociation( BioAssay bioAssay, BioMaterial bioMaterial );

    @CheckReturnValue
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    BioAssay thaw( BioAssay bioAssay );

    @CheckReturnValue
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<BioAssay> thaw( Collection<BioAssay> bioAssays );

    List<BioAssayValueObject> loadValueObjects( Collection<BioAssay> entities, boolean basic );
}

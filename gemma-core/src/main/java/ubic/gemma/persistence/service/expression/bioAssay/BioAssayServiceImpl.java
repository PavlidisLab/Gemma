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
package ubic.gemma.persistence.service.expression.bioAssay;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialDao;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author pavlidis
 * @author keshav
 * @author joseph
 * @see BioAssayService
 */
@Service
public class BioAssayServiceImpl extends AbstractFilteringVoEnabledService<BioAssay, BioAssayValueObject>
        implements BioAssayService {

    private final BioAssayDao bioAssayDao;

    private final BioMaterialDao bioMaterialDao;

    @Autowired
    private BioAssayReadService readService;

    @Autowired
    public BioAssayServiceImpl( BioAssayDao bioAssayDao, BioMaterialDao bioMaterialDao ) {
        super( bioAssayDao );
        this.bioAssayDao = bioAssayDao;
        this.bioMaterialDao = bioMaterialDao;
    }

    @Override
    @Transactional
    public void addBioMaterialAssociation( final BioAssay bioAssay,
            final ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial ) {
        this.handleAddBioMaterialAssociation( bioAssay, bioMaterial );

    }

    /**
     * @see BioAssayService#removeBioMaterialAssociation(BioAssay, ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    @Transactional
    public void removeBioMaterialAssociation( final BioAssay bioAssay, final BioMaterial bioMaterial ) {
        this.handleRemoveBioMaterialAssociation( bioAssay, bioMaterial );
    }

    // =====================================================================
    // Read methods -- delegate to BioAssayReadService.
    // ACL @Secured annotations live on the BioAssayService interface
    // and apply at the facade proxy boundary.
    // =====================================================================

    /**
     * @see BioAssayService#findBioAssayDimensions(BioAssay)
     */
    @Override
    public Collection<BioAssayDimension> findBioAssayDimensions( final BioAssay bioAssay ) {
        return readService.findBioAssayDimensions( bioAssay );
    }

    @Override
    public BioAssay findByShortName( String shortName ) {
        return readService.findByShortName( shortName );
    }

    @Override
    public Collection<BioAssay> findByAccession( String accession ) {
        return readService.findByAccession( accession );
    }

    @Override
    public Collection<BioAssay> findSubBioAssays( BioAssay bioAssay, boolean direct ) {
        return readService.findSubBioAssays( bioAssay, direct );
    }

    @Override
    public Collection<BioAssay> findSiblings( BioAssay bioAssay ) {
        return readService.findSiblings( bioAssay );
    }

    @Override
    public Collection<BioAssaySet> getBioAssaySets( BioAssay bioAssay ) {
        return readService.getBioAssaySets( bioAssay );
    }

    @Override
    public BioAssay thaw( BioAssay ba ) {
        return readService.thaw( ba );
    }

    @Override
    public Collection<BioAssay> thaw( Collection<BioAssay> bioAssays ) {
        return readService.thaw( bioAssays );
    }

    @Override
    public List<BioAssayValueObject> loadValueObjects( Collection<BioAssay> entities, @Nullable Map<BioAssay, BioAssay> assay2sourceAssayMap, boolean basic, boolean allFactorValues ) {
        return readService.loadValueObjects( entities, assay2sourceAssayMap, basic, allFactorValues );
    }

    @Override
    public CursorPage<BioAssayValueObject> loadValueObjectsByCursorForExpressionExperiment(
            ExpressionExperiment ee, @Nullable Cursor cursor, int limit ) {
        return readService.loadValueObjectsByCursorForExpressionExperiment( ee, cursor, limit );
    }

    @Override
    public CursorPage<BioAssayValueObject> loadValueObjectsByCursorForSubSet(
            ExpressionExperimentSubSet subset, @Nullable Cursor cursor, int limit ) {
        return readService.loadValueObjectsByCursorForSubSet( subset, cursor, limit );
    }

    // =====================================================================
    // Write helpers (stay on the facade).
    // =====================================================================

    private void handleAddBioMaterialAssociation( BioAssay bioAssay, BioMaterial bioMaterial ) {
        // add bioMaterial to bioAssay
        bioAssay.setSampleUsed( bioMaterial );

        // add bioAssay to bioMaterial
        Set<BioAssay> currentBioAssays = bioMaterial.getBioAssaysUsedIn();
        currentBioAssays.add( bioAssay );
        bioMaterial.setBioAssaysUsedIn( currentBioAssays );

        // update bioMaterial name - remove text after pipes
        // this should not be necessary going forward

        // build regular expression - match only text before the first pipe
        Pattern pattern = Pattern.compile( "^(.+)|" );
        String bmName = bioMaterial.getName();
        Matcher matcher = pattern.matcher( bmName );
        if ( matcher.find() ) {
            String shortName = matcher.group();
            bioMaterial.setName( shortName );
        }

        this.update( bioAssay );
        this.bioMaterialDao.update( bioMaterial );
    }

    // TODO: Refactor so that it accepts ids and does security check later.
    private void handleRemoveBioMaterialAssociation( BioAssay bioAssay, BioMaterial bioMaterial ) {
        BioAssay bioAssayTemp = Objects.requireNonNull( this.bioAssayDao.load( bioAssay.getId() ),
                String.format( "No BioAssay with ID %d.", bioAssay.getId() ) );
        BioMaterial biomaterialToBeRemoved = Objects.requireNonNull( this.bioMaterialDao.load( bioMaterial.getId() ),
                String.format( "No BioMaterial with ID %d.", bioMaterial.getId() ) );

        BioMaterial currentBioMaterials = bioAssayTemp.getSampleUsed();
        bioAssayTemp.setSampleUsed( currentBioMaterials );

        // Remove bioAssay from bioMaterial
        Set<BioAssay> currentBioAssays = biomaterialToBeRemoved.getBioAssaysUsedIn();
        currentBioAssays.remove( bioAssayTemp );
        biomaterialToBeRemoved.setBioAssaysUsedIn( currentBioAssays );

        this.bioMaterialDao.update( biomaterialToBeRemoved );
        this.update( bioAssayTemp );

        // Check to see if the bioMaterial is now orphaned.
        // If it is, remove it; if not, update it.
        if ( currentBioAssays.size() == 0 ) {
            this.bioMaterialDao.remove( biomaterialToBeRemoved );
        }

    }
}

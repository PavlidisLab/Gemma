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
package ubic.gemma.model.expression.bioAssay;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

/**
 * @author pavlidis
 * @author keshav
 * @author joseph
 * @version $Id$
 * @see ubic.gemma.model.expression.bioAssay.BioAssayService
 */
@Service
public class BioAssayServiceImpl implements BioAssayService {

    @Autowired
    private ubic.gemma.model.expression.biomaterial.BioMaterialDao bioMaterialDao;

    @Autowired
    private ubic.gemma.model.expression.bioAssay.BioAssayDao bioAssayDao;

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#addBioMaterialAssociation(ubic.gemma.model.expression.bioAssay.BioAssay,
     *      ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    public void addBioMaterialAssociation( final BioAssay bioAssay,
            final ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial ) {
        this.handleAddBioMaterialAssociation( bioAssay, bioMaterial );

    }

    /**
     * @see BioAssayService#countAll()
     */
    @Override
    public java.lang.Integer countAll() {
        return this.handleCountAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#create(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    @Override
    public BioAssay create( BioAssay bioAssay ) {
        return this.getBioAssayDao().create( bioAssay );
    }

    /**
     * @see BioAssayService#findBioAssayDimensions(BioAssay)
     */
    @Override
    public java.util.Collection<BioAssayDimension> findBioAssayDimensions( final BioAssay bioAssay ) {
        return this.handleFindBioAssayDimensions( bioAssay );

    }

    @Override
    public Collection<BioAssay> findByAccession( String accession ) {
        return this.getBioAssayDao().findByAccession( accession );
    }

    /**
     * @see BioAssayService#findOrCreate(BioAssay)
     */
    @Override
    public BioAssay findOrCreate( final BioAssay bioAssay ) {
        return this.handleFindOrCreate( bioAssay );

    }

    /**
     * @see BioAssayService#load(java.lang.Long)
     */
    @Override
    public BioAssay load( final java.lang.Long id ) {
        return this.handleLoad( id );

    }

    /**
     * @see BioAssayService#loadAll()
     */
    @Override
    public java.util.Collection<BioAssay> loadAll() {
        return this.handleLoadAll();

    }

    /**
     * @see BioAssayService#remove(BioAssay)
     */
    @Override
    public void remove( final BioAssay bioAssay ) {
        this.handleRemove( bioAssay );

    }

    /**
     * @see BioAssayService#removeBioMaterialAssociation(BioAssay, ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    public void removeBioMaterialAssociation( final BioAssay bioAssay, final BioMaterial bioMaterial ) {
        this.handleRemoveBioMaterialAssociation( bioAssay, bioMaterial );

    }

    /**
     * Sets the reference to <code>bioAssay</code>'s DAO.
     */
    public void setBioAssayDao( BioAssayDao bioAssayDao ) {
        this.bioAssayDao = bioAssayDao;
    }

    /**
     * Sets the reference to <code>bioMaterialService</code>.
     */
    public void setBioMaterialDao( ubic.gemma.model.expression.biomaterial.BioMaterialDao bioMaterialDao ) {
        this.bioMaterialDao = bioMaterialDao;
    }

    /**
     * @see BioAssayService#thaw(BioAssay)
     */
    @Override
    public void thaw( final BioAssay bioAssay ) {
        this.handleThaw( bioAssay );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#thaw(java.util.Collection)
     */
    @Override
    public Collection<BioAssay> thaw( Collection<BioAssay> bioAssays ) {
        return this.getBioAssayDao().thaw( bioAssays );
    }

    /**
     * @see BioAssayService#update(BioAssay)
     */
    @Override
    public void update( final BioAssay bioAssay ) {
        this.handleUpdate( bioAssay );

    }

    /**
     * Gets the reference to <code>bioAssay</code>'s DAO.
     */
    protected BioAssayDao getBioAssayDao() {
        return this.bioAssayDao;
    }

    /**
     * Gets the reference to <code>bioMaterialDao</code>.
     */
    protected ubic.gemma.model.expression.biomaterial.BioMaterialDao getBioMaterialDao() {
        return this.bioMaterialDao;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssay.BioAssayServiceBase#handleAssociateBioMaterial(ubic.gemma.model.expression
     * .bioAssay.BioAssay, ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    protected void handleAddBioMaterialAssociation( BioAssay bioAssay, BioMaterial bioMaterial ) {
        // add bioMaterial to bioAssay
        bioAssay.setSampleUsed( bioMaterial );

        // add bioAssay to bioMaterial
        Collection<BioAssay> currentBioAssays = bioMaterial.getBioAssaysUsedIn();
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
        this.getBioMaterialDao().update( bioMaterial );
    }

    protected Integer handleCountAll() {
        return this.getBioAssayDao().countAll();
    }

    protected Collection<BioAssayDimension> handleFindBioAssayDimensions( BioAssay bioAssay ) {
        if ( bioAssay.getId() == null ) throw new IllegalArgumentException( "BioAssay must be persistent" );
        return this.getBioAssayDao().findBioAssayDimensions( bioAssay );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#findOrCreate(edu.columbia.gemma.expression.bioAssay.BioAssay)
     */
    protected BioAssay handleFindOrCreate( BioAssay bioAssay ) {
        return this.getBioAssayDao().findOrCreate( bioAssay );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#findById(Long)
     */
    protected BioAssay handleLoad( Long id ) {
        return this.getBioAssayDao().load( id );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#loadAll()
     */
    protected Collection<BioAssay> handleLoadAll() {
        return ( Collection<BioAssay> ) this.getBioAssayDao().loadAll();
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#markAsMissing(edu.columbia.gemma.expression.bioAssay.BioAssay)
     */
    protected void handleRemove( BioAssay bioAssay ) {
        this.getBioAssayDao().remove( bioAssay );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssay.BioAssayServiceBase#handleRemoveBioMaterial(ubic.gemma.model.expression.
     * bioAssay.BioAssay, ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    // TODO: Refactor so that it accepts ids and does security check later.
    protected void handleRemoveBioMaterialAssociation( BioAssay bioAssay, BioMaterial bioMaterial ) {
        BioAssay bioAssayTemp = this.getBioAssayDao().load( bioAssay.getId() );
        BioMaterial biomaterialToBeRemoved = this.getBioMaterialDao().load( bioMaterial.getId() );

        BioMaterial currentBioMaterials = bioAssayTemp.getSampleUsed();
        bioAssayTemp.setSampleUsed( currentBioMaterials );

        // Remove bioAssay from bioMaterial
        Collection<BioAssay> currentBioAssays = biomaterialToBeRemoved.getBioAssaysUsedIn();
        currentBioAssays.remove( bioAssayTemp );
        biomaterialToBeRemoved.setBioAssaysUsedIn( currentBioAssays );

        this.getBioMaterialDao().update( biomaterialToBeRemoved );
        this.update( bioAssayTemp );

        // Check to see if the bioMaterial is now orphaned.
        // If it is, delete it; if not, update it.
        if ( currentBioAssays.size() == 0 ) {
            this.getBioMaterialDao().remove( biomaterialToBeRemoved );
        }

    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#saveBioAssay(edu.columbia.gemma.expression.bioAssay.BioAssay)
     */
    protected void handleSaveBioAssay( BioAssay bioAssay ) {
        this.getBioAssayDao().create( bioAssay );
    }

    protected void handleThaw( BioAssay bioAssay ) {
        this.getBioAssayDao().thaw( bioAssay );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#update(BioAssay)
     */
    protected void handleUpdate( BioAssay bioAssay ) {
        this.getBioAssayDao().update( bioAssay );
    }

}
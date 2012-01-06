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
public class BioAssayServiceImpl extends ubic.gemma.model.expression.bioAssay.BioAssayServiceBase {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#create(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public BioAssay create( BioAssay bioAssay ) {
        return this.getBioAssayDao().create( bioAssay );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssay.BioAssayServiceBase#handleAssociateBioMaterial(ubic.gemma.model.expression
     * .bioAssay.BioAssay, ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    protected void handleAddBioMaterialAssociation( BioAssay bioAssay, BioMaterial bioMaterial ) throws Exception {
        // add bioMaterial to bioAssay
        Collection<BioMaterial> currentBioMaterials = bioAssay.getSamplesUsed();
        currentBioMaterials.add( bioMaterial );
        bioAssay.setSamplesUsed( currentBioMaterials );

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

    @Override
    protected Integer handleCountAll() throws Exception {
        return this.getBioAssayDao().countAll();
    }

    @Override
    protected Collection<BioAssayDimension> handleFindBioAssayDimensions( BioAssay bioAssay ) throws Exception {
        if ( bioAssay.getId() == null ) throw new IllegalArgumentException( "BioAssay must be persistent" );
        return this.getBioAssayDao().findBioAssayDimensions( bioAssay );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#findOrCreate(edu.columbia.gemma.expression.bioAssay.BioAssay)
     */
    @Override
    protected BioAssay handleFindOrCreate( BioAssay bioAssay ) throws Exception {
        return this.getBioAssayDao().findOrCreate( bioAssay );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#findById(Long)
     */
    @Override
    protected BioAssay handleLoad( Long id ) throws Exception {
        return this.getBioAssayDao().load( id );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#loadAll()
     */
    @Override
    protected Collection<BioAssay> handleLoadAll() throws Exception {
        return ( Collection<BioAssay> ) this.getBioAssayDao().loadAll();
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#markAsMissing(edu.columbia.gemma.expression.bioAssay.BioAssay)
     */
    @Override
    protected void handleRemove( BioAssay bioAssay ) throws Exception {
        this.getBioAssayDao().remove( bioAssay );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.bioAssay.BioAssayServiceBase#handleRemoveBioMaterial(ubic.gemma.model.expression.
     * bioAssay.BioAssay, ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    protected void handleRemoveBioMaterialAssociation( BioAssay bioAssay, BioMaterial bioMaterial ) throws Exception {
        // remove bioMaterial from bioAssay
        this.getBioAssayDao().thaw( bioAssay );

        Collection<BioMaterial> currentBioMaterials = bioAssay.getSamplesUsed();
        currentBioMaterials.remove( bioMaterial );
        bioAssay.setSamplesUsed( currentBioMaterials );

        // remove bioAssay from bioMaterial
        Collection<BioAssay> currentBioAssays = bioMaterial.getBioAssaysUsedIn();
        currentBioAssays.remove( bioAssay );
        bioMaterial.setBioAssaysUsedIn( currentBioAssays );

        this.getBioMaterialDao().update( bioMaterial );
        this.update( bioAssay );

        // check to see if the bioMaterial is now orphaned.
        // if it is, delete it; if not, update it
        if ( currentBioAssays.size() == 0 ) {
            this.getBioMaterialDao().remove( bioMaterial );
        }

    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#saveBioAssay(edu.columbia.gemma.expression.bioAssay.BioAssay)
     */
    protected void handleSaveBioAssay( ubic.gemma.model.expression.bioAssay.BioAssay bioAssay )
            throws java.lang.Exception {
        this.getBioAssayDao().create( bioAssay );
    }

    @Override
    protected void handleThaw( BioAssay bioAssay ) throws Exception {
        this.getBioAssayDao().thaw( bioAssay );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#update(BioAssay)
     */
    @Override
    protected void handleUpdate( BioAssay bioAssay ) throws Exception {
        this.getBioAssayDao().update( bioAssay );
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

    @Override
    public Collection<BioAssay> findByAccession( String accession ) {
        return this.getBioAssayDao().findByAccession( accession );
    }

}
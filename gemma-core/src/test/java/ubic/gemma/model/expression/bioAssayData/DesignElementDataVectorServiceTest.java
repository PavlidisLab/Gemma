/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.model.expression.bioAssayData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.ConfigUtils;

/**
 * @author joseph
 * @version $Id$
 */
public class DesignElementDataVectorServiceTest extends BaseSpringContextTest {

    @Autowired
    ExpressionExperimentService expressionExperimentService;

    ExpressionExperiment newee = null;

    DesignElementDataVector dedv;

    @Autowired
    protected GeoDatasetService geoService;

    @Autowired
    ArrayDesignService arrayDesignService;

    @Autowired
    CompositeSequenceService compositeSequenceService;

    @Autowired
    GeneService geneService;

    @Before
    public void setup() throws Exception {
        dedv = RawExpressionDataVector.Factory.newInstance();
    }

    @Test
    public void testFindByQt() {

        try {
            String path = ConfigUtils.getString( "gemma.home" );
            assert path != null;
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path
                    + AbstractGeoServiceTest.GEO_TEST_DATA_ROOT + "gse432Short" ) );
            Collection<?> results = geoService.fetchAndLoad( "GSE432", false, true, false, false );
            newee = ( ExpressionExperiment ) results.iterator().next();

        } catch ( AlreadyExistsInSystemException e ) {
            newee = ( ExpressionExperiment ) e.getData();
        }

        newee.setShortName( RandomStringUtils.randomAlphabetic( 12 ) );
        expressionExperimentService.update( newee );

        this.expressionExperimentService.thawLite( newee );

        DesignElementDataVectorService dedvs = ( DesignElementDataVectorService ) this
                .getBean( "designElementDataVectorService" );

        Collection<ExpressionExperiment> ees = new HashSet<ExpressionExperiment>();
        ees.add( newee );

        QuantitationType qt = null;
        for ( QuantitationType q : newee.getQuantitationTypes() ) {
            if ( q.getIsPreferred() ) {
                qt = q;
                break;
            }
        }

        assertNotNull( "QT is null", qt );

        Collection<? extends DesignElementDataVector> preferredVectors = dedvs.find( qt );

        assertNotNull( preferredVectors );
        assertEquals( 40, preferredVectors.size() );
    }

    /**
     * Fill in some fake genes associated with the test ee's array design.
     * 
     * @return
     */
    protected Collection<Gene> getGeneAssociatedWithEe() {
        int i = 0;
        ArrayDesign ad = newee.getBioAssays().iterator().next().getArrayDesignUsed();
        Taxon taxon = taxonService.findByCommonName( "mouse" );
        ad = this.arrayDesignService.thawLite( ad );
        Collection<Gene> genes = new HashSet<Gene>();
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            if ( i >= 10 ) break;
            Gene g = geneService.thaw( this.getTestPeristentGene() );
            BlatAssociation blata = BlatAssociation.Factory.newInstance();
            blata.setGeneProduct( g.getProducts().iterator().next() );
            BlatResult br = BlatResult.Factory.newInstance();
            BioSequence bs = cs.getBiologicalCharacteristic();
            if ( bs == null ) {
                bs = BioSequence.Factory.newInstance();
                bs.setName( RandomStringUtils.random( 10 ) );
                bs.setTaxon( taxon );
                bs = ( BioSequence ) persisterHelper.persist( bs );
                cs.setBiologicalCharacteristic( bs );
                compositeSequenceService.update( cs );
            }

            br.setQuerySequence( bs );
            blata.setBlatResult( br );
            blata.setBioSequence( bs );
            persisterHelper.persist( blata );
            genes.add( g );
        }
        return genes;
    }

}
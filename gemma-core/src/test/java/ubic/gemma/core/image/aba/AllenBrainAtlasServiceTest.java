/*
 * The Gemma project
 *
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.core.image.aba;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.testing.BaseSpringContextTest;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

/**
 * Alan brain Atlas service test.
 *
 * @author kelsey
 */
public class AllenBrainAtlasServiceTest extends BaseSpringContextTest {

    @Autowired
    private AllenBrainAtlasService abaService = null;

    @Autowired
    private GeneService geneService = null;

    @Autowired
    private TaxonService taxonService = null;

    @Test
    public void testGetGene() throws Exception {
        AbaGene abaGene;
        Gene gene = geneService.findByOfficialSymbol( "grin1", taxonService.findByCommonName( "Mouse" ) );

        try {
            abaGene = abaService.getGene( gene );
        } catch ( IOException e ) {
            if ( e.getMessage().contains( "502" ) || e.getMessage().contains( "503" ) ) {
                log.warn( "Server error from Allen Atlas: skipping test" );
                return;
            }
            throw e;
        }

        Collection<ImageSeries> representativeSaggitalImages = new HashSet<>();

        for ( ImageSeries is : abaGene.getImageSeries() ) {
            if ( is == null )
                continue;

            ImageSeries imageSeries = abaService.getImageSeries( gene );
            Collection<Image> representativeImages = new HashSet<>();

            for ( Image img : imageSeries.getImages() ) {
                if ( ( 2600 > img.getPosition() ) && ( img.getPosition() > 2200 ) ) {
                    representativeImages.add( img );
                }
            }

            if ( representativeImages.isEmpty() )
                continue;

            // Only add if there is something to add
            is.setImages( representativeImages );
            representativeSaggitalImages.add( is );

        }
        abaGene.setImageSeries( representativeSaggitalImages );
    }

}

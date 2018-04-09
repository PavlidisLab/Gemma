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
import ubic.gemma.core.testing.BaseSpringContextTest;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.assertNotNull;

/**
 * Alan brain Atlas service test.
 *
 * @author kelsey
 */
public class AllenBrainAtlasServiceTest extends BaseSpringContextTest {

    @Autowired
    private AllenBrainAtlasService abaService = null;

    @Test
    public void testGetGene() throws Exception {
        AbaGene grin1;

        try {
            grin1 = abaService.getGene( "Grin1" );
        } catch ( IOException e ) {
            if ( e.getMessage().contains( "502" ) || e.getMessage().contains( "503" ) ) {
                log.warn( "Server error from Allen Atlas: skipping test" );
                return;
            }
            throw e;
        }

        Collection<ImageSeries> representativeSaggitalImages = new HashSet<>();

        for ( ImageSeries is : grin1.getImageSeries() ) {
            if ( is == null )
                continue;
            if ( is.getPlane().equalsIgnoreCase( "sagittal" ) ) {

                Collection<Image> images = abaService.getImageSeries( is.getImageSeriesId() );
                Collection<Image> representativeImages = new HashSet<>();

                for ( Image img : images ) {
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
        }
        grin1.setImageSeries( representativeSaggitalImages );

        // log.info( grin1 );
    }

    /**
     * Not all ABA genes have the only the first letter capitalized
     */
    @Test
    public void testGetGeneCapitals() {
        AbaGene gene = null;

        try {
            gene = abaService.getGene( "BC004044" );
        } catch ( IOException e ) {
            if ( e.getMessage().contains( "502" ) || e.getMessage().contains( "503" ) ) {
                log.warn( "Server error from Allen Atlas: skipping test" );
                return;
            }
        }
        assertNotNull( gene );

        try {
            gene = abaService.getGene( "grin1" );
        } catch ( IOException e ) {
            if ( e.getMessage().contains( "502" ) || e.getMessage().contains( "503" ) ) {
                log.warn( "Server error from Allen Atlas: skipping test" );
                return;
            }
        }

        assertNotNull( gene );
    }

}

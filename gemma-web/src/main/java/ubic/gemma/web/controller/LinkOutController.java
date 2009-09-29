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

package ubic.gemma.web.controller;

import java.util.ArrayList;
import java.util.Collection;

import ubic.gemma.image.LinkOutValueObject;
import ubic.gemma.image.aba.AllenBrainAtlasService;
import ubic.gemma.image.aba.Image;
import ubic.gemma.image.aba.ImageSeries;

/**
 * A controller for getting details from other web resources (like allen brain atlas)
 * 
 * @author kelsey
 * @version $Id$
 * @spring.bean id="linkOutController"
 * @spring.property name="allenBrainAtlasService" ref="allenBrainAtlasService"
 */

public class LinkOutController {

    private AllenBrainAtlasService allenBrainAtlasService = null;

    public void setAllenBrainAtlasService( AllenBrainAtlasService allenBrainAtlasService ) {
        this.allenBrainAtlasService = allenBrainAtlasService;
    }

    /**
     * AJAX METHOD Given a gene's official symbol will return value object with the link to use
     * 
     * @param geneOfficialSymbol
     * @return
     */
    public LinkOutValueObject getAllenBrainAtlasLink( String geneOfficialSymbol ) {

        // Get Allen Brain Atals information and put in value object
        Collection<ImageSeries> imageSeries = allenBrainAtlasService
                .getRepresentativeSaggitalImages( geneOfficialSymbol );

        if ( imageSeries != null ) {
            String abaGeneUrl = allenBrainAtlasService.getGeneUrl( geneOfficialSymbol );
            Collection<Image> representativeImages = allenBrainAtlasService.getImagesFromImageSeries( imageSeries );
            Collection<String> imageUrls = new ArrayList<String>();

            for ( Image image : representativeImages ) {
                imageUrls.add( image.getDownloadExpressionPath() );
            }

            if ( !imageUrls.isEmpty() ) return new LinkOutValueObject( imageUrls, abaGeneUrl );
        }

        return null;
    }

}

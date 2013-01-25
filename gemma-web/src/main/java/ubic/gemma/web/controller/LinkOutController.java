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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.directwebremoting.annotations.Param;
import org.directwebremoting.annotations.RemoteMethod;
import org.directwebremoting.annotations.RemoteProxy;
import org.directwebremoting.spring.BeanCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import ubic.gemma.image.LinkOutValueObject;
import ubic.gemma.image.aba.AllenBrainAtlasService;
import ubic.gemma.image.aba.Image;
import ubic.gemma.image.aba.ImageSeries;

/**
 * A controller for getting details from other web resources (like allen brain atlas)
 * 
 * @author kelsey
 * @version $Id$
 */
@Controller
@RemoteProxy(creator = BeanCreator.class, creatorParams = @Param(name = "bean", value = "linkOutController"), name = "LinkOutController")
public class LinkOutController {

    @Autowired private AllenBrainAtlasService allenBrainAtlasService;

    /**
     * AJAX METHOD Given a gene's official symbol will return value object with the link to use
     * 
     * @param geneOfficialSymbol
     * @return
     */
    @RemoteMethod
    public LinkOutValueObject getAllenBrainAtlasLink( String geneOfficialSymbol ) {

        Collection<ImageSeries> imageSeries = null;
        String abaGeneUrl = null;
        Collection<String> imageUrls = new ArrayList<String>();

        // Get Allen Brain Atals information and put in value object
        try {
            imageSeries = allenBrainAtlasService.getRepresentativeSaggitalImages( geneOfficialSymbol );

            if ( imageSeries != null ) {
                abaGeneUrl = allenBrainAtlasService.getGeneUrl( geneOfficialSymbol );
                Collection<Image> representativeImages = allenBrainAtlasService.getImagesFromImageSeries( imageSeries );

                for ( Image image : representativeImages ) {
                    imageUrls.add( image.getDownloadExpressionPath() );
                }
            }
        } catch ( IOException e ) {

        }
        return new LinkOutValueObject( imageUrls, abaGeneUrl, geneOfficialSymbol );

    }

    public void setAllenBrainAtlasService( AllenBrainAtlasService allenBrainAtlasService ) {
        this.allenBrainAtlasService = allenBrainAtlasService;
    }

}

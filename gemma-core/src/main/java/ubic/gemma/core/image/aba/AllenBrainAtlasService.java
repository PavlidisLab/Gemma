/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.core.image.aba;

import ubic.gemma.model.genome.Gene;

import java.io.IOException;
import java.util.Collection;

/**
 * @author paul
 */
public interface AllenBrainAtlasService {

    /**
     *
     * @param gene gene to look up in ABA.
     * @return AbaGene.
     * @throws IOException if there is a problem while manipulating the file.
     */
    AbaGene getGene( Gene gene ) throws IOException;

    /**
     * Given a Gemma gene object returns an allen brain atlas gene URL
     *
     * @param gene gene
     * @return an allen brain atlas gene details URL
     */
    String getGeneUrl( Gene gene );

    /**
     * Returns a collection of images from all the imageSeries given.
     *
     * @param imageSeries image series
     * @return collection of images
     */
    Collection<Image> getImagesFromImageSeries( Collection<ImageSeries> imageSeries );

    /**
     * Given a Gene, returns all the image series that contain sagittal images for the given gene. Each series
     * will only contain one image from the middle of the series.
     *
     * @param gene gene to look for
     * @return all the image series that contain sagittal images for the given gene
     * @throws IOException if there is a problem while manipulating the file
     */
    Collection<ImageSeries> getSagittalImageSeries( Gene gene ) throws IOException;

}
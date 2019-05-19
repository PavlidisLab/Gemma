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

import java.util.LinkedList;
import java.util.List;

/**
 * Represents the ImageSeries information returned from the AIBS brain atlas
 *
 * @author kelsey
 */
public class ImageSeries {

    private Integer imageSeriesId;
    private List<Image> images;

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public ImageSeries( Integer imageSeriesId ) {
        this.imageSeriesId = imageSeriesId;
    }

    @SuppressWarnings("unused") // Possible external use
    public void addImage( Image image ) {
        if ( this.images == null )
            this.images = new LinkedList<>();
        this.images.add( image );
    }

    public List<Image> getImages() {
        return images;
    }

    public void setImages( List<Image> images ) {
        this.images = images;
    }

    @SuppressWarnings("WeakerAccess") // Consistency
    public Integer getImageSeriesId() {
        return imageSeriesId;
    }

    @SuppressWarnings("unused") // Possible external use
    public void setImageSeriesId( Integer imageSeriesId ) {
        this.imageSeriesId = imageSeriesId;
    }

}

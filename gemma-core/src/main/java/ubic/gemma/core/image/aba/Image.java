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

/**
 * allen brain Atlas Image class. represents 1 image in the Allen brain atlas library.
 *
 * @author kelsey
 */
@SuppressWarnings("unused") // Possible external use
public class Image {

    private String displayName;
    private Integer id;
    private Integer position = 0;
    private Integer referenceAtlasIndex = 0;
    private String thumbnailUrl;
    private String zoomifiedNisslUrl;
    private String expressionThumbnailUrl;
    private String downloadImagePath;
    private String downloadExpressionPath;
    private Integer height = 0;
    private Integer width = 0;

    public Image() {
        super();
    }

    public Image( String displayName, Integer id, Integer position, Integer referenceAtlasIndex, String thumbnailUrl,
            String zoomifiedNisslUrl, String expressionThumbnailUrl, String downloadImagePath,
            String downloadExpressionPath, Integer height, Integer width ) {

        this();
        this.displayName = displayName;
        this.id = id;
        this.position = position;
        this.referenceAtlasIndex = referenceAtlasIndex;
        this.thumbnailUrl = thumbnailUrl;
        this.zoomifiedNisslUrl = zoomifiedNisslUrl;
        this.expressionThumbnailUrl = expressionThumbnailUrl;
        this.downloadImagePath = downloadImagePath;
        this.downloadExpressionPath = downloadExpressionPath;
        this.height = height;
        this.width = width;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName( String displayName ) {
        this.displayName = displayName;
    }

    public String getDownloadExpressionPath() {
        return downloadExpressionPath;
    }

    public void setDownloadExpressionPath( String downloadExpressionPath ) {
        this.downloadExpressionPath = downloadExpressionPath;
    }

    public String getDownloadImagePath() {
        return downloadImagePath;
    }

    public void setDownloadImagePath( String downloadImagePath ) {
        this.downloadImagePath = downloadImagePath;
    }

    public String getExpressionThumbnailUrl() {
        return expressionThumbnailUrl;
    }

    public void setExpressionThumbnailUrl( String expressionThumbnailUrl ) {
        this.expressionThumbnailUrl = expressionThumbnailUrl;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight( Integer height ) {
        this.height = height;
    }

    public Integer getId() {
        return id;
    }

    public void setId( Integer id ) {
        this.id = id;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition( Integer position ) {
        this.position = position;
    }

    public Integer getReferenceAtlasIndex() {
        return referenceAtlasIndex;
    }

    public void setReferenceAtlasIndex( Integer referenceAtlasIndex ) {
        this.referenceAtlasIndex = referenceAtlasIndex;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl( String thumbnailUrl ) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth( Integer width ) {
        this.width = width;
    }

    public String getZoomifiedNisslUrl() {
        return zoomifiedNisslUrl;
    }

    public void setZoomifiedNisslUrl( String zoomifiedNisslUrl ) {
        this.zoomifiedNisslUrl = zoomifiedNisslUrl;
    }

}

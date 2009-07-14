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
package ubic.gemma.image.aba;

/**
 * Alan brain Atlas Image class.  represents 1 image in the alan brain atlas library.
 *  
 * @version $Id$ @author kelsey
 *
 */
public class Image {

    String displayName;
    Integer id;
    Integer position;
    Integer referenceAtlasIndex;
    String thumbnailUrl;
    String zoomifiedNisslUrl;
    String expressionThumbnailUrl;
    String downloadImagePath;
    String downloadExpressionPath;
    
    
   
    
    public Image() {
        super();
    }


    public Image( String displayName, Integer id, Integer position, Integer referenceAtlasIndex, String thumbnailUrl,
            String zoomifiedNisslUrl, String expressionThumbnailUrl, String downloadImagePath,
            String downloadExpressionPath ) {
        
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
    }
    
    
    public String getDisplayName() {
        return displayName;
    }
    public void setDisplayName( String displayName ) {
        this.displayName = displayName;
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
    public String getZoomifiedNisslUrl() {
        return zoomifiedNisslUrl;
    }
    public void setZoomifiedNisslUrl( String zoomifiedNisslUrl ) {
        this.zoomifiedNisslUrl = zoomifiedNisslUrl;
    }
    public String getExpressionThumbnailUrl() {
        return expressionThumbnailUrl;
    }
    public void setExpressionThumbnailUrl( String expressionThumbnailUrl ) {
        this.expressionThumbnailUrl = expressionThumbnailUrl;
    }
    public String getDownloadImagePath() {
        return downloadImagePath;
    }
    public void setDownloadImagePath( String downloadImagePath ) {
        this.downloadImagePath = downloadImagePath;
    }
    public String getDownloadExpressionPath() {
        return downloadExpressionPath;
    }
    public void setDownloadExpressionPath( String downloadExpressionPath ) {
        this.downloadExpressionPath = downloadExpressionPath;
    }
    
}

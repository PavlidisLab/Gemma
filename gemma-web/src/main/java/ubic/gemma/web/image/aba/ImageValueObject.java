/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */
/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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

package ubic.gemma.web.image.aba;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.image.aba.Image;
import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * 
 * 
 * @author tvrossum
 * @version $Id$
 */
public class ImageValueObject implements java.io.Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -4487372931236671216L;
    
    private String displayName;
    private int id;
    private int position;
    private int referenceAtlasIndex;
    private String thumbnailUrl;
    private String zoomifiedNisslUrl;
    private String expressionThumbnailUrl;
    private String downloadImagePath;
    private String downloadExpressionPath;
    private int height;
    private int width;
    private String abaGeneURL;
    private GeneValueObject abaHomologousMouseGene;
    private String queryGeneSymbol;
    private boolean usingHomologue;
    
    /**
     * needed for javabean contract
     */
    public ImageValueObject() {
    }

    /**
     * @param displayName
     * @param id
     * @param position
     * @param referenceAtlasIndex
     * @param thumbnailUrl
     * @param zoomifiedNisslUrl
     * @param expressionThumbnailUrl
     * @param downloadImagePath
     * @param downloadExpressionPath
     * @param height
     * @param width
     * @param abaGeneURL
     * @param abaHomologousMouseGene
     * @param queryGeneSymbol
     * @param usingHomologue
     */
    public ImageValueObject( String displayName, int id, int position, int referenceAtlasIndex, String thumbnailUrl,
            String zoomifiedNisslUrl, String expressionThumbnailUrl, String downloadImagePath,
            String downloadExpressionPath, int height, int width, String abaGeneURL,
            GeneValueObject abaHomologousMouseGene, String queryGeneSymbol, boolean usingHomologue ) {
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
        this.abaGeneURL = abaGeneURL;
        this.abaHomologousMouseGene = abaHomologousMouseGene;
        this.queryGeneSymbol = queryGeneSymbol;
        this.usingHomologue = usingHomologue;
    }

    public static Collection<ImageValueObject> convert2ValueObjects( Collection<Image> images, String abaGeneURL, GeneValueObject abaHomologousMouseGene, String queryGeneSymbol, boolean usingHomologue ){
        Collection<ImageValueObject> converted = new HashSet<ImageValueObject>();
        if ( images == null ) return converted;

        for ( Image i : images ) {
            if ( i == null ) continue;
            converted.add( new ImageValueObject( i.getDisplayName(), i.getId(), i.getPosition(), i.getReferenceAtlasIndex(), 
                    i.getThumbnailUrl(), i.getZoomifiedNisslUrl(), i.getExpressionThumbnailUrl(), i.getDownloadImagePath(), 
                    i.getDownloadExpressionPath(), i.getHeight(), i.getWidth(), abaGeneURL, abaHomologousMouseGene, queryGeneSymbol, usingHomologue ) );
        }

        return converted;
    }
    /**
     * @return the displayName
     */
    public String getDisplayName() {
        return displayName;
    }
    /**
     * @param displayName the displayName to set
     */
    public void setDisplayName( String displayName ) {
        this.displayName = displayName;
    }
    /**
     * @return the id
     */
    public int getId() {
        return id;
    }
    /**
     * @param id the id to set
     */
    public void setId( int id ) {
        this.id = id;
    }
    /**
     * @return the position
     */
    public int getPosition() {
        return position;
    }
    /**
     * @param position the position to set
     */
    public void setPosition( int position ) {
        this.position = position;
    }
    /**
     * @return the referenceAtlasIndex
     */
    public int getReferenceAtlasIndex() {
        return referenceAtlasIndex;
    }
    /**
     * @param referenceAtlasIndex the referenceAtlasIndex to set
     */
    public void setReferenceAtlasIndex( int referenceAtlasIndex ) {
        this.referenceAtlasIndex = referenceAtlasIndex;
    }
    /**
     * @return the thumbnailUrl
     */
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
    /**
     * @param thumbnailUrl the thumbnailUrl to set
     */
    public void setThumbnailUrl( String thumbnailUrl ) {
        this.thumbnailUrl = thumbnailUrl;
    }
    /**
     * @return the zoomifiedNisslUrl
     */
    public String getZoomifiedNisslUrl() {
        return zoomifiedNisslUrl;
    }
    /**
     * @param zoomifiedNisslUrl the zoomifiedNisslUrl to set
     */
    public void setZoomifiedNisslUrl( String zoomifiedNisslUrl ) {
        this.zoomifiedNisslUrl = zoomifiedNisslUrl;
    }
    /**
     * @return the expressionThumbnailUrl
     */
    public String getExpressionThumbnailUrl() {
        return expressionThumbnailUrl;
    }
    /**
     * @param expressionThumbnailUrl the expressionThumbnailUrl to set
     */
    public void setExpressionThumbnailUrl( String expressionThumbnailUrl ) {
        this.expressionThumbnailUrl = expressionThumbnailUrl;
    }
    /**
     * @return the downloadImagePath
     */
    public String getDownloadImagePath() {
        return downloadImagePath;
    }
    /**
     * @param downloadImagePath the downloadImagePath to set
     */
    public void setDownloadImagePath( String downloadImagePath ) {
        this.downloadImagePath = downloadImagePath;
    }
    /**
     * @return the downloadExpressionPath
     */
    public String getDownloadExpressionPath() {
        return downloadExpressionPath;
    }
    /**
     * @param downloadExpressionPath the downloadExpressionPath to set
     */
    public void setDownloadExpressionPath( String downloadExpressionPath ) {
        this.downloadExpressionPath = downloadExpressionPath;
    }
    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }
    /**
     * @param height the height to set
     */
    public void setHeight( int height ) {
        this.height = height;
    }
    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }
    /**
     * @param width the width to set
     */
    public void setWidth( int width ) {
        this.width = width;
    }

    /**
     * @return the abaGeneURL
     */
    public String getAbaGeneURL() {
        return abaGeneURL;
    }

    /**
     * @param abaGeneURL the abaGeneURL to set
     */
    public void setAbaGeneURL( String abaGeneURL ) {
        this.abaGeneURL = abaGeneURL;
    }

    /**
     * @return the abaHomologousMouseGene
     */
    public GeneValueObject getAbaHomologousMouseGene() {
        return abaHomologousMouseGene;
    }

    /**
     * @param abaHomologousMouseGene the abaHomologousMouseGene to set
     */
    public void setAbaHomologousMouseGene( GeneValueObject abaHomologousMouseGene ) {
        this.abaHomologousMouseGene = abaHomologousMouseGene;
    }


    /**
     * @return the queryGeneSymbol
     */
    public String getQueryGeneSymbol() {
        return queryGeneSymbol;
    }


    /**
     * @param queryGeneSymbol the queryGeneSymbol to set
     */
    public void setQueryGeneSymbol( String queryGeneSymbol ) {
        this.queryGeneSymbol = queryGeneSymbol;
    }


    /**
     * @return the usingHomologue
     */
    public boolean isUsingHomologue() {
        return usingHomologue;
    }


    /**
     * @param usingHomologue the usingHomologue to set
     */
    public void setUsingHomologue( boolean usingHomologue ) {
        this.usingHomologue = usingHomologue;
    }
}

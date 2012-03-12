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
package ubic.gemma.image.aba;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.Collection;

import org.w3c.dom.Document;

/**
 * @author paul
 * @version $Id$
 */
public interface AllenBrainAtlasService {

    /**
     * http://brain-map.org
     */
    public static final String API_BASE_URL = "http://www.brain-map.org";
    /**
     * /aba/api/gene/[geneSymbol].xml";
     */
    public static final String GET_GENE_URL = "/aba/api/gene/@.xml";
    /**
     * /aba/api/imageseries/[imageSeriesId].xml
     */
    public static final String GET_IMAGESERIES_URL = "/aba/api/imageseries/@.xml";
    /**
     * /aba/api/neuroblast/[structure]/[imageseriesid].xml
     */
    public static final String GET_NEUROBLAST_URL = "/aba/api/neuroblast/@/@.xml";
    /**
     * /aba/api/neuroblast/[structure]/[imageseriesid]/[Sagittal | Coronal].xml
     */
    public static final String GET_NEUROBLAST_PLANE_URL = "/aba/api/neuroblast/@/@/@.xml";
    /**
     * /aba/api/expression/[imageSeriesId].sva
     */
    public static final String GET_EXPRESSION_VOLUME_URL = "/aba/api/expression/@.sva";
    /**
     * /aba/api/expression/imageseries/[imageSeriesId].xml
     */
    public static final String GET_EXPRESSION_INFO_URL = "/aba/api/expression/imageseries/@.xml";
    /**
     * /aba/api/ara/[Sagittal | Coronal].xml
     */
    public static final String GET_ATLAS_INFO_URL = "/aba/api/ara/@.xml";
    /**
     * /aba/api/atlas/map/[imageseriesid].map
     */
    public static final String GET_ATLAS_IMAGE_MAP_URL = "/aba/api/atlas/map/@.map";
    /**
     * /aba/api/image/info?path=[the actual path to the image, as recovered from the imageSeries.xml]
     */
    public static final String GET_IMAGE_INFO_BYPATH_URL = "/aba/api/image/info?path=@";
    /**
     * /aba/api/image/info/[imageId].xml
     */
    public static final String GET_IMAGE_INFO_BYID_URL = "/aba/api/image/info/@.xml";
    /**
     * /aba/api/image?zoom=[image tier; usually 0-6, or -1 for highest tier]&path=[actual path, as above]
     */
    public static final String GET_IMAGE_URL = "/aba/api/image?mime=@&zoom=@&path=@";
    /**
     * /aba/api/image?zoom=[tier]&top=[unscaled pixel top]&left=[unscaled pixel left]&width=[actual pixel
     * width]&height=[actual pixel height]&path=[as above]
     */
    public static final String GET_IMAGE_ROI_URL = "/aba/api/image?mime=@&zoom=@&top=@&left=@&width=@&height=@&path=@";
    /**
     * /aba/api/gene/search?term=[some text, which will be used in a contains query for symbol, name & aliases]
     */
    public static final String SEARCH_GENE_URL = "/aba/api/gene/search?term=@";
    /**
     * For showing details about gene information on the allen brain atlas web site
     */
    public static final String HTML_GENE_DETAILS_URL = "http://mouse.brain-map.org/brain/@.html?ispopup=1";
    /**
     * requesting an ROI with MIME_IMAGE from a browser will let the image be shown within the browser; using
     * MIME_APPLICATION will cause the user to prompt to download.
     */
    public static final Integer MIME_IMAGE = 2;
    public static final Integer MIME_APPLICATION = 1;

    /**
     * Returns a document describing the given ImageSeriesId
     * 
     * @param imageseriesId
     * @return
     */
    public abstract Document getAtlasImageMap( Integer imageseriesId );

    /**
     * Finds the associated immage map for the given series id and writes it to the given output stream
     * 
     * @param imageseriesid
     * @param out
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    public abstract boolean getAtlasImageMap( Integer imageseriesid, OutputStream out ) throws MalformedURLException,
            IOException;

    /**
     * Use this method to get information like width, height & number of tiers (zoom levels) available for the given
     * image.
     * 
     * @param plane
     * @param out
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    public abstract boolean getAtlasInfo( String plane, OutputStream out ) throws MalformedURLException, IOException;

    /**
     * Gets the caching directory
     * 
     * @return
     */
    public abstract String getCacheDir();

    /**
     * Is caching on?
     * 
     * @return
     */
    public abstract boolean getCaching();

    /**
     * return std error stream
     * 
     * @return
     */
    public abstract PrintStream getErrOut();

    /**
     * @param imageseriesId
     * @param out
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    public abstract boolean getExpressionInfo( Integer imageseriesId, OutputStream out ) throws MalformedURLException,
            IOException;

    /**
     * Each of these data files represent the volume of space occupied by a single mouse brain. The volume space is
     * divided into individual 3D cubic voxels, of dimension (200 x 200 x 200) microns. The file contains the set of
     * sagittally arranged voxels (expressed as x, y, z coordinates) that have an expression energy value other than
     * 0.0. Along with each voxel in the data file is an "expression energy" value. The energy value is a function of
     * the intensity of expression found within that voxel, together with the density of expression in that voxel. Due
     * to the way gene expression experiments are carried out there are generally at least 200 microns between any two
     * (25 micron thick) sections. In these datafiles, interpolation is used to "smooth" the energy values between those
     * gaps to produce an expression value for each voxel that corresponds to brain volume. For more information about
     * how expression is mapped to the atlas, see the Informatics Data Processing white paper. The ImageSeriesID
     * parameter is an integer; find these as part of the return document from the Genes method.
     * 
     * @param imageseriesId
     * @param out
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    public abstract boolean getExpressionVolume( Integer imageseriesId, OutputStream out )
            throws MalformedURLException, IOException;

    /**
     * Returns AbaGene object for a gene symbol. If it fails to find a gene using the given string it tries the first
     * letter capitalized string.
     * 
     * @param givenGene
     * @return AbaGene
     * @throws IOException
     */
    public abstract AbaGene getGene( String givenGene ) throws IOException;

    /**
     * Given a valid official symbol for a gene (case sensitive) returns an allen brain atals gene details URL
     * 
     * @param gene
     * @return
     */
    public abstract String getGeneUrl( String gene );

    /**
     * Not all the parameters are required. The simplest url would look like this:
     * http://www.brain-map.org/aba/api/image?zoom=[zoom]&path=[path]. The different options available are: path
     * Required. The path to the desired image file. The image file path is available as part of the imageSeries XML
     * returned by the ImageSeries API method. The XPath /image-series/images/image/downloadImagePath will return the
     * paths for all of the images in a given image series. zoom Required. The level of resolution desired. The lowest
     * resolution is 0, which is a thumbnail sized image. The highest resolution varies by image, but is usually 6 for
     * ABA images. Each level is a downsampling by a factor of two of the next higher level. Use the value -1 to request
     * the highest resolution available. mime Optional. Determines the mime type of the HTTP response. The default
     * produces an image with mime type "application/jpeg". Most web browsers, when receiving this kind of response will
     * prompt the user to pick an application to open the file, or to select a location to save the file to disk. Using
     * mime=2 will cause a response with mime type = "jpeg/image" to be returned. Most browsers will display the
     * returned image as soon as it is available. When retrieving an image with anything other than a web browser, this
     * parameter can usually be ignored. The following region-of-interest parameters are optional, however if any of
     * them are given, they must all be present. top The y coordinate of the top left corner of the region of interest.
     * This is given in terms of the full size image, regardless of which tier is requested. left The x coordinate of
     * the top left corner of the desired region of interest. As above, this is in terms of the full scale image,
     * regardless of the tier requested. width The actual width in pixels of the desired image. height The actual height
     * in pixels of the desired image. Any malformed URL or other failure in the image fetching process results in an
     * HTTP response code of 500. Example http://www.brain-
     * map.org/aba/api/image?zoom=5&top=4000&left=8000&width=300&height=30 0&mime=2&path=/production11/Guk1_04-
     * 0874_25411/zoomify/primary/0207030123/Guk1_70_0207030123_A.aff
     * 
     * @param imagePath
     * @param zoom
     * @param top
     * @param left
     * @param width
     * @param height
     * @param mimeType
     * @param out
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    public abstract boolean getImageROI( String imagePath, Integer zoom, Integer top, Integer left, Integer width,
            Integer height, Integer mimeType, OutputStream out ) throws MalformedURLException, IOException;

    /**
     * ImageSeriesID is an integer; find these as part of the return document from the Genes method.
     * 
     * @param imageseriesId
     * @return
     */
    public abstract Collection<Image> getImageseries( Integer imageseriesId );

    /**
     * Returns a collection of images from all the imageSeries given (1 imageSeries can have many images)
     * 
     * @param imageSeries
     * @return
     */
    public abstract Collection<Image> getImagesFromImageSeries( Collection<ImageSeries> imageSeries );

    /**
     * REturns the info logging stream
     * 
     * @return
     */
    public abstract PrintStream getInfoOut();

    /**
     * Given a Gene, returns all the image series that contain saggital images for the given gene
     * 
     * @param gene
     * @return
     * @throws IOException
     */
    public abstract Collection<ImageSeries> getRepresentativeSaggitalImages( String gene ) throws IOException;

    /**
     * Is verbose logging on?
     * 
     * @return
     */
    public abstract boolean getVerbose();

    /**
     * @param searchTerm
     * @param out
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    public abstract boolean searchGenes( String searchTerm, OutputStream out ) throws MalformedURLException,
            IOException;

    /**
     * Sets the caching directory
     * 
     * @param s
     */
    public abstract void setCacheDir( String s );

    /**
     * Turn caching on or off
     * 
     * @param v
     */
    public abstract void setCaching( boolean v );

    /**
     * @param out
     */
    public abstract void setErrOut( PrintStream out );

    /**
     * @param out
     */
    public abstract void setInfoOut( PrintStream out );

    /**
     * @param v
     */
    public abstract void setVerbose( boolean v );

}
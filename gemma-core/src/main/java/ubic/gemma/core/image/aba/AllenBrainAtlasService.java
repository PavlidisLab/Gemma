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

import org.w3c.dom.Document;
import ubic.gemma.model.genome.Gene;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;

/**
 * @author paul
 */
@SuppressWarnings("unused") // Possible external use
public interface AllenBrainAtlasService {

    /**
     * http://api.brain-map.org/api/v2/data
     */
    String API_BASE_URL = "http://api.brain-map.org/api/v2";
    /**
     * /Gene/query.xml?criteria=rma::criteria,[entrez_id$eq**NCBI_ID**]";
     */
    String GET_GENE_URL = "/data/Gene/query.xml?criteria=rma::criteria,[entrez_id$eq@]";
    /**
     * /SectionDataSet/query.xml?criteria=rma::criteria,genes[entrez_id$eq**NCBI_ID**]
     */
    String GET_IMAGESERIES_URL = "/data/SectionDataSet/query.xml?criteria=rma::criteria,genes[entrez_id$eq@],rma::include,section_images";
    /**
     * /aba/api/neuroblast/[structure]/[imageseriesid].xml
     */
    String GET_NEUROBLAST_URL = "/aba/api/neuroblast/@/@.xml"; //FIXME
    /**
     * /aba/api/neuroblast/[structure]/[imageseriesid]/[Sagittal | Coronal].xml
     */
    String GET_NEUROBLAST_PLANE_URL = "/aba/api/neuroblast/@/@/@.xml";//FIXME
    /**
     * /aba/api/expression/[imageSeriesId].sva
     */
    String GET_EXPRESSION_VOLUME_URL = "/aba/api/expression/@.sva";//FIXME
    /**
     * /aba/api/expression/imageseries/[imageSeriesId].xml
     */
    String GET_EXPRESSION_INFO_URL = "/aba/api/expression/imageseries/@.xml";//FIXME
    /**
     * /aba/api/ara/[Sagittal | Coronal].xml
     */
    String GET_ATLAS_INFO_URL = "/aba/api/ara/@.xml";//FIXME
    /**
     * /aba/api/atlas/map/[imageseriesid].map
     */
    String GET_ATLAS_IMAGE_MAP_URL = "/aba/api/atlas/map/@.map";//FIXME
    /**
     * /aba/api/image/info?path=[the actual path to the image, as recovered from the imageSeries.xml]
     */
    String GET_IMAGE_INFO_BYPATH_URL = "/aba/api/image/info?path=@";//FIXME
    /**
     * /aba/api/image/info/[imageId].xml
     */
    String GET_IMAGE_INFO_BYID_URL = "/aba/api/image/info/@.xml";//FIXME
    /**
     * /image_download/**IMAGE ID**?downsample=**SIZE REDUCTION FACTOR**
     */
    String GET_IMAGE_URL = "/image_download/@?downsample=@";
    /**
     * /aba/api/image?zoom=[tier]&amp;top=[unscaled pixel top]&amp;left=[unscaled pixel left]&amp;width=[actual pixel
     * width]&amp;height=[actual pixel height]&amp;path=[as above]
     */
    String GET_IMAGE_ROI_URL = "/aba/api/image?mime=@&zoom=@&top=@&left=@&width=@&height=@&path=@";//FIXME
    /**
     * /aba/api/gene/search?term=[some text, which will be used in a contains query for symbol, name &amp; aliases]
     */
    String SEARCH_GENE_URL = "/aba/api/gene/search?term=@";//FIXME
    /**
     * For showing details about gene information on the allen brain atlas web site
     */
    String HTML_GENE_DETAILS_URL = "http://mouse.brain-map.org/brain/@.html?ispopup=1";//FIXME

    /**
     * @param imageseriesId image series ID
     * @return a document describing the given ImageSeriesId
     */
    Document getAtlasImageMap( Integer imageseriesId );

    /**
     * @param imageseriesid image series id
     * @param out           output stream
     * @return the associated immage map for the given series id and writes it to the given output stream
     * @throws IOException if there is a problem while manipulating the file
     */
    boolean getAtlasImageMap( Integer imageseriesid, OutputStream out ) throws IOException;

    /**
     * @param plane plane
     * @param out   output stream
     * @return information like width, height &amp; number of tiers (zoom levels) available for the given
     * image.
     * @throws IOException if there is a problem while manipulating the file
     */
    boolean getAtlasInfo( String plane, OutputStream out ) throws IOException;

    String getCacheDir();

    void setCacheDir( String s );

    /**
     * @return Is caching on?
     */
    boolean getCaching();

    /**
     * @param v Turn caching on or off
     */
    void setCaching( boolean v );

    /**
     * @return return std error stream
     */
    PrintStream getErrOut();

    /**
     * @param out set std error stream
     */
    void setErrOut( PrintStream out );

    boolean getExpressionInfo( Integer imageseriesId, OutputStream out ) throws IOException;

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
     * @param imageseriesId image series id
     * @param out           output stream
     * @return success
     * @throws IOException if there is a problem while manipulating the file
     */
    boolean getExpressionVolume( Integer imageseriesId, OutputStream out ) throws IOException;

    /**
     * Returns AbaGene object for a gene symbol. If it fails to find a gene using the given string it tries the first
     * letter capitalized string.
     *
     * @param gene gene to look up in ABA.
     * @return AbaGene.
     * @throws IOException if there is a problem while manipulating the file.
     */
    AbaGene getGene( Gene gene ) throws IOException;

    /**
     * Given a valid official symbol for a gene (case sensitive) returns an allen brain atals gene details URL
     *
     * @param gene gene symbol
     * @return an allen brain atlas gene details URL
     */
    String getGeneUrl( String gene );

    /**
     * Not all the parameters are required. The simplest url would look like this:
     * http://www.brain-map.org/aba/api/image?zoom=[zoom]&amp;path=[path]. The different options available are: path
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
     * map.org/aba/api/image?zoom=5&amp;top=4000&amp;left=8000&amp;width=300&amp;height=30 0&amp;mime=2&amp;path=/production11/Guk1_04-
     * 0874_25411/zoomify/primary/0207030123/Guk1_70_0207030123_A.aff
     *
     * @param imagePath image path
     * @param zoom      zoom
     * @param top       top
     * @param left      left
     * @param width     width
     * @param height    height
     * @param mimeType  mime type
     * @param out       output stream
     * @return success
     * @throws IOException if there is a problem while manipulating the file
     */
    boolean getImageROI( String imagePath, Integer zoom, Integer top, Integer left, Integer width, Integer height,
            Integer mimeType, OutputStream out ) throws IOException;

    /**
     *
     * @param gene the gene to get the series for.
     * @return collection of images
     */
    ImageSeries getImageSeries( Gene gene );

    /**
     * Returns a collection of images from all the imageSeries given (1 imageSeries can have many images)
     *
     * @param imageSeries image series
     * @return collection of images
     */
    Collection<Image> getImagesFromImageSeries( Collection<ImageSeries> imageSeries );

    /**
     * @return the info logging stream
     */
    PrintStream getInfoOut();

    void setInfoOut( PrintStream out );

    /**
     * Given a Gene, returns all the image series that contain saggital images for the given gene
     *
     * @param gene gene to look for
     * @return all the image series that contain saggital images for the given gene
     * @throws IOException if there is a problem while manipulating the file
     */
    Collection<ImageSeries> getRepresentativeSaggitalImages( Gene gene ) throws IOException;

    /**
     * @return Is verbose logging on?
     */
    boolean getVerbose();

    void setVerbose( boolean v );

    boolean searchGenes( String searchTerm, OutputStream out ) throws IOException;

}
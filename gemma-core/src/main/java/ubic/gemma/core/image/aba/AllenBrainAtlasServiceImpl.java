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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ubic.gemma.core.loader.entrez.pubmed.XMLUtils;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.persistence.util.Settings;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashSet;

/**
 * Acts as a convenient front end to the Allen Brain Atlas REST (web) services Used the ABAApi.java as the original
 * template for this Service (found in ABA demo code). For the most current API regarding these methods go to
 * <a href="http://community.brain-map.org/confluence/display/DataAPI/Home">brain map DATA API web page</a>
 * NO AJAX Methods directly exposed by this service.
 *
 * @author kelsey
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // Possible external use
@Component
public class AllenBrainAtlasServiceImpl implements AllenBrainAtlasService {

    private static final String ABA_CACHE = "/abaCache/";

    private static final Log log = LogFactory.getLog( AllenBrainAtlasServiceImpl.class.getName() );

    protected PrintStream infoOut;
    protected PrintStream errOut;
    protected boolean verbose;
    protected boolean useFileCache;
    protected String cacheDir;

    public AllenBrainAtlasServiceImpl() {
        this.initDefaults();
    }

    @Override
    public Document getAtlasImageMap( Integer imageSeriesId ) {
        File outputFile = this.getFile( "atlasImageMap" + imageSeriesId.toString() );
        Document atlasImageMapDoc = null;
        FileInputStream in = null;

        try (FileOutputStream out = new FileOutputStream( outputFile )) {
            this.getAtlasImageMap( imageSeriesId, out );

            in = new FileInputStream( outputFile );
            atlasImageMapDoc = XMLUtils.openAndParse( in );
        } catch ( ParserConfigurationException | SAXException | IOException pce ) {
            AllenBrainAtlasServiceImpl.log.error( pce );
        } finally {
            if ( in != null ) {
                try {
                    in.close();
                } catch ( IOException e ) {
                    AllenBrainAtlasServiceImpl.log.error( "Failed to close FileInputStream" );
                }
            }
        }

        return atlasImageMapDoc;

    }

    @Override
    public boolean getAtlasImageMap( Integer imageSeriesId, OutputStream out ) throws IOException {

        String args[] = { imageSeriesId.toString() };
        String getImageMapUrl = this.buildUrlString( AllenBrainAtlasService.GET_ATLAS_IMAGE_MAP_URL, args );

        return ( this.doPageDownload( getImageMapUrl, out ) );
    }

    @Override
    public boolean getAtlasInfo( String plane, OutputStream out ) throws IOException {

        String args[] = { plane };
        String getAtlasInfoUrl = this.buildUrlString( AllenBrainAtlasService.GET_ATLAS_INFO_URL, args );

        return ( this.doPageDownload( getAtlasInfoUrl, out ) );
    }

    @Override
    public String getCacheDir() {
        return ( this.cacheDir );
    }

    @Override
    public void setCacheDir( String s ) {
        this.cacheDir = s;
    }

    @Override
    public boolean getCaching() {
        return ( this.useFileCache );
    }

    @Override
    public void setCaching( boolean v ) {
        this.useFileCache = v;
    }

    @Override
    public PrintStream getErrOut() {
        return ( this.errOut );
    }

    @Override
    public void setErrOut( PrintStream out ) {
        this.errOut = out;
    }

    @Override
    public boolean getExpressionInfo( Integer imageSeriesId, OutputStream out ) throws IOException {

        String args[] = { imageSeriesId.toString() };
        String getExpressionInfoUrl = this.buildUrlString( AllenBrainAtlasService.GET_EXPRESSION_INFO_URL, args );

        return ( this.doPageDownload( getExpressionInfoUrl, out ) );
    }

    @Override
    public boolean getExpressionVolume( Integer imageSeriesId, OutputStream out ) throws IOException {

        String args[] = { imageSeriesId.toString() };
        String getVolumeUrl = this.buildUrlString( AllenBrainAtlasService.GET_EXPRESSION_VOLUME_URL, args );

        return ( this.doPageDownload( getVolumeUrl, out ) );
    }

    @Override
    public AbaGene getGene( Gene gene ) throws IOException {
        AbaGene result = this.fetchGene( gene );
        if ( result == null ) {
            AllenBrainAtlasServiceImpl.log.info( "Gene " + gene.getOfficialSymbol() + ", ID " + gene.getId()
                    + " could not be retrieved aba." );
        }
        return result;
    }

    @Override
    public String getGeneUrl( String gene ) {
        return AllenBrainAtlasService.HTML_GENE_DETAILS_URL.replaceFirst( "@", this.correctCase( gene ) );
    }

    @Override
    public boolean getImageROI( String imagePath, Integer zoom, Integer top, Integer left, Integer width,
            Integer height, Integer mimeType, OutputStream out ) throws IOException {

        String args[] = { mimeType.toString(), zoom.toString(), top.toString(), left.toString(), width.toString(),
                height.toString(), imagePath };
        String getImageUrl = this.buildUrlString( AllenBrainAtlasService.GET_IMAGE_ROI_URL, args );

        return ( this.doPageDownload( getImageUrl, out ) );
    }

    @Override
    public ImageSeries getImageSeries( Gene gene ) {

        File outputFile = this.getFile( "ImageseriesId_" + gene.getNcbiGeneId().toString() );
        Document imageSeriesDoc;

        try (FileOutputStream out = new FileOutputStream( outputFile )) {
            this.getImageSeries( gene, out );
        } catch ( Exception e ) {
            AllenBrainAtlasServiceImpl.log.error( e.getMessage(), e.getCause() );
            return null;
        }

        try (FileInputStream input = new FileInputStream( outputFile )) {
            imageSeriesDoc = XMLUtils.openAndParse( input );
        } catch ( Exception e ) {
            AllenBrainAtlasServiceImpl.log.error( e.getMessage(), e.getCause() );
            return null;
        }

        NodeList sectionDataSets = imageSeriesDoc.getChildNodes().item( 0 ).getChildNodes().item( 0 ).getChildNodes();

        for ( int i = 0; i < sectionDataSets.getLength(); i++ ) {
            Node node = sectionDataSets.item( i );
            if ( node.getNodeName().equals( "section-data-set" ) ) {
                Integer id = Integer.parseInt( XMLUtils.extractOneChildText( node, "id" ) );

                ImageSeries is = new ImageSeries( id );
                is.setImages( this.getImageSeriesImages(
                        XMLUtils.extractOneChild( node, "section-images" ).getChildNodes() ) );
                return is;
            }
        }

        AllenBrainAtlasServiceImpl.log
                .error( "Image series for gene ID " + gene.getId() + " could not be retrieved from ABA." );
        return null;
    }

    @Override
    public Collection<Image> getImagesFromImageSeries( Collection<ImageSeries> imageSeries ) {

        Collection<Image> representativeImages = new HashSet<>();

        if ( imageSeries != null ) {
            for ( ImageSeries is : imageSeries ) {
                if ( is.getImages() == null )
                    continue;

                for ( Image img : is.getImages() ) {
                    // Convert the urls into fully qualified ones for ez displaying
                    String args[] = { img.getId().toString(), "5" };
                    img.setDownloadExpressionPath( this.buildUrlString( AllenBrainAtlasService.GET_IMAGE_URL, args ) );
                    img.setExpressionThumbnailUrl(
                            AllenBrainAtlasService.API_BASE_URL + img.getExpressionThumbnailUrl() );
                    representativeImages.add( img );
                }
            }
        }

        return representativeImages;

    }

    @Override
    public PrintStream getInfoOut() {
        return ( this.infoOut );
    }

    @Override
    public void setInfoOut( PrintStream out ) {
        this.infoOut = out;
    }

    @Override
    public Collection<ImageSeries> getRepresentativeSaggitalImages( Gene gene ) {

        Collection<ImageSeries> representativeSaggitalImages = new HashSet<>();

        ImageSeries is = this.getImageSeries( gene );

        Collection<Image> representativeImages = new HashSet<>();

        for ( Image img : is.getImages() ) {
            if ( img != null ) { // && ( 2600 > img.getPosition() ) && ( img.getPosition() > 2200 )
                representativeImages.add( img );
            }
        }

        if ( representativeImages.isEmpty() )
            return null;

        // Only add if there is something to add
        is.setImages( representativeImages );
        representativeSaggitalImages.add( is );

        return representativeSaggitalImages;

    }

    @Override
    public boolean getVerbose() {
        return ( this.verbose );
    }

    /*
     * Convenience method for striping out the images from the image series. Also fully qualifies URLs for link to allen
     * brain atlas web site
     */
    @Override
    public void setVerbose( boolean v ) {
        this.verbose = v;
    }

    @Override
    public boolean searchGenes( String searchTerm, OutputStream out ) throws IOException {

        String args[] = { searchTerm };
        String searchGenesUrl = this.buildUrlString( AllenBrainAtlasService.SEARCH_GENE_URL, args );

        return ( this.doPageDownload( searchGenesUrl, out ) );
    }

    /**
     * Given a predefined URL (one of the constants declared in the AllenBrainAtlasService) and a list of arguments will
     * return the correct REST URL for the desired method call
     *
     * @param urlPattern url pattern
     * @param args       arguments
     * @return full url
     */
    protected String buildUrlString( String urlPattern, String args[] ) {

        for ( String arg : args )
            urlPattern = urlPattern.replaceFirst( "@", arg );

        return ( AllenBrainAtlasService.API_BASE_URL + urlPattern );
    }

    protected boolean getGene( Gene gene, OutputStream out ) throws IOException {

        String args[] = { gene.getNcbiGeneId().toString() };
        String getGeneUrl = this.buildUrlString( AllenBrainAtlasService.GET_GENE_URL, args );

        return ( this.doPageDownload( getGeneUrl, out ) );
    }

    protected boolean getImage( Integer imageId, Integer downsample, OutputStream out ) throws IOException {
        String args[] = { imageId.toString(), downsample.toString() };
        String getImageUrl = this.buildUrlString( AllenBrainAtlasService.GET_IMAGE_URL, args );

        return ( this.doPageDownload( getImageUrl, out ) );
    }

    protected boolean getImageInfo( Integer imageId, OutputStream out ) throws IOException {

        String args[] = { imageId.toString() };
        String getImageInfoUrl = this.buildUrlString( AllenBrainAtlasService.GET_IMAGE_INFO_BYID_URL, args );

        return ( this.doPageDownload( getImageInfoUrl, out ) );
    }

    protected boolean getImageInfo( String imagePath, OutputStream out ) throws IOException {

        String args[] = { imagePath };
        String getImageInfoUrl = this.buildUrlString( AllenBrainAtlasService.GET_IMAGE_INFO_BYPATH_URL, args );

        return ( this.doPageDownload( getImageInfoUrl, out ) );
    }

    protected void getImageSeries( Gene gene, OutputStream out ) throws IOException {

        String args[] = { gene.getNcbiGeneId().toString() };
        String getImageSeriesUrl = this.buildUrlString( AllenBrainAtlasService.GET_IMAGESERIES_URL, args );

        this.doPageDownload( getImageSeriesUrl, out );
    }

    protected boolean getNeuroblast( Integer imageSeriesId, String structure, String plane, OutputStream out )
            throws IOException {

        String getNeuroblastUrl;

        if ( plane == null ) {
            String args[] = { structure, imageSeriesId.toString() };
            getNeuroblastUrl = this.buildUrlString( AllenBrainAtlasService.GET_NEUROBLAST_URL, args );
        } else {
            String args[] = { structure, imageSeriesId.toString(), plane };
            getNeuroblastUrl = this.buildUrlString( AllenBrainAtlasService.GET_NEUROBLAST_PLANE_URL, args );
        }

        return ( this.doPageDownload( getNeuroblastUrl, out ) );
    }

    /**
     * Given a gene too look for for will return the corresponding abaGene (useful for finding images)
     *
     * @param gene the gene to look for in ABA.
     * @return ABA gene
     * @throws IOException              when there are IO problems.
     * @throws IllegalArgumentException when the given gene does not have an NCBI ID.
     */
    private AbaGene fetchGene( Gene gene ) throws IOException {

        if ( gene.getNcbiGeneId() == null )
            throw new IllegalArgumentException( "Gene ID " + gene.getId() + " has null NCBI ID. Can not browse ABA." );

        File outputFile = this.getFile( gene.getNcbiGeneId().toString() );
        Document geneDoc = null;

        try (FileOutputStream out = new FileOutputStream( outputFile )) {
            this.getGene( gene, out );
        }

        try (FileInputStream input = new FileInputStream( outputFile )) {
            geneDoc = XMLUtils.openAndParse( input );
        } catch ( ParserConfigurationException | FileNotFoundException e ) {
            AllenBrainAtlasServiceImpl.log.warn( e );
            return null;
        } catch ( SAXException e ) {
            AllenBrainAtlasServiceImpl.log.warn( e );
        }

        if ( geneDoc == null ) {
            AllenBrainAtlasServiceImpl.log.warn( "ABA XML did not contain any data." );
            return null;
        }

        Collection<String> xmlData = XMLUtils.extractTagData( geneDoc, "id" );
        Integer geneId = xmlData.isEmpty() ? null : Integer.parseInt( xmlData.iterator().next() );

        xmlData = XMLUtils.extractTagData( geneDoc, "name" );
        String geneName = xmlData.isEmpty() ? null : xmlData.iterator().next();

        xmlData = XMLUtils.extractTagData( geneDoc, "acronym" );
        String geneSymbol = xmlData.isEmpty() ? null : xmlData.iterator().next();

        xmlData = XMLUtils.extractTagData( geneDoc, "entrez-id" );
        String ncbiAccessionNumber = xmlData.isEmpty() ? null : xmlData.iterator().next();

        String geneUrl = ( geneSymbol == null ) ? null : this.getGeneUrl( geneSymbol );

        if ( geneId == null && geneSymbol == null ) {
            AllenBrainAtlasServiceImpl.log.warn( "ABA XML file did not contain gene ID and symbol." );
            return null;
        }

        AbaGene geneData = new AbaGene( geneId, geneSymbol, geneName, ncbiAccessionNumber, geneUrl, null );

        NodeList idList = geneDoc.getChildNodes().item( 0 ).getChildNodes();

        return geneData;
    }

    private Collection<Image> getImageSeriesImages( NodeList sectionImages ) {
        Collection<Image> results = new HashSet<>();

        for ( int j = 0; j < sectionImages.getLength(); j++ ) {
            this.processImageNode( results, sectionImages.item( j ) );
        }

        return results;
    }

    private void processImageNode( Collection<Image> results, Node imageNode ) {
        if ( !imageNode.getNodeName().equals( "section-image" ) )
            return;

        XmlImageProperties imgProps = new XmlImageProperties( imageNode );

        Integer imageId = imgProps.getImageId();
        String downloadImagePath = imgProps.getDownloadImagePath();

        if ( imageId != null && downloadImagePath != null ) {
            Image img = new Image( imgProps.getDisplayName(), imageId, imgProps.getPosition(),
                    imgProps.getReferenceAtlasIndex(), imgProps.getThumbnailUrl(), imgProps.getZoomifiedNisslUrl(),
                    imgProps.getExpressionThumbnailUrl(), downloadImagePath, imgProps.getDownloadExpressionPath(), 0,
                    0 );
            results.add( img );
        } else {
            AllenBrainAtlasServiceImpl.log
                    .info( "Skipping adding image to collection because necessary data missing after parsing image xml" );
        }
    }

    /**
     * @param geneName gene name
     * @return The allen brain atlas website 1st letter of gene symbol is capitalized, rest are not (webservice is case
     * sensitive)
     */
    private String correctCase( String geneName ) {
        return StringUtils.capitalize( StringUtils.lowerCase( geneName ) );
    }

    private boolean doPageDownload( String urlString, OutputStream out ) throws IOException {

        URL url = new URL( urlString );
        try (DataInputStream in = this.getInput( url )) {
            if ( in == null )
                return false;

            this.transferData( in, out );

            return true;
        }
    }

    private DataInputStream getCachedFile( String cachedName ) throws FileNotFoundException {
        return ( new DataInputStream( new FileInputStream( cachedName ) ) );
    }

    private File getFile( String fileName ) {

        File outputFile = new File( this.cacheDir + "aba_" + fileName + ".xml" );

        if ( outputFile.exists() ) {
            EntityUtils.deleteFile( outputFile );

            // wait for file to be deleted before proceeding
            int i = 5;
            while ( ( i > 0 ) && ( outputFile.exists() ) ) {
                try {
                    Thread.sleep( 1000 );
                } catch ( InterruptedException ie ) {
                    AllenBrainAtlasServiceImpl.log.error( ie );
                }
                i--;
            }

        }
        return outputFile;

    }

    private DataInputStream getInput( URL url ) throws IOException {

        if ( this.useFileCache ) {
            String cachedName = this.cacheDir + "/" + url.toString().replace( "/", "_" );
            File f = new File( cachedName );
            if ( f.exists() ) {
                if ( this.verbose )
                    this.infoOut.println( "Using cached file '" + cachedName + "'" );

                return ( this.getCachedFile( cachedName ) );
            }
        }

        HttpURLConnection conn = ( HttpURLConnection ) url.openConnection();
        conn.connect();
        DataInputStream in = new DataInputStream( conn.getInputStream() );

        if ( conn.getResponseCode() != HttpURLConnection.HTTP_OK ) {
            this.errOut.println( conn.getResponseMessage() );
            return ( null );
        }

        if ( this.verbose )
            this.showHeader( conn );

        if ( this.useFileCache ) {
            String cachedName = this.cacheDir + "/" + url.toString().replace( "/", "_" );
            try (FileOutputStream out = new FileOutputStream( new File( cachedName ) )) {
                this.transferData( in, out );
                return ( this.getCachedFile( cachedName ) );
            }
        }

        return ( in );
    }

    private void initDefaults() {
        this.verbose = false;
        this.useFileCache = false;
        this.cacheDir = Settings.getString( "gemma.appdata.home" ) + AllenBrainAtlasServiceImpl.ABA_CACHE;
        File abaCacheDir = new File( this.cacheDir );
        if ( !( abaCacheDir.exists() && abaCacheDir.canRead() ) ) {
            AllenBrainAtlasServiceImpl.log
                    .warn( "Attempting to create aba cache directory in '" + this.cacheDir + "'" );
            EntityUtils.mkdirs( abaCacheDir );
        }

        this.infoOut = System.out;
        this.errOut = System.err;
    }

    private void showHeader( URLConnection url ) {

        this.infoOut.println();
        this.infoOut.println( "URL              : " + url.getURL().toString() );
        this.infoOut.println( "Content-Type     : " + url.getContentType() );
        this.infoOut.println( "Content-Length   : " + url.getContentLength() );
        if ( url.getContentEncoding() != null )
            this.infoOut.println( "Content-Encoding : " + url.getContentEncoding() );
    }

    private void transferData( DataInputStream in, OutputStream out ) throws IOException {
        // This is whacked. There must be a better way than throwing an exception.
        boolean EOF = false;
        while ( !EOF ) {
            try {
                out.write( in.readUnsignedByte() );
            } catch ( EOFException eof ) {
                EOF = true;
            }
        }
    }

    private class XmlImageProperties {
        private Integer imageId;
        private String displayName;
        private Integer position = 0;
        private Integer referenceAtlasIndex = 0;
        private String thumbnailUrl;
        private String zoomifiedNisslUrl;
        private String expressionThumbnailUrl;
        private String downloadImagePath;
        private String downloadExpressionPath;

        public XmlImageProperties( Node image ) {
            NodeList childNodes = image.getChildNodes();

            for ( int m = 0; m < childNodes.getLength(); m++ ) {

                Node c = childNodes.item( m );
                String n = c.getNodeName();
                switch ( n ) {
                    case "#text":
                        continue; // added to make faster as half of comparisons are empty nodes of this type!
                    case "id":
                        imageId = Integer.parseInt( XMLUtils.getTextValue( ( Element ) c ) );
                        break;
                    //                    case "imagedisplayname":
                    //                        displayName = XMLUtils.getTextValue( ( Element ) c );
                    //                        break;
                    //                    case "position":
                    //                        position = Integer.parseInt( XMLUtils.getTextValue( ( Element ) c ) );
                    //                        break;
                    //                    case "referenceatlasindex":
                    //                        referenceAtlasIndex = Integer.parseInt( XMLUtils.getTextValue( ( Element ) c ) );
                    //                        break;
                    //                    case "thumbnailurl":
                    //                        thumbnailUrl = XMLUtils.getTextValue( ( Element ) c );
                    //                        break;
                    //                    case "zoomifiednisslurl":
                    //                        zoomifiedNisslUrl = XMLUtils.getTextValue( ( Element ) c );
                    //                        break;
                    //                    case "expressthumbnailurl":
                    //                        expressionThumbnailUrl = XMLUtils.getTextValue( ( Element ) c );
                    //                        break;
                    case "path":
                        downloadImagePath = XMLUtils.getTextValue( ( Element ) c );
                        break;
                    case "expression-path":
                        downloadExpressionPath = XMLUtils.getTextValue( ( Element ) c );
                        break;
                    default:
                }
            }
        }

        public Integer getImageId() {
            return imageId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Integer getPosition() {
            return position;
        }

        public Integer getReferenceAtlasIndex() {
            return referenceAtlasIndex;
        }

        public String getThumbnailUrl() {
            return thumbnailUrl;
        }

        public String getZoomifiedNisslUrl() {
            return zoomifiedNisslUrl;
        }

        public String getExpressionThumbnailUrl() {
            return expressionThumbnailUrl;
        }

        public String getDownloadImagePath() {
            return downloadImagePath;
        }

        public String getDownloadExpressionPath() {
            return downloadExpressionPath;
        }

    }
}

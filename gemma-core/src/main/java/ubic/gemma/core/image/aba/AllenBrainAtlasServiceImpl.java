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
import ubic.gemma.persistence.util.Settings;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashSet;

/**
 * Acts as a convenient front end to the Allen Brain Atlas REST (web) services Used the ABAapi.java as the original
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
        initDefaults();
    }

    @Override
    public Document getAtlasImageMap( Integer imageseriesId ) {
        File outputFile = getFile( "atlasImageMap" + imageseriesId.toString() );
        Document atlasImageMapDoc = null;
        FileInputStream in = null;

        try (FileOutputStream out = new FileOutputStream( outputFile )) {
            this.getAtlasImageMap( imageseriesId, out );

            in = new FileInputStream( outputFile );
            atlasImageMapDoc = XMLUtils.openAndParse( in );
        } catch ( ParserConfigurationException | SAXException | IOException pce ) {
            log.error( pce );
        } finally {
            if ( in != null ) {
                try {
                    in.close();
                } catch ( IOException e ) {
                    log.error( "Failed to close FileInputStream" );
                }
            }
        }

        return atlasImageMapDoc;

    }

    @Override
    public boolean getAtlasImageMap( Integer imageseriesid, OutputStream out ) throws IOException {

        String args[] = { imageseriesid.toString() };
        String getImageMapUrl = buildUrlString( GET_ATLAS_IMAGE_MAP_URL, args );

        return ( doPageDownload( getImageMapUrl, out ) );
    }

    @Override
    public boolean getAtlasInfo( String plane, OutputStream out ) throws IOException {

        String args[] = { plane };
        String getAtlasInfoUrl = buildUrlString( GET_ATLAS_INFO_URL, args );

        return ( doPageDownload( getAtlasInfoUrl, out ) );
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
    public boolean getExpressionInfo( Integer imageseriesId, OutputStream out ) throws IOException {

        String args[] = { imageseriesId.toString() };
        String getExpressionInfoUrl = buildUrlString( GET_EXPRESSION_INFO_URL, args );

        return ( doPageDownload( getExpressionInfoUrl, out ) );
    }

    @Override
    public boolean getExpressionVolume( Integer imageseriesId, OutputStream out ) throws IOException {

        String args[] = { imageseriesId.toString() };
        String getVolumeUrl = buildUrlString( GET_EXPRESSION_VOLUME_URL, args );

        return ( doPageDownload( getVolumeUrl, out ) );
    }

    @Override
    public AbaGene getGene( String givenGene ) throws IOException {
        AbaGene result = getGene( givenGene, false );
        if ( result == null ) {
            result = getGene( givenGene, true );
        }
        if ( result == null ) {
            log.info( givenGene + " not found in aba" );
        }
        return result;
    }

    @Override
    public String getGeneUrl( String gene ) {
        return HTML_GENE_DETAILS_URL.replaceFirst( "@", this.correctCase( gene ) );
    }

    @Override
    public boolean getImageROI( String imagePath, Integer zoom, Integer top, Integer left, Integer width,
            Integer height, Integer mimeType, OutputStream out ) throws IOException {

        String args[] = { mimeType.toString(), zoom.toString(), top.toString(), left.toString(), width.toString(),
                height.toString(), imagePath };
        String getImageUrl = buildUrlString( GET_IMAGE_ROI_URL, args );

        return ( doPageDownload( getImageUrl, out ) );
    }

    @Override
    public Collection<Image> getImageseries( Integer imageseriesId ) {

        File outputFile = getFile( "ImageseriesId_" + imageseriesId.toString() );
        Document imageSeriesDoc;

        try (FileOutputStream out = new FileOutputStream( outputFile )) {
            this.getImageseries( imageseriesId, out );
        } catch ( Exception e ) {
            log.error( e.getMessage(), e.getCause() );
            return null;
        }

        try (FileInputStream input = new FileInputStream( outputFile )) {
            imageSeriesDoc = XMLUtils.openAndParse( input );
        } catch ( Exception e ) {
            log.error( e.getMessage(), e.getCause() );
            return null;
        }

        NodeList idList = imageSeriesDoc.getChildNodes().item( 0 ).getChildNodes();
        Collection<Image> results = new HashSet<>();

        for ( int i = 0; i < idList.getLength(); i++ ) {
            Node item = idList.item( i );

            if ( !item.getNodeName().equals( "images" ) )
                continue;

            NodeList imageList = item.getChildNodes();

            for ( int j = 0; j < imageList.getLength(); j++ ) {

                Node image = imageList.item( j );

                if ( !image.getNodeName().equals( "image" ) )
                    continue;

                NodeList childNodes = image.getChildNodes();

                Integer imageId = null;
                String displayName = null;
                Integer position = null;
                Integer referenceAtlasIndex = null;
                String thumbnailUrl = null;
                String zoomifiedNisslUrl = null;
                String expressionThumbnailUrl = null;
                String downloadImagePath = null;
                String downloadExpressionPath = null;

                for ( int m = 0; m < childNodes.getLength(); m++ ) {

                    Node c = childNodes.item( m );

                    // log.info( c.getNodeName() );
                    String n = c.getNodeName();
                    try {
                        switch ( n ) {
                            case "#text":
                                continue; // added to make faster as half of comparisions are empty nodes of this type!
                            case "imageid":
                                imageId = Integer.parseInt( XMLUtils.getTextValue( ( Element ) c ) );
                                break;
                            case "imagedisplayname":
                                displayName = XMLUtils.getTextValue( ( Element ) c );
                                break;
                            case "position":
                                position = Integer.parseInt( XMLUtils.getTextValue( ( Element ) c ) );
                                break;
                            case "referenceatlasindex":
                                referenceAtlasIndex = Integer.parseInt( XMLUtils.getTextValue( ( Element ) c ) );
                                break;
                            case "thumbnailurl":
                                thumbnailUrl = XMLUtils.getTextValue( ( Element ) c );
                                break;
                            case "zoomifiednisslurl":
                                zoomifiedNisslUrl = XMLUtils.getTextValue( ( Element ) c );
                                break;
                            case "expressthumbnailurl":
                                expressionThumbnailUrl = XMLUtils.getTextValue( ( Element ) c );
                                break;
                            case "downloadImagePath":
                                downloadImagePath = XMLUtils.getTextValue( ( Element ) c );
                                break;
                            case "downloadExpressionPath":
                                downloadExpressionPath = XMLUtils.getTextValue( ( Element ) c );
                                break;
                            default:
                        }
                    } catch ( IOException ioe ) {
                        log.warn( ioe );
                    }
                } // for loop

                if ( imageId != null && downloadImagePath != null ) {
                    Image img = new Image( displayName, imageId, position, referenceAtlasIndex, thumbnailUrl,
                            zoomifiedNisslUrl, expressionThumbnailUrl, downloadImagePath, downloadExpressionPath, 0,
                            0 );
                    results.add( img );
                } else {
                    log.info(
                            "Skipping adding image to collection cause necessary data missing after parsing image xml" );
                }

            }
        }

        return results;

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
                    String args[] = { "2", "2", img.getDownloadExpressionPath() };
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
    public Collection<ImageSeries> getRepresentativeSaggitalImages( String gene ) throws IOException {

        AbaGene grin1 = this.getGene( gene );
        if ( grin1 == null )
            return null;

        Collection<ImageSeries> representativeSaggitalImages = new HashSet<>();

        for ( ImageSeries is : grin1.getImageSeries() ) {
            if ( is.getPlane().equalsIgnoreCase( "sagittal" ) ) {

                Collection<Image> images = this.getImageseries( is.getImageSeriesId() );
                Collection<Image> representativeImages = new HashSet<>();

                for ( Image img : images ) {
                    if ( ( 2600 > img.getPosition() ) && ( img.getPosition() > 2200 ) ) {
                        representativeImages.add( img );
                    }
                }

                if ( representativeImages.isEmpty() )
                    continue;

                // Only add if there is something to add
                is.setImages( representativeImages );
                representativeSaggitalImages.add( is );
            }
        }
        // grin1.setImageSeries( representativeSaggitalImages );

        return representativeSaggitalImages;

    }

    @Override
    public boolean getVerbose() {
        return ( this.verbose );
    }

    /*
     * Convieniece method for striping out the images from the image series. Also fully qaulifies URLs for link to allen
     * brain atlas web site @param imageSeries @return
     */

    @Override
    public void setVerbose( boolean v ) {
        this.verbose = v;
    }

    @Override
    public boolean searchGenes( String searchTerm, OutputStream out ) throws IOException {

        String args[] = { searchTerm };
        String searchGenesUrl = buildUrlString( SEARCH_GENE_URL, args );

        return ( doPageDownload( searchGenesUrl, out ) );
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

        return ( API_BASE_URL + urlPattern );
    }

    /**
     * Given a gene too look for for will return the corresponding abaGene (useful for finding images)
     *
     * @param givenGene symbol of gene that will be used to search ABA.
     * @return ABA gene
     */
    protected AbaGene getGene( String givenGene, boolean correctCase ) throws IOException {
        String gene = givenGene;

        if ( correctCase ) {
            gene = correctCase( gene );
        }

        File outputFile = getFile( gene );
        Document geneDoc = null;

        try (FileOutputStream out = new FileOutputStream( outputFile )) {
            this.getGene( gene, out );
        }

        try (FileInputStream input = new FileInputStream( outputFile )) {
            geneDoc = XMLUtils.openAndParse( input );
        } catch ( ParserConfigurationException pce ) {
            log.warn( pce );
            return null;
        } catch ( SAXException se ) {
            log.warn( se );
        } catch ( FileNotFoundException fnfe ) {
            return null;
        }

        if ( geneDoc == null ) {
            return null;
        }

        Collection<String> xmlData = XMLUtils.extractTagData( geneDoc, "geneid" );
        Integer geneId = xmlData.isEmpty() ? null : Integer.parseInt( xmlData.iterator().next() );

        xmlData = XMLUtils.extractTagData( geneDoc, "genename" );
        String geneName = xmlData.isEmpty() ? null : xmlData.iterator().next();

        xmlData = XMLUtils.extractTagData( geneDoc, "genesymbol" );
        String geneSymbol = xmlData.isEmpty() ? null : xmlData.iterator().next();

        xmlData = XMLUtils.extractTagData( geneDoc, "entrezgeneid" );
        Integer entrezGeneId = xmlData.isEmpty() ? null : Integer.parseInt( xmlData.iterator().next() );

        xmlData = XMLUtils.extractTagData( geneDoc, "ncbiaccessionnumber" );
        String ncbiAccessionNumber = xmlData.isEmpty() ? null : xmlData.iterator().next();

        String geneUrl = ( geneSymbol == null ) ? null : this.getGeneUrl( geneSymbol );

        if ( geneId == null && geneSymbol == null )
            return null;

        AbaGene geneData = new AbaGene( geneId, geneSymbol, geneName, entrezGeneId, ncbiAccessionNumber, geneUrl,
                null );

        NodeList idList = geneDoc.getChildNodes().item( 0 ).getChildNodes();

        // log.debug( "Got " + idList.getLength() );

        for ( int i = 0; i < idList.getLength(); i++ ) {
            Node item = idList.item( i );

            if ( !item.getNodeName().equals( "image-series" ) )
                continue;

            NodeList imageSeriesList = item.getChildNodes();

            for ( int j = 0; j < imageSeriesList.getLength(); j++ ) {

                Node imageSeries = imageSeriesList.item( j );

                NodeList childNodes = imageSeries.getChildNodes();
                Integer imageSeriesId = null;
                String plane = null;

                for ( int m = 0; m < childNodes.getLength(); m++ ) {

                    Node c = childNodes.item( m );

                    // log.info( c.getNodeName() );
                    String n = c.getNodeName();
                    try {
                        switch ( n ) {
                            case "imageseriesid":
                                imageSeriesId = Integer.parseInt( XMLUtils.getTextValue( ( Element ) c ) );
                                break;
                            case "plane":
                                plane = XMLUtils.getTextValue( ( Element ) c );
                                break;
                            default:
                                // Just skip and check the next one.
                        }
                    } catch ( IOException ioe ) {
                        log.warn( ioe );
                    }

                }

                if ( imageSeriesId != null && plane != null ) {
                    ImageSeries is = new ImageSeries( imageSeriesId, plane );
                    geneData.addImageSeries( is );
                    log.debug( "added image series to gene data" );
                } else {
                    log.debug( "Skipping adding imageSeries to gene cause data missing" );
                }

            }
        }

        return geneData;

    }

    protected boolean getGene( String gene, OutputStream out ) throws IOException {

        String args[] = { gene };
        String getGeneUrl = buildUrlString( GET_GENE_URL, args );

        return ( doPageDownload( getGeneUrl, out ) );
    }

    protected boolean getImage( String imagePath, Integer zoom, Integer mimeType, OutputStream out )
            throws IOException {
        String args[] = { mimeType.toString(), zoom.toString(), imagePath };
        String getImageUrl = buildUrlString( GET_IMAGE_URL, args );

        return ( doPageDownload( getImageUrl, out ) );
    }

    protected boolean getImageInfo( Integer imageId, OutputStream out ) throws IOException {

        String args[] = { imageId.toString() };
        String getImageInfoUrl = buildUrlString( GET_IMAGE_INFO_BYID_URL, args );

        return ( doPageDownload( getImageInfoUrl, out ) );
    }

    protected boolean getImageInfo( String imagePath, OutputStream out ) throws IOException {

        String args[] = { imagePath };
        String getImageInfoUrl = buildUrlString( GET_IMAGE_INFO_BYPATH_URL, args );

        return ( doPageDownload( getImageInfoUrl, out ) );
    }

    protected boolean getImageseries( Integer imageseriesId, OutputStream out ) throws IOException {

        String args[] = { imageseriesId.toString() };
        String getImageseriesUrl = buildUrlString( GET_IMAGESERIES_URL, args );

        return ( doPageDownload( getImageseriesUrl, out ) );
    }

    protected boolean getNeuroblast( Integer imageseriesId, String structure, String plane, OutputStream out )
            throws IOException {

        String getNeuroblastUrl;

        if ( plane == null ) {
            String args[] = { structure, imageseriesId.toString() };
            getNeuroblastUrl = buildUrlString( GET_NEUROBLAST_URL, args );
        } else {
            String args[] = { structure, imageseriesId.toString(), plane };
            getNeuroblastUrl = buildUrlString( GET_NEUROBLAST_PLANE_URL, args );
        }

        return ( doPageDownload( getNeuroblastUrl, out ) );
    }

    /**
     * @param geneName gene name
     * @return The allen brain atlas website 1st letter of gene symbol is capatalized, rest are not (webservice is case
     * sensitive)
     */
    private String correctCase( String geneName ) {
        return StringUtils.capitalize( StringUtils.lowerCase( geneName ) );
    }

    private boolean doPageDownload( String urlString, OutputStream out ) throws IOException {

        URL url = new URL( urlString );
        try (DataInputStream in = getInput( url )) {
            if ( in == null )
                return ( false );

            transferData( in, out );

            return ( true );
        }
    }

    private DataInputStream getCachedFile( String cachedName ) throws FileNotFoundException {
        return ( new DataInputStream( new FileInputStream( cachedName ) ) );
    }

    private File getFile( String fileName ) {

        File outputFile = new File( this.cacheDir + "aba_" + fileName + ".xml" );

        if ( outputFile.exists() ) {
            outputFile.delete();

            // wait for file to be deleted before proceeding
            int i = 5;
            while ( ( i > 0 ) && ( outputFile.exists() ) ) {
                try {
                    Thread.sleep( 1000 );
                } catch ( InterruptedException ie ) {
                    log.error( ie );
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

                return ( getCachedFile( cachedName ) );
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
            showHeader( conn );

        if ( this.useFileCache ) {
            String cachedName = this.cacheDir + "/" + url.toString().replace( "/", "_" );
            try (FileOutputStream out = new FileOutputStream( new File( cachedName ) )) {
                transferData( in, out );
                return ( getCachedFile( cachedName ) );
            }
        }

        return ( in );
    }

    private void initDefaults() {
        this.verbose = false;
        this.useFileCache = false;
        this.cacheDir = Settings.getString( "gemma.appdata.home" ) + ABA_CACHE;
        File abaCacheDir = new File( this.cacheDir );
        if ( !( abaCacheDir.exists() && abaCacheDir.canRead() ) ) {
            log.warn( "Attempting to create aba cache directory in '" + this.cacheDir + "'" );
            abaCacheDir.mkdirs();
        }

        this.infoOut = System.out;
        this.errOut = System.err;
    }

    private void showHeader( URLConnection url ) {

        this.infoOut.println( "" );
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

}

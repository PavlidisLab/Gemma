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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ubic.gemma.core.util.XMLUtils;
import ubic.gemma.model.genome.Gene;

import java.nio.file.Path;
import java.util.*;

/**
 * Acts as a convenient front end to the Allen Brain Atlas REST (web) services Used the ABAApi.java as the original
 * template for this Service (found in ABA demo code). For the most current API regarding these methods go to
 * <a href="http://community.brain-map.org/confluence/display/DataAPI/Home">brain map DATA API web page</a>
 * NO AJAX Methods directly exposed by this service.
 *
 * @author kelsey
 */
@Component
public class AllenBrainAtlasServiceImpl implements AllenBrainAtlasService {

    private static final Log log = LogFactory.getLog( AllenBrainAtlasServiceImpl.class.getName() );
    private final AbaLoader loader;

    @Autowired
    public AllenBrainAtlasServiceImpl( @Qualifier("gemma.appdata.home") Path appDataHome ) {
        this.loader = new AbaLoader( appDataHome.resolve( "abaCache" ), 1200 * 1000 );
    }

    /**
     * Given a gene too look for for will return the corresponding abaGene (useful for finding images)
     *
     * @param  gene                     the gene to look for in ABA.
     * @return ABA gene
     * @throws IllegalArgumentException when the given gene does not have an NCBI ID.
     */
    @Override
    public AbaGene getGene( Gene gene ) {

        if ( gene.getNcbiGeneId() == null )
            throw new IllegalArgumentException( "Gene ID " + gene.getId() + " has null NCBI ID. Can not browse ABA." );

        Document geneDoc = loader.getAbaGeneXML( gene );

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

        if ( geneId == null && geneSymbol == null ) {
            AllenBrainAtlasServiceImpl.log.warn( "ABA XML file did not contain gene ID or symbol." );
            return null;
        }

        return new AbaGene( geneId, geneSymbol, geneName, ncbiAccessionNumber, this.getGeneUrl( gene ),
                this.getSagittalImageSeries( gene ) );
    }

    @Override
    public String getGeneUrl( Gene gene ) {
        return loader.getGeneUrl( gene );
    }

    @Override
    public Collection<Image> getImagesFromImageSeries( Collection<ImageSeries> imageSeries ) {

        Collection<Image> representativeImages = new HashSet<>();

        if ( imageSeries != null ) {
            for ( ImageSeries is : imageSeries ) {
                if ( is.getImages() == null )
                    continue;
                representativeImages.addAll( is.getImages() );
            }
        }

        return representativeImages;

    }

    @Override
    public Collection<ImageSeries> getSagittalImageSeries( Gene gene ) {

        Document imageSeriesDoc = loader.getAbaGeneSagittalImages( gene );

        Collection<ImageSeries> imageSeries = this.extractSeries( imageSeriesDoc );

        return stripImageSeries( imageSeries );
    }

    /**
     * Scans the given series and returns new series only containing the image of the middle section.
     *
     * @param imageSeries the series to strip.
     */
    private Collection<ImageSeries> stripImageSeries( Collection<ImageSeries> imageSeries ) {
        Collection<ImageSeries> strippedSeries = new LinkedList<>();

        for ( ImageSeries is : imageSeries ) {
            List<Image> images = is.getImages();

            if ( !images.isEmpty() ) {

                // Sort by image section number
                Collections.sort( images, new Comparator<Image>() {
                    @Override
                    public int compare( Image image, Image t1 ) {
                        return image.getSectionNumber().compareTo( t1.getSectionNumber() );
                    }
                } );

                // Create new IS and add the middle picture
                ImageSeries newIs = new ImageSeries( is.getImageSeriesId() );
                newIs.addImage( images.get( images.size() / 2 ) );
                strippedSeries.add( newIs );
            }
        }

        return strippedSeries;
    }

    private Collection<ImageSeries> extractSeries( Document imageSeriesDoc ) {
        NodeList sectionDataSets = imageSeriesDoc.getChildNodes().item( 0 ).getChildNodes().item( 0 ).getChildNodes();
        Collection<ImageSeries> series = new LinkedList<>();

        for ( int i = 0; i < sectionDataSets.getLength(); i++ ) {
            Node node = sectionDataSets.item( i );
            if ( node.getNodeName().equals( "section-data-set" ) ) {
                String idText = XMLUtils.extractOneChildText( node, "id" );
                if ( idText == null ) {
                    continue;
                }

                ImageSeries is = new ImageSeries( Integer.parseInt( idText ) );

                Node images = XMLUtils.extractOneChild( node, "section-images" );

                if ( images == null ) {
                    AllenBrainAtlasServiceImpl.log.warn( "ABA series " + idText + " did not contain any images." );
                    continue;
                }

                is.setImages( this.getImageSeriesImages( images.getChildNodes() ) );
                series.add( is );
            }
        }
        return series;
    }

    @SuppressWarnings("ConstantConditions") // Handled with the catch block
    private List<Image> getImageSeriesImages( NodeList sectionImages ) {
        List<Image> results = new LinkedList<>();

        for ( int j = 0; j < sectionImages.getLength(); j++ ) {
            Node imageNode = sectionImages.item( j );

            if ( !imageNode.getNodeName().equals( "section-image" ) ) {
                continue;
            }

            try {
                int id = Integer.parseInt( XMLUtils.extractOneChildText( imageNode, "id" ) );
                Image img = new Image( //
                        "Allen brain map image id " + id, id, //
                        Integer.parseInt( XMLUtils.extractOneChildText( imageNode, "height" ) ), //
                        Integer.parseInt( XMLUtils.extractOneChildText( imageNode, "width" ) ), //
                        XMLUtils.extractOneChildText( imageNode, "path" ), //
                        XMLUtils.extractOneChildText( imageNode, "expression-path" ), //
                        loader.getImageUrl( id ),
                        Integer.parseInt( XMLUtils.extractOneChildText( imageNode, "section-number" ) ) ); //
                results.add( img );

            } catch ( NullPointerException e ) {
                AllenBrainAtlasServiceImpl.log.error( "Processing ABA image node caused an NPA", e );
            }
        }

        return results;
    }

}

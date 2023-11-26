/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
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
package ubic.gemma.core.loader.expression.geo.service;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.core.io.ClassPathResource;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ubic.gemma.core.util.test.Assumptions.assumeThatResourceIsAvailable;

/**
 * @author paul
 */
public class GeoBrowserServiceParseTest {

    @Test
    @Category(SlowTest.class)
    public void testParse() throws Exception {
        assumeThatResourceIsAvailable( "http://www.ncbi.nlm.nih.gov/entrez/query/DTD/eSummary_041029.dtd" );
        String response = "<?xml version=\"1.0\"?>\n<!DOCTYPE eSummaryResult PUBLIC \"-//NLM//DTD eSummaryResult, 29 October 2004//EN\" \"http://www.ncbi.nlm.nih.gov/entrez/query/DTD/eSummary_041029.dtd\">\n"
                + "<eSummaryResult> <DocSum>   <Id>200004595</Id>  <Item Name=\"GDS\" Type=\"String\"></Item>  <Item Name=\"title\" Type=\"String\">Expression profiling of motor cortex in sporadic amyotrophic lateral sclerosis</Item> "
                + " <Item Name=\"summary\" Type=\"String\">We used oligonucleotide microarrays to find differentially"
                + " expressed genes between control subjects and those affected by sporadic amyotrophic lateral sclerosis. Keywords: disease state analysis</Item>   "
                + " <Item Name=\"GPL\" Type=\"String\">1708</Item>  <Item Name=\"GSE\" Type=\"String\">4595</Item>  <Item Name=\"taxon\" Type=\"String\">Homo sapiens</Item>  "
                + "  <Item Name=\"GSM_L\" Type=\"String\"></Item>    <Item Name=\"GSM_titles_L\" Type=\"String\"></Item>   "
                + "  <Item Name=\"entryType\" Type=\"String\">GSE</Item>   "
                + "  <Item Name=\"gdsType\" Type=\"String\">Expression profiling by array</Item>   "
                + "  <Item Name=\"ptechType\" Type=\"String\"></Item>    <Item Name=\"valType\" Type=\"String\"></Item> "
                + " <Item Name=\"SSInfo\" Type=\"String\"></Item>   <Item Name=\"subsetInfo\" Type=\"String\"></Item> "
                + "  <Item Name=\"PDAT\" Type=\"String\">2007/03/26</Item>   <Item Name=\"suppFile\" Type=\"String\"></Item>  "
                + "   <Item Name=\"Samples\" Type=\"List\">       <Item Name=\"Sample\" Type=\"Structure\">    "
                + "       <Item Name=\"Accession\" Type=\"String\">GSM102829</Item>      "
                + "     <Item Name=\"Title\" Type=\"String\">Control 2</Item>       </Item>    "
                + "    <Item Name=\"Sample\" Type=\"Structure\">       "
                + "    <Item Name=\"Accession\" Type=\"String\">GSM102832</Item>     "
                + "      <Item Name=\"Title\" Type=\"String\">Control 5</Item>   "
                + "    </Item>         <Item Name=\"Sample\" Type=\"Structure\">    "
                + "       <Item Name=\"Accession\" Type=\"String\">GSM102835</Item>  "
                + "        <Item Name=\"Title\" Type=\"String\">Control 8</Item>      "
                + " </Item>         <Item Name=\"Sample\" Type=\"Structure\">       "
                + "    <Item Name=\"Accession\" Type=\"String\">GSM102838</Item>       "
                + "    <Item Name=\"Title\" Type=\"String\">Diseased 2</Item>      </Item>   "
                + "      <Item Name=\"Sample\" Type=\"Structure\">        "
                + "   <Item Name=\"Accession\" Type=\"String\">GSM102845</Item>   "
                + "        <Item Name=\"Title\" Type=\"String\">Diseased 7</Item>      </Item>   "
                + "      <Item Name=\"Sample\" Type=\"Structure\">         "
                + "  <Item Name=\"Accession\" Type=\"String\">GSM102830</Item>    "
                + "       <Item Name=\"Title\" Type=\"String\">Control 3</Item>       </Item>   "
                + "      <Item Name=\"Sample\" Type=\"Structure\">  "
                + "         <Item Name=\"Accession\" Type=\"String\">GSM102837</Item>     "
                + "      <Item Name=\"Title\" Type=\"String\">Diseased 1</Item>      </Item>   "
                + "      <Item Name=\"Sample\" Type=\"Structure\">         "
                + "  <Item Name=\"Accession\" Type=\"String\">GSM102840</Item>   "
                + "        <Item Name=\"Title\" Type=\"String\">Diseased 4</Item>      </Item>  "
                + "      <Item Name=\"Sample\" Type=\"Structure\">   "
                + "        <Item Name=\"Accession\" Type=\"String\">GSM102846</Item>   "
                + "        <Item Name=\"Title\" Type=\"String\">Diseased 8</Item>      </Item>     "
                + "    <Item Name=\"Sample\" Type=\"Structure\">     "
                + "      <Item Name=\"Accession\" Type=\"String\">GSM102828</Item>       "
                + "    <Item Name=\"Title\" Type=\"String\">Control 1</Item>       </Item>     "
                + "    <Item Name=\"Sample\" Type=\"Structure\">      "
                + "     <Item Name=\"Accession\" Type=\"String\">GSM102831</Item>    "
                + "       <Item Name=\"Title\" Type=\"String\">Control 4</Item>       </Item>     "
                + "    <Item Name=\"Sample\" Type=\"Structure\">    "
                + "      <Item Name=\"Accession\" Type=\"String\">GSM102834</Item>   "
                + "       <Item Name=\"Title\" Type=\"String\">Control 7</Item>       </Item>    "
                + "     <Item Name=\"Sample\" Type=\"Structure\">    "
                + "      <Item Name=\"Accession\" Type=\"String\">GSM102841</Item>     "
                + "     <Item Name=\"Title\" Type=\"String\">Diseased 5</Item>      </Item>    "
                + "     <Item Name=\"Sample\" Type=\"Structure\">      "
                + "     <Item Name=\"Accession\" Type=\"String\">GSM102844</Item>    "
                + "      <Item Name=\"Title\" Type=\"String\">Diseased 6</Item>      </Item>    "
                + "     <Item Name=\"Sample\" Type=\"Structure\">     "
                + "      <Item Name=\"Accession\" Type=\"String\">GSM102847</Item>    "
                + "       <Item Name=\"Title\" Type=\"String\">Diseased 9</Item>      </Item>     "
                + "    <Item Name=\"Sample\" Type=\"Structure\">     "
                + "     <Item Name=\"Accession\" Type=\"String\">GSM102833</Item>    "
                + "       <Item Name=\"Title\" Type=\"String\">Control 6</Item>       </Item>      "
                + "   <Item Name=\"Sample\" Type=\"Structure\">      "
                + "     <Item Name=\"Accession\" Type=\"String\">GSM102836</Item>     "
                + "      <Item Name=\"Title\" Type=\"String\">Control 9</Item>       </Item>    "
                + "     <Item Name=\"Sample\" Type=\"Structure\">     "
                + "      <Item Name=\"Accession\" Type=\"String\">GSM102839</Item>      "
                + "     <Item Name=\"Title\" Type=\"String\">Diseased 3</Item>      </Item>     "
                + "    <Item Name=\"Sample\" Type=\"Structure\">      "
                + "     <Item Name=\"Accession\" Type=\"String\">GSM102849</Item>     "
                + "      <Item Name=\"Title\" Type=\"String\">Diseased 10</Item>         </Item>     "
                + "    <Item Name=\"Sample\" Type=\"Structure\">     "
                + "     <Item Name=\"Accession\" Type=\"String\">GSM102852</Item>    "
                + "       <Item Name=\"Title\" Type=\"String\">Diseased 11</Item>         </Item> "
                + "    </Item>     <Item Name=\"Relations\" Type=\"List\"></Item> "
                + " <Item Name=\"n_samples\" Type=\"Integer\">20</Item>  "
                + "   <Item Name=\"SeriesTitle\" Type=\"String\"></Item> "
                + " <Item Name=\"PlatformTitle\" Type=\"String\"></Item>   "
                + " <Item Name=\"PlatformTaxa\" Type=\"String\"></Item>  "
                + "   <Item Name=\"SamplesTaxa\" Type=\"String\"></Item> "
                + " <Item Name=\"PubMedIds\" Type=\"List\">   "
                + "      <Item Name=\"int\" Type=\"Integer\">17244347</Item>     </Item>  "
                + "   <Item Name=\"Projects\" Type=\"List\"></Item> </DocSum> "
                + " <DocSum>   <Id>100001708</Id>  <Item Name=\"GDS\" Type=\"String\">2080;2824;3268;3429;3432;3433</Item>   "
                + "  <Item Name=\"title\" Type=\"String\">Agilent-012391 Whole Human Genome Oligo Microarray G4112A (Feature Number version)</Item> "
                + " <Item Name=\"summary\" Type=\"String\">This single 44K formatted microarray represents a compiled view of the human genome as it is understood today."
                + " The sequence information used to design this product was derived from a broad survey of well known sources such as RefSeq, Goldenpath, Ensembl, Unigene and others."
                + " The resulting view of the human genome covers 41K unique genes and transcripts which have been verified and optimized by alignment to the human genome assembly "
                + "and by Agilent's Empirical Validation process. Arrays of this design have barcodes that begin with 16012391 or 2512391."
                + " Orientation: Features are numbered numbered Left-to-Right, Top-to-Bottom as scanned by an Agilent scanner (barcode on the left, DNA on the back surface,"
                + " scanned through the glass), matching the FeatureNum output from Agilent's Feature Extraction software. The ID column represents the Agilent "
                + "Feature Extraction feature number. Rows and columns are numbered as scanned by an Axon Scanner (barcode on the bottom, DNA on the front surface). To match data scanned on an Axon scanner, "
                + "use the RefNumber column contained in the Agilent-provided GAL file as the ID_REF column in sample submissions. "
                + "*** A different version of this platform with the Agilent Probe names in the ID column is assigned accession number GPL6848. Protocol: see manufacturer's web site at http://www.agilent.com/</Item>   "
                + "  <Item Name=\"GPL\" Type=\"String\">1708</Item> "
                + " <Item Name=\"GSE\" Type=\"String\">2740;4809;4864;5272;5346;5966;6711;7190;8347;8511;8845;9709;9815;10198;11223;11422;12164;14063;15956;18276;18372;20062;20191;25303;25534;2035;4214;4763;7215;7315;"
                + "7469;8873;9014;10621;11003;11064;12154;12244;12449;12472;15110;15862;16957;18188;"
                + "20582;20881;3155;4595;4707;4823;4906;5176;5350;5546;7316;7329;7413;7512;8190;9170;9578;10195;10613;11108;11423;"
                + "11429;11975;12428;13029;15356;15838;15966;16255;23851;25143;25175;4117;4901;6427;7317;7410;7423;7960;8834;8917;"
                + "9637;10601;10704;12391;12622;13673;14466;15112;15363;15739;17011;18229;20147;20350;23611;24151;24639</Item>  "
                + "   <Item Name=\"taxon\" Type=\"String\">Homo sapiens</Item> "
                + "   <Item Name=\"GSM_L\" Type=\"String\"></Item>    <Item Name=\"GSM_titles_L\" Type=\"String\"></Item>   "
                + "  <Item Name=\"entryType\" Type=\"String\">GPL</Item>     <Item Name=\"gdsType\" Type=\"String\"></Item> "
                + " <Item Name=\"ptechType\" Type=\"String\">in situ oligonucleotide</Item>  "
                + "   <Item Name=\"valType\" Type=\"String\"></Item>  <Item Name=\"SSInfo\" Type=\"String\"></Item> "
                + "  <Item Name=\"subsetInfo\" Type=\"String\"></Item>   <Item Name=\"PDAT\" Type=\"String\">2004/11/17</Item>   "
                + "<Item Name=\"suppFile\" Type=\"String\"></Item>     <Item Name=\"Samples\" Type=\"List\"></Item> "
                + "   <Item Name=\"Relations\" Type=\"List\">         <Item Name=\"Relation\" Type=\"Structure\">     "
                + "        <Item Name=\"RelationType\" Type=\"String\">Alternative to</Item>      "
                + "     <Item Name=\"TargetObject\" Type=\"String\">GPL6848</Item>      </Item>     </Item>  "
                + "   <Item Name=\"n_samples\" Type=\"Integer\">2758</Item>   <Item Name=\"SeriesTitle\" Type=\"String\"></Item> "
                + " <Item Name=\"PlatformTitle\" Type=\"String\"></Item>    <Item Name=\"PlatformTaxa\" Type=\"String\"></Item>  "
                + "   <Item Name=\"SamplesTaxa\" Type=\"String\"></Item>  <Item Name=\"PubMedIds\" Type=\"List\"></Item>"
                + "  <Item Name=\"Projects\" Type=\"List\"></Item> </DocSum>  </eSummaryResult>";
        GeoBrowserServiceImpl serv = new GeoBrowserServiceImpl();
        serv.afterPropertiesSet();
        ArrayDesignService ads = mock( ArrayDesignService.class );
        ExpressionExperimentService ees = mock( ExpressionExperimentService.class );
        serv.arrayDesignService = ads;
        serv.expressionExperimentService = ees;
        serv.formatDetails( response, "" );
        verify( ads ).findByShortName( "GPL1708" );
        verify( ees ).findByShortName( "GSE4595" );
    }

    @Test
    public void testParse2() throws Exception {
        assumeThatResourceIsAvailable( "http://www.ncbi.nlm.nih.gov/entrez/query/DTD/eSummary_041029.dtd" );
        try ( InputStream is = new ClassPathResource( "/data/loader/expression/geo/geo.esummary.test1.xml" ).getInputStream();
                BufferedReader r = new BufferedReader( new InputStreamReader( is ) ) ) {

            String l;
            StringBuilder buf = new StringBuilder();
            while ( ( l = r.readLine() ) != null ) {
                buf.append( l );
            }

            String response = buf.toString();

            GeoBrowserServiceImpl serv = new GeoBrowserServiceImpl();
            serv.afterPropertiesSet();

            ArrayDesignService ads = mock( ArrayDesignService.class );
            ExpressionExperimentService ees = mock( ExpressionExperimentService.class );
            serv.arrayDesignService = ads;
            serv.expressionExperimentService = ees;
            serv.formatDetails( response, "" );
            verify( ads ).findByShortName( "GPL570" );
            verify( ees ).findByShortName( "GSE27128" );
        }
    }

    @Test
    public void testParse3() throws Exception {
        assumeThatResourceIsAvailable( "http://www.ncbi.nlm.nih.gov/entrez/query/DTD/eSummary_041029.dtd" );
        try ( InputStream is = new ClassPathResource( "/data/loader/expression/geo/geo.esummary.test2.xml" ).getInputStream();
                BufferedReader r = new BufferedReader( new InputStreamReader( is ) ) ) {

            String l;
            StringBuilder buf = new StringBuilder();
            while ( ( l = r.readLine() ) != null ) {
                buf.append( l );
            }
            String response = buf.toString();

            GeoBrowserServiceImpl serv = new GeoBrowserServiceImpl();
            serv.afterPropertiesSet();

            ArrayDesignService ads = mock( ArrayDesignService.class );
            ExpressionExperimentService ees = mock( ExpressionExperimentService.class );
            serv.arrayDesignService = ads;
            serv.expressionExperimentService = ees;
            serv.formatDetails( response, "" );
            verify( ads ).findByShortName( "GPL3829" );
            verify( ees ).findByShortName( "GSE21230" );
        }

    }

    @Test
    public void testMINiMLParse() throws Exception {
        ClassPathResource resource = new ClassPathResource( "/data/loader/expression/geo/GSE180363.miniml.xml" );
        GeoBrowser serv = new GeoBrowser();
        GeoRecord rec = new GeoRecord();
        serv.parseMINiML( rec, serv.parseMiniMLDocument( resource.getURL() ) );
        assertTrue( rec.isSubSeries() );
    }

    @Test
    @Category(SlowTest.class)
    public void testSampleMINiMLParse() throws Exception {
        ClassPathResource resource = new ClassPathResource( "/data/loader/expression/geo/GSE171682.xml" );
        GeoBrowser serv = new GeoBrowser();
        GeoRecord rec = new GeoRecord();
        serv.parseSampleMiNIML( rec, serv.parseMiniMLDocument( resource.getURL() ) );

        assertTrue( rec.getSampleDetails().contains( "colorectal cancer" ) );
        assertTrue( rec.getSampleDetails().contains( "Large intestine" ) );
        assertEquals( "RNA-Seq", rec.getLibraryStrategy() );
    }

}

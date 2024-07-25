/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.core.loader.expression.geo;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.filters.StringInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ubic.basecode.math.StringDistance;
import ubic.basecode.util.StringUtil;
import ubic.gemma.core.loader.entrez.EutilFetch;
import ubic.gemma.core.loader.expression.geo.model.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to handle cases where there are multiple GEO dataset for a single actual experiment. This can occur in at least
 * two ways:
 * <ol>
 * <li>There is a single GSE (e.g., GSE674) but two datasets (GDS472, GDS473). This can happen when there are two
 * different microarrays used such as the "A" and B" HG-U133 Affymetrix arrays. (Each GDS can only refer to a single
 * platform)</li>
 * <li>Rarely, there can be two series, as well as two data sets, for the situation described above. These are
 * 'pathological' (due to incorrect data entry by a user, back in the day) and GEO folks should be removing them
 * eventually.</li>
 * </ol>
 * <p>
 * One major problem is figuring out which samples (GSMs) correspond across the datasets. In the example of GSE674,
 * there are samples like C6-U133A (in GDS472) and C6-133B (in GDS473), which apparently, but not "officially"
 * correspond to the same biological RNA. The difficulty is that there is no fail-proof way to determine which samples
 * match up. We do the best we can by using the edit distance between the sample names. Ties can be a problem but for
 * now the samples are sorted and the first best match is the one kept, on the assumption that corresponding samples
 * will have lower numbers. (that is, sample 12929 will match with 12945, not 12955, if the edit distance among the
 * choices is the same).
 * </p>
 * Another problem is that there is no way to go from GDS--&gt;GSE--&gt;other GDS without scraping the GEO web site.
 *
 * @author pavlidis
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class DatasetCombiner {

    static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    @SuppressWarnings("Annotator")
    private static final String PUNCTUATION_REGEXP = "[()\\s-._]";
    /**
     * Careful, GEO changes this sometimes.
     */
    private static final String GSE_RECORD_REGEXP = "(GSE\\d+)";
    private static final String ENTREZ_GEO_QUERY_URL_SUFFIX = "[Accession]&cmd=search";
    private static final String ENTREZ_GEO_QUERY_URL_BASE = "https://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gds&term=";
    private static final Log log = LogFactory.getLog( DatasetCombiner.class.getName() );
    /**
     * Threshold normalized similarity between two strings before we bother to make a match. The normalized similarity
     * is the ratio between the unnormalized edit distance and the length of the longer of the two strings. This is used
     * as a maximum distance (the pair of descriptors must be at least this close).
     * Setting this correctly is important if there are to be singletons (samples that don't match to others)
     */
    private static final double SIMILARITY_THRESHOLD = 0.5;
    /**
     * Used to help ignore identifiers of microarrays in sample titles.
     */
    private static final Map<String, Collection<String>> microarrayNameStrings = new HashMap<>();

    static {
        // note : all lower case!
        DatasetCombiner.microarrayNameStrings.put( "u133", new HashSet<String>() );
        DatasetCombiner.microarrayNameStrings.put( "u95", new HashSet<String>() );
        DatasetCombiner.microarrayNameStrings.put( "u74", new HashSet<String>() );
        DatasetCombiner.microarrayNameStrings.put( "v2", new HashSet<String>() );
        DatasetCombiner.microarrayNameStrings.put( "chip", new HashSet<String>() );
        DatasetCombiner.microarrayNameStrings.get( "u133" ).add( "u133A" );
        DatasetCombiner.microarrayNameStrings.get( "u133" ).add( "u133B" );
        DatasetCombiner.microarrayNameStrings.get( "u95" ).add( "u95A" );
        DatasetCombiner.microarrayNameStrings.get( "u95" ).add( "u95B" );
        DatasetCombiner.microarrayNameStrings.get( "u95" ).add( "u95C" );
        DatasetCombiner.microarrayNameStrings.get( "u95" ).add( "u95D" );
        DatasetCombiner.microarrayNameStrings.get( "u95" ).add( "u95E" );
        DatasetCombiner.microarrayNameStrings.get( "u74" ).add( "u74A" );
        DatasetCombiner.microarrayNameStrings.get( "u74" ).add( "u74B" );
        DatasetCombiner.microarrayNameStrings.get( "u74" ).add( "u74C" );
        DatasetCombiner.microarrayNameStrings.get( "v2" ).add( "av2" );
        DatasetCombiner.microarrayNameStrings.get( "v2" ).add( "av2" );
        DatasetCombiner.microarrayNameStrings.get( "v2" ).add( "av2" );
        DatasetCombiner.microarrayNameStrings.get( "chip" ).add( "chip a" );
        DatasetCombiner.microarrayNameStrings.get( "chip" ).add( "chip b" );
        DatasetCombiner.microarrayNameStrings.get( "chip" ).add( "chip c" );
        DatasetCombiner.microarrayNameStrings.get( "chip" ).add( "chipa" );
        DatasetCombiner.microarrayNameStrings.get( "chip" ).add( "chipb" );
        DatasetCombiner.microarrayNameStrings.get( "chip" ).add( "chipc" );
    }

    // Maps of sample accessions to other useful bits.
    private final LinkedHashMap<String, String> accToPlatform = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> accToTitle = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> accToDataset = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> accToOrganism = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> accToSecondaryTitle = new LinkedHashMap<>();
    private boolean doSampleMatching;

    public DatasetCombiner() {
        this.doSampleMatching = true;
    }

    public DatasetCombiner( boolean doSampleMatching ) {
        this.doSampleMatching = doSampleMatching;
    }

    /**
     * Given GEO series ids, find all associated data sets.
     *
     * @param seriesAccessions accessions
     * @return a collection of associated GDS accessions. If no GDS is found, the collection will be empty.
     */
    public static Collection<String> findGDSforGSE( Collection<String> seriesAccessions ) {
        /*
         * go from GSE to GDS, using screen scraping.
         */
        // http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gds&term=GSE674[Accession]&cmd=search
        // grep on "GDS[[digits]] record"
        Collection<String> associatedDatasetAccessions = new HashSet<>();

        for ( String seriesAccession : seriesAccessions ) {
            associatedDatasetAccessions.addAll( DatasetCombiner.findGDSforGSE( seriesAccession ) );
        }
        return associatedDatasetAccessions;

    }

    /**
     * @param seriesAccession series accession
     * @return GDSs that correspond to the given series. It will be empty if there is no GDS matching.
     */
    public static Collection<String> findGDSforGSE( String seriesAccession ) {

        Collection<String> associatedDatasetAccessions = new HashSet<>();
        try {
            String details = EutilFetch.fetch( "gds", seriesAccession, 100 );
            if ( details.equalsIgnoreCase( "no results" ) ) {
                return associatedDatasetAccessions;
            }
            XPathFactory xf = XPathFactory.newInstance();
            XPath xpath = xf.newXPath();

            /*
             * Get all Items of type GDS that are from a DocSum with an Item entryType of GDS.
             */
            XPathExpression xgds = xpath.compile(
                    "/eSummaryResult/DocSum[Item/@Name=\"entryType\" and (Item=\"GDS\")]/Item[@Name=\"GDS\"][1]/text()" );

            DocumentBuilder builder = DatasetCombiner.factory.newDocumentBuilder();

            /*
             * Bug 2690. There must be a better way.
             */
            details = details.replaceAll( "encoding=\"UTF-8\"", "" );
            try ( StringInputStream sis = new StringInputStream( StringUtils.strip( details ) ) ) {

                Document document = builder.parse( sis );

                NodeList result = ( NodeList ) xgds.evaluate( document, XPathConstants.NODESET );
                for ( int i = 0; i < result.getLength(); i++ ) {
                    String nodeValue = result.item( i ).getNodeValue();
                    // if ( nodeValue.contains( ";" ) ) continue; //
                    associatedDatasetAccessions.add( "GDS" + nodeValue );
                }

                return associatedDatasetAccessions;

            }

        } catch ( IOException e ) {
            throw new RuntimeException( "Could not parse XML data from remote server", e );
        } catch ( ParserConfigurationException | SAXException | XPathExpressionException e ) {
            throw new RuntimeException( "XML parsing error of remote data", e );
        }
    }

    /**
     * Given a GDS, find the corresponding GSEs (there can be more than one in rare cases).
     *
     * @param datasetAccession dataset accession
     * @return Collection of series this data set is derived from (this is almost always just a single item).
     */
    public static Collection<String> findGSEforGDS( String datasetAccession ) {
        /*
         * go from GDS to GSE, using screen scraping.
         */
        // http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gds&term=GSE674[Accession]&cmd=search
        // grep on "GDS[[digits]] record"
        URL url = null;

        Pattern pat = Pattern.compile( DatasetCombiner.GSE_RECORD_REGEXP );

        Collection<String> associatedSeriesAccession = new HashSet<>();

        try {
            url = new URL( DatasetCombiner.ENTREZ_GEO_QUERY_URL_BASE + datasetAccession
                    + DatasetCombiner.ENTREZ_GEO_QUERY_URL_SUFFIX );
            URLConnection conn = url.openConnection();
            conn.connect();

            try (InputStream is = conn.getInputStream();
                    BufferedReader br = new BufferedReader( new InputStreamReader( is ) )) {

                String line;
                while ( ( line = br.readLine() ) != null ) {
                    Matcher mat = pat.matcher( line );
                    if ( mat.find() ) {
                        String capturedAccession = mat.group( 1 );
                        associatedSeriesAccession.add( capturedAccession );
                    }
                }
            }
        } catch ( MalformedURLException e ) {
            DatasetCombiner.log.error( e, e );
            throw new RuntimeException( "Invalid URL " + url, e );
        } catch ( IOException e ) {
            DatasetCombiner.log.error( e, e );
            throw new RuntimeException( "Could not get data from remote server", e );
        }

        if ( associatedSeriesAccession.size() == 0 ) {
            throw new IllegalStateException( "No GSE found for " + datasetAccession );
        }

        return associatedSeriesAccession;

    }

    public static Map<GeoPlatform, List<GeoSample>> getPlatformSampleMap( GeoSeries geoSeries ) {
        Map<GeoPlatform, List<GeoSample>> platformSamples = new HashMap<>();

        for ( GeoSample sample : geoSeries.getSamples() ) {

            for ( GeoPlatform platform : sample.getPlatforms() ) {
                if ( !platformSamples.containsKey( platform ) ) {
                    platformSamples.put( platform, new ArrayList<GeoSample>() );
                }
                platformSamples.get( platform ).add( sample );
            }
        }
        return platformSamples;
    }

    /**
     * Given a GEO dataset id, find all GDS ids that are associated with it.
     *
     * @param datasetAccession the geo accession
     * @return all GDS associated with the given accession
     */
    public Collection<String> findGDSforGDS( String datasetAccession ) {
        return DatasetCombiner.findGDSforGSE( DatasetCombiner.findGSEforGDS( datasetAccession ) );
    }

    /**
     * Try to line up samples across datasets.
     *
     * @param dataSets datasets
     * @return sample correspondence
     */
    public GeoSampleCorrespondence findGSECorrespondence( Collection<GeoDataset> dataSets ) {

        if ( dataSets == null )
            return null;
        if ( dataSets.size() == 0 ) {
            throw new IllegalArgumentException( "No datasets!" );
        }

        this.checkPlatformsMatchSeries( dataSets );

        this.fillAccessionMaps( dataSets );
        int numDatasets = dataSets.size();
        return this.findCorrespondence( numDatasets );
    }

    /**
     * Try to line up samples across datasets contained in a series.
     *
     * @param series geo series
     * @return geo sample correspondence
     */
    public GeoSampleCorrespondence findGSECorrespondence( GeoSeries series ) {
        Collection<GeoDataset> datasets = series.getDataSets();
        if ( datasets != null && datasets.size() > 0 ) {
            this.fillAccessionMaps( datasets );

            // make sure all samples are accounted for - just informative
            Collection<GeoSample> missed = new HashSet<>();
            for ( GeoSample sample : series.getSamples() ) {
                if ( !this.accToDataset.containsKey( sample.getGeoAccession() ) ) {
                    missed.add( sample );
                }
            }
            if ( !missed.isEmpty() ) {
                DatasetCombiner.log.warn( "There were one or more samples missing from the datasets: " + StringUtils
                        .join( missed, " | " ) );
            }
            return this.findGSECorrespondence( datasets );
        }

        int numPlatforms = this.fillAccessionMaps( series );
        return this.findCorrespondence( numPlatforms );
    }

    /**
     * See bug 1672 for why this is needed.
     *
     * @param dataSets datasets
     */
    private void checkPlatformsMatchSeries( Collection<GeoDataset> dataSets ) {
        for ( GeoDataset dataset : dataSets ) {
            boolean found = false;
            GeoPlatform platform = dataset.getPlatform();

            if ( dataset.getSeries().size() == 0 )
                continue;

            Collection<GeoPlatform> seenPlatforms = new HashSet<>();

            for ( GeoSeries series : dataset.getSeries() ) {
                for ( GeoSample sample : series.getSamples() ) {
                    if ( sample.getPlatforms().contains( platform ) ) {
                        found = true;
                    }
                    seenPlatforms.addAll( sample.getPlatforms() );
                }
            }

            if ( !found ) {

                if ( seenPlatforms.size() == 1 ) {
                    DatasetCombiner.log.warn( dataset + " is associated with wrong platform? " + platform
                            + ", switching it to use series platform " + seenPlatforms.iterator().next() );
                    dataset.setPlatform( seenPlatforms.iterator().next() );
                } else {
                    /*
                     * Maybe there is a way to handle this, but not worth it. Dataset uses the wrong platform.
                     */
                    throw new IllegalStateException( platform + " on dataset " + dataset
                            + " is not used at all by associated series, can't determine correct platform as series uses more than one." );
                }
            }
        }

    }

    private int computeDistance( String trimmedTest, String trimmedTarget ) {

        return StringDistance.editDistance( trimmedTarget, trimmedTest );

    }

    private void fillAccessionMap( GeoSample sample, GeoData owner ) {
        String title = sample.getTitle();
        if ( StringUtils.isNotBlank( title ) ) {
            accToTitle.put( sample.getGeoAccession(), title );
        }
        accToDataset.put( sample.getGeoAccession(), owner.getGeoAccession() );
        accToSecondaryTitle.put( sample.getGeoAccession(), sample.getTitleInDataset() ); // could be null.
        String organism = this.getSampleOrganism( sample );
        if ( StringUtils.isNotBlank( organism ) ) {
            accToOrganism.put( sample.getGeoAccession(), organism );
        }
    }

    private void fillAccessionMaps( Collection<GeoDataset> dataSets ) {

        for ( GeoDataset dataset : dataSets ) {
            GeoPlatform platform = dataset.getPlatform();
            assert platform != null;
            platform.getOrganisms().add( dataset.getOrganism() );
            if ( dataset.getSubsets().size() == 0 ) {
                assert dataset.getSeries().size() > 0;
                for ( GeoSeries series : dataset.getSeries() ) {
                    for ( GeoSample sample : series.getSamples() ) {

                        if ( sample.getPlatforms().size() == 0 )
                            sample.addPlatform( platform );
                        this.fillAccessionMap( sample, dataset );
                    }
                }
            } else {
                for ( GeoSubset subset : dataset.getSubsets() ) {
                    for ( GeoSample sample : subset.getSamples() ) {

                        if ( sample.getPlatforms().size() == 0 )
                            sample.addPlatform( platform );

                        this.fillAccessionMap( sample, dataset );
                    }
                }
            }
        }
    }

    /**
     * This is used if there are no 'datasets' (GDS) to work with; we just use platforms.
     *
     * @param series geo series
     * @return platform sample size
     */
    private int fillAccessionMaps( GeoSeries series ) {

        Map<GeoPlatform, List<GeoSample>> platformSamples = DatasetCombiner.getPlatformSampleMap( series );

        for ( GeoPlatform platform : platformSamples.keySet() ) {
            for ( GeoSample sample : platformSamples.get( platform ) ) {
                assert sample != null : "Null sample for platform " + platform.getDescription();
                this.fillAccessionMap( sample, platform );
            }
        }

        return platformSamples.keySet().size();
    }

    /**
     * This is the main point where comparisons are made.
     *
     * @param numDatasetsOrPlatforms number of datasets or platforms
     * @return geo sample correspondence
     */
    private GeoSampleCorrespondence findCorrespondence( int numDatasetsOrPlatforms ) {
        GeoSampleCorrespondence result = new GeoSampleCorrespondence();

        result.setAccToTitleMap( accToTitle );

        final List<String> sampleAccs = new ArrayList<>( accToDataset.keySet() );
        assert sampleAccs.size() > 0;

        if ( numDatasetsOrPlatforms <= 1 || !this.doSampleMatching ) {
            DatasetCombiner.log.debug( "Each bioassay will get a distinct biomaterial" );
            for ( String sample : sampleAccs ) {
                result.addCorrespondence( sample, null );
            }
            return result;
        }

        String commonPrefix = StringUtil.commonPrefix( accToTitle.values() );
        if ( commonPrefix != null ) {
            DatasetCombiner.log.debug( "Common prefix = " + commonPrefix );
            commonPrefix = commonPrefix.toLowerCase();
        }
        String commonSuffix = StringUtil.commonSuffix( accToTitle.values() );
        if ( commonSuffix != null ) {
            DatasetCombiner.log.debug( "Common suffix = " + commonSuffix );
            commonSuffix = commonSuffix.toLowerCase();
        }

        // using the sorted order helps find the right matches.
        Collections.sort( sampleAccs );

        Map<String, Collection<String>> alreadyMatched = new HashMap<>();

        // do it by data set, so we constrain comparing items in _this_ data set to ones in _other_ data sets (or other
        // platforms)
        // The inner loops are just to get the samples in the data set (platform) being considered.
        //noinspection MismatchedQueryAndUpdateOfCollection // I do not dare to touch this method without proper refactoring
        Collection<String> alreadyTestedDatasetsOrPlatforms = new HashSet<>();

        List<String> dataSets = new ArrayList<>( new HashSet<>( accToDataset.values() ) );

        List<String> platforms = new ArrayList<>( new HashSet<>( accToPlatform.values() ) );

        List<String> valuesToUse;
        LinkedHashMap<String, String> accToDatasetOrPlatform;

        if ( dataSets.size() > 0 ) {
            valuesToUse = dataSets;
            this.sortDataSets( sampleAccs, valuesToUse );
            accToDatasetOrPlatform = accToDataset;
            result.setAccToDatasetOrPlatformMap( accToDataset );
            DatasetCombiner.log.debug( dataSets.size() + " datasets" );
        } else {
            valuesToUse = platforms;
            this.sortPlatforms( sampleAccs, valuesToUse );
            accToDatasetOrPlatform = accToPlatform;
            result.setAccToDatasetOrPlatformMap( accToPlatform );
            DatasetCombiner.log.debug( platforms.size() + " platforms" );
        }

        this.processDatasets( result, sampleAccs, commonPrefix, commonSuffix, alreadyMatched,
                alreadyTestedDatasetsOrPlatforms, valuesToUse, accToDatasetOrPlatform );

        DatasetCombiner.log.debug( result );
        return result;
    }

    private void processDatasets( GeoSampleCorrespondence result, List<String> sampleAccs, String commonPrefix,
            String commonSuffix, Map<String, Collection<String>> alreadyMatched,
            Collection<String> alreadyTestedDatasetsOrPlatforms, List<String> valuesToUse,
            LinkedHashMap<String, String> accToDatasetOrPlatform ) {

        Collection<String> allMatched = new HashSet<>();

        // we start with the smallest dataset/platform.
        for ( String datasetOrPlatformA : valuesToUse ) {
            alreadyTestedDatasetsOrPlatforms.add( datasetOrPlatformA );
            DatasetCombiner.log.debug( "Finding matches for samples in " + datasetOrPlatformA );

            // for each sample in this data set...
            for ( int j = 0; j < sampleAccs.size(); j++ ) {
                this.processSample( result, sampleAccs, commonPrefix, commonSuffix, alreadyMatched, valuesToUse,
                        accToDatasetOrPlatform, allMatched, datasetOrPlatformA, j );
            }
        }
    }

    private void processSample( GeoSampleCorrespondence result, List<String> sampleAccs, String commonPrefix,
            String commonSuffix, Map<String, Collection<String>> alreadyMatched, List<String> valuesToUse,
            LinkedHashMap<String, String> accToDatasetOrPlatform, Collection<String> allMatched,
            String datasetOrPlatformA, int j ) {

        String targetAcc = sampleAccs.get( j );

        // skip samples that are not in this data set.
        if ( !accToDataset.get( targetAcc ).equals( datasetOrPlatformA ) ) {
            return;
        }
        if ( allMatched.contains( targetAcc ) )
            return;

        if ( !accToTitle.containsKey( targetAcc ) ) {
            return;
        }
        String targetTitle = accToTitle.get( targetAcc ).toLowerCase();
        String targetSecondaryTitle = null;
        if ( accToSecondaryTitle.get( targetAcc ) != null ) {
            targetSecondaryTitle = accToSecondaryTitle.get( targetAcc ).toLowerCase();
        }

        DatasetCombiner.log.debug( "Target: " + targetAcc + " (" + datasetOrPlatformA + ") " + targetTitle + (
                targetSecondaryTitle == null ?
                        "" :
                        " a.k.a " + targetSecondaryTitle ) );
        if ( StringUtils.isBlank( targetTitle ) )
            throw new IllegalArgumentException( "Can't have blank titles for samples" );

        Collection<String> bonusWords = this.getMicroarrayStringsToMatch( targetTitle );

        /*
         * For each of the other data sets
         */
        boolean wasTied = false;
        for ( String datasetOrPlatformB : valuesToUse ) {
            wasTied = this.processSecondDataset( result, sampleAccs, commonPrefix, commonSuffix, alreadyMatched,
                    accToDatasetOrPlatform, allMatched, datasetOrPlatformA, wasTied, targetAcc, targetTitle,
                    targetSecondaryTitle, bonusWords, datasetOrPlatformB );
        }
    }

    private boolean processSecondDataset( GeoSampleCorrespondence result, List<String> sampleAccs, String commonPrefix,
            String commonSuffix, Map<String, Collection<String>> alreadyMatched,
            LinkedHashMap<String, String> accToDatasetOrPlatform, Collection<String> allMatched,
            String datasetOrPlatformA, boolean wasTied, String targetAcc, String targetTitle,
            String targetSecondaryTitle, Collection<String> bonusWords, String datasetOrPlatformB ) {

        if ( datasetOrPlatformB.equals( datasetOrPlatformA ) ) {
            return wasTied;
        }

        // initialize data structure.
        if ( alreadyMatched.get( targetAcc ) == null ) {
            alreadyMatched.put( targetAcc, new HashSet<String>() );
        }

        /*
         * Keep us from getting multiple matches.
         */
        if ( alreadyMatched.get( targetAcc ).contains( datasetOrPlatformB ) ) {
            return wasTied;
        }

        wasTied = this.findBestSampleHit( result, sampleAccs, commonPrefix, commonSuffix, alreadyMatched,
                accToDatasetOrPlatform, allMatched, datasetOrPlatformA, wasTied, targetAcc, targetTitle,
                targetSecondaryTitle, bonusWords, datasetOrPlatformB );

        return wasTied;
    }

    private boolean findBestSampleHit( GeoSampleCorrespondence result, List<String> sampleAccs, String commonPrefix,
            String commonSuffix, Map<String, Collection<String>> alreadyMatched,
            LinkedHashMap<String, String> accToDatasetOrPlatform, Collection<String> allMatched,
            String datasetOrPlatformA, boolean wasTied, String targetAcc, String targetTitle,
            String targetSecondaryTitle, Collection<String> bonusWords, String datasetOrPlatformB ) {
        // find the best match in this data set.
        double minDistance = Double.MAX_VALUE;
        String bestMatch = null;
        String bestMatchAcc = null;

        int numTested = 0;
        for ( String testAcc : sampleAccs ) {

            if ( this.checkCanSkip( alreadyMatched, accToDatasetOrPlatform, allMatched, datasetOrPlatformA, targetAcc,
                    datasetOrPlatformB, testAcc ) )
                continue;

            numTested++;

            if ( !accToTitle.containsKey( testAcc ) ) {
                continue;
            }

            String testTitle = accToTitle.get( testAcc ).toLowerCase();
            String testSecondaryTitle = null;
            if ( accToSecondaryTitle.get( testAcc ) != null ) {
                testSecondaryTitle = accToSecondaryTitle.get( testAcc ).toLowerCase();
            }

            if ( StringUtils.isBlank( testTitle ) )
                throw new IllegalArgumentException( "Can't have blank titles for samples" );

            double bonus = this.calculateBonus( targetTitle, bonusWords, testTitle );

            /*
             * If one name is much longer than the other, presumably the author didn't use the same naming
             * scheme for all samples; we need to trim the longer one to match the shorter one; we use the
             * prefix.
             */
            String trimmedTest = testTitle;
            String trimmedTarget = targetTitle;

            if ( commonPrefix != null ) {
                trimmedTest = trimmedTest.replaceFirst( "^" + Pattern.quote( commonPrefix ), "" );
                trimmedTarget = trimmedTarget.replaceFirst( "^" + Pattern.quote( commonPrefix ), "" );
            }
            if ( commonSuffix != null ) {
                trimmedTest = trimmedTest.replaceFirst( Pattern.quote( commonSuffix ) + "$", "" );
                trimmedTarget = trimmedTarget.replaceFirst( Pattern.quote( commonSuffix ) + "$", "" );
            }

            // remove some punctuation
            trimmedTest = trimmedTest.replaceAll( DatasetCombiner.PUNCTUATION_REGEXP, "" );
            trimmedTarget = trimmedTarget.replaceAll( DatasetCombiner.PUNCTUATION_REGEXP, "" );

            // Computing the distance
            double distance = this.computeDistance( trimmedTest, trimmedTarget );

            distance -= bonus;

            double normalizedDistance = distance / Math.max( trimmedTarget.length(), trimmedTest.length() );

            double secondaryDistance;
            if ( targetSecondaryTitle != null && testSecondaryTitle != null ) {
                secondaryDistance = this.computeDistance( targetSecondaryTitle, testSecondaryTitle );

                if ( secondaryDistance < distance ) {
                    distance = secondaryDistance;
                    normalizedDistance =
                            distance / Math.max( targetSecondaryTitle.length(), testSecondaryTitle.length() );
                }
            }

            if ( !this.meetsMinimalThreshold( normalizedDistance ) ) {
                continue;
            }

            // better than last one?
            if ( distance > minDistance ) {
                continue;
            }

            // Try to resolve the tie. Messy, yes.
            if ( distance == minDistance ) {
                wasTied = true;
                assert bestMatchAcc != null;
                double prefixWeightedDistanceA = StringDistance
                        .prefixWeightedHammingDistance( targetAcc, bestMatchAcc, 1.0 );
                double prefixWeightedDistanceB = StringDistance
                        .prefixWeightedHammingDistance( targetAcc, testAcc, 1.0 );
                if ( prefixWeightedDistanceA == prefixWeightedDistanceB ) {
                    double suffixWeightedDistanceA = StringDistance
                            .suffixWeightedHammingDistance( targetAcc, bestMatchAcc, 1.0 );
                    double suffixWeightedDistanceB = StringDistance
                            .suffixWeightedHammingDistance( targetAcc, testAcc, 1.0 );
                    if ( prefixWeightedDistanceA == prefixWeightedDistanceB ) {
                        continue; // still tied, keep old one
                    } else if ( suffixWeightedDistanceA < suffixWeightedDistanceB ) {
                        // new one is better.
                        minDistance = distance;
                        bestMatch = testTitle;
                        bestMatchAcc = testAcc;
                        DatasetCombiner.log
                                .debug( "Current best match (tie broken): " + testAcc + " (" + datasetOrPlatformB + ") "
                                        + testTitle + ( testSecondaryTitle == null ?
                                        "" :
                                        " a.k.a " + testSecondaryTitle + ", distance = " + distance ) );
                        wasTied = false;
                    }
                    if ( suffixWeightedDistanceA > suffixWeightedDistanceB ) {
                        // old one is still better.
                        wasTied = false;
                        //noinspection UnnecessaryContinue // better for readability
                        continue;
                    }
                } else if ( prefixWeightedDistanceA > prefixWeightedDistanceB ) {
                    // new one is better.
                    minDistance = distance;
                    bestMatch = testTitle;
                    bestMatchAcc = testAcc;
                    DatasetCombiner.log
                            .debug( "Current best match (tie broken): " + testAcc + " (" + datasetOrPlatformB + ") "
                                    + testTitle + ( testSecondaryTitle == null ?
                                    "" :
                                    " a.k.a " + testSecondaryTitle + ", distance = " + distance ) );
                    wasTied = false;
                } else {
                    wasTied = false;
                    //noinspection UnnecessaryContinue // better for readability
                    continue; // old best is still better.
                }
            } else {
                // definite new winner no tie
                minDistance = distance;
                bestMatch = testTitle;
                bestMatchAcc = testAcc;
                DatasetCombiner.log
                        .debug( "Current best match: " + testAcc + " (" + datasetOrPlatformB + ") " + testTitle + (
                                testSecondaryTitle == null ?
                                        "" :
                                        " a.k.a " + testSecondaryTitle + ", distance = " + distance ) );
                wasTied = false;
            }

        } // end loop over samples in second data set.
        DatasetCombiner.log.debug( "Tested " + numTested + " samples" );

        /*
         * Now have the best hit for sample from the outer dataset, in the inner data set.
         */
        if ( bestMatchAcc == null || wasTied ) {
            if ( DatasetCombiner.log.isDebugEnabled() )
                DatasetCombiner.log
                        .debug( "No match found in " + datasetOrPlatformB + " for " + targetAcc + "\t" + targetTitle
                                + " (" + datasetOrPlatformA
                                + ") (This can happen if sample was not run on all the platforms used; or if there were ties that could not be broken; or when we were unable to match)" );
            result.addCorrespondence( targetAcc, null );
            allMatched.add( targetAcc );
        } else {
            if ( DatasetCombiner.log.isDebugEnabled() )
                DatasetCombiner.log
                        .debug( "Match:\n" + targetAcc + "\t" + targetTitle + " (" + accToDataset.get( targetAcc ) + ")"
                                + "\n" + bestMatchAcc + "\t" + bestMatch + " (" + accToDataset.get( bestMatchAcc ) + ")"
                                + " (Distance: " + minDistance + ")" );
            result.addCorrespondence( targetAcc, bestMatchAcc );
            alreadyMatched.get( bestMatchAcc ).add( datasetOrPlatformA );
            alreadyMatched.get( targetAcc ).add( datasetOrPlatformB );
            allMatched.add( targetAcc );
            allMatched.add( bestMatchAcc );
        }
        return wasTied;
    }

    private boolean checkCanSkip( Map<String, Collection<String>> alreadyMatched,
            LinkedHashMap<String, String> accToDatasetOrPlatform, Collection<String> allMatched,
            String datasetOrPlatformA, String targetAcc, String datasetOrPlatformB, String testAcc ) {
        return allMatched.contains( testAcc ) || !this
                .shouldTest( accToDatasetOrPlatform, alreadyMatched, datasetOrPlatformA, targetAcc, datasetOrPlatformB,
                        testAcc );
    }

    private double calculateBonus( String targetTitle, Collection<String> bonusWords, String testTitle ) {
        double bonus = 0.0;
        bonusWords.addAll( this.getMicroarrayStringsToMatch( testTitle ) );
        for ( String n : bonusWords ) {
            if ( testTitle.contains( n ) ) {
                DatasetCombiner.log.debug( testTitle + " gets a bonus in matching " + targetTitle );
                bonus = 1; // this basically means we discount that difference.
                break;
            }
        }
        return bonus;
    }

    /**
     * @param title title
     * @return stop-strings relating to microarray names.
     */
    private Collection<String> getMicroarrayStringsToMatch( String title ) {
        Collection<String> result = new HashSet<>();
        for ( String key : DatasetCombiner.microarrayNameStrings.keySet() ) {
            if ( title.contains( key ) ) {
                for ( String value : DatasetCombiner.microarrayNameStrings.get( key ) ) {
                    if ( title.contains( value ) ) {
                        result.add( value );
                    }
                }
            }
        }
        return result;
    }

    private String getSampleOrganism( GeoSample sample ) {
        Collection<GeoPlatform> platforms = sample.getPlatforms();
        assert platforms.size() > 0 : sample + " had no platform assigned";
        GeoPlatform platform = platforms.iterator().next();
        Collection<String> organisms = platform.getOrganisms();
        assert organisms.size() > 0;
        return organisms.iterator().next();
    }

    private boolean meetsMinimalThreshold( double normalizedDistance ) {
        return !( normalizedDistance > DatasetCombiner.SIMILARITY_THRESHOLD );
    }

    /**
     * Implements constraints on samples to test.
     *
     * @param accToDatasetOrPlatform (depending on which we are using, platforms or data sets)
     * @return should test
     */
    private boolean shouldTest( LinkedHashMap<String, String> accToDatasetOrPlatform,
            Map<String, Collection<String>> alreadyMatched, String datasetA, String targetAcc, String datasetB,
            String testAcc ) {
        boolean shouldTest = true;

        // initialize data structure.
        if ( alreadyMatched.get( testAcc ) == null ) {
            alreadyMatched.put( testAcc, new HashSet<String>() );
        }

        // only use samples from the current test dataset.
        if ( !accToDatasetOrPlatform.get( testAcc ).equals( datasetB ) ) {
            shouldTest = false;
        }

        // disallow multiple matches.
        if ( alreadyMatched.get( testAcc ).contains( datasetA ) ) {
            // log.debug( testAcc + " already matched to a sample in " + datasetA + ", skipping" );
            shouldTest = false;
        }

        if ( !accToOrganism.get( targetAcc ).equals( accToOrganism.get( testAcc ) ) ) {
            DatasetCombiner.log.debug( testAcc + " From wrong organism" );
            shouldTest = false;
        }
        return shouldTest;
    }

    private void sortDataSets( final List<String> sampleAccs, List<String> dataSets ) {
        this.sortMap( sampleAccs, dataSets, accToDataset );
    }

    private void sortPlatforms( final List<String> sampleAccs, List<String> platforms ) {
        this.sortMap( sampleAccs, platforms, accToPlatform );
    }

    private void sortMap( final List<String> sampleAccs, final List<String> objects, final Map<String, String> map ) {
        Collections.sort( objects, new Comparator<String>() {
            @Override
            public int compare( String arg0, String arg1 ) {
                int numSamples0 = 0;
                int numSamples1 = 0;
                for ( String targetAcc : sampleAccs ) {
                    // skip samples that are not in this data set.
                    if ( map.get( targetAcc ).equals( arg0 ) ) {
                        numSamples0++;
                    } else if ( map.get( targetAcc ).equals( arg1 ) ) {
                        numSamples1++;
                    }
                }

                return Integer.compare( numSamples0, numSamples1 );
            }
        } );
    }

}

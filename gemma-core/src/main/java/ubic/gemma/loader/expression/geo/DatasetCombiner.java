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
package ubic.gemma.loader.expression.geo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hsqldb.lib.StringInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ubic.basecode.math.StringDistance;
import ubic.basecode.util.StringUtil;
import ubic.gemma.loader.entrez.EutilFetch;
import ubic.gemma.loader.expression.geo.model.GeoData;
import ubic.gemma.loader.expression.geo.model.GeoDataset;
import ubic.gemma.loader.expression.geo.model.GeoPlatform;
import ubic.gemma.loader.expression.geo.model.GeoSample;
import ubic.gemma.loader.expression.geo.model.GeoSeries;
import ubic.gemma.loader.expression.geo.model.GeoSubset;

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
 * <p>
 * Another problem is that there is no way to go from GDS-->GSE-->other GDS without scraping the GEO web site.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class DatasetCombiner {

    private static final String PUNCTUATION_REGEXP = "[\\(\\)\\s-\\._]";

    /**
     * Careful, GEO changes this sometimes.
     */
    private static final String GSE_RECORD_REGEXP = "(GSE\\d+)";
    /**
     * 
     */
    private static final String ENTREZ_GEO_QUERY_URL_SUFFIX = "[Accession]&cmd=search";
    /**
     * 
     */
    private static final String ENTREZ_GEO_QUERY_URL_BASE = "https://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gds&term=";

    private boolean doSampleMatching = true;

    /**
     * Threshold normalized similarity between two strings before we bother to make a match. The normalized similarity
     * is the ratio between the unnormalized edit distance and the length of the longer of the two strings. This is used
     * as a maximum distance (the pair of descriptors must be at least this close).
     * <p>
     * Setting this correctly is important if there are to be singletons (samples that don't match to others)
     */
    private final double SIMILARITY_THRESHOLD = 0.5;

    public DatasetCombiner( boolean doSampleMatching ) {
        this.doSampleMatching = doSampleMatching;
    }

    public DatasetCombiner() {
        this.doSampleMatching = true;
    }

    private static Log log = LogFactory.getLog( DatasetCombiner.class.getName() );

    // Maps of sample accessions to other useful bits.
    LinkedHashMap<String, String> accToPlatform = new LinkedHashMap<String, String>();
    LinkedHashMap<String, String> accToTitle = new LinkedHashMap<String, String>();
    LinkedHashMap<String, String> accToDataset = new LinkedHashMap<String, String>();
    LinkedHashMap<String, String> accToOrganism = new LinkedHashMap<String, String>();
    LinkedHashMap<String, String> accToSecondaryTitle = new LinkedHashMap<String, String>();

    /**
     * Given a GDS, find the corresponding GSEs (there can be more than one in rare cases).
     * 
     * @param datasetAccession
     * @return Collection of series this data set is derived from (this is almost always just a single item).
     */
    public static Collection<String> findGSEforGDS( String datasetAccession ) {
        /*
         * go from GDS to GSE, using screen scraping.
         */
        // http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gds&term=GSE674[Accession]&cmd=search
        // grep on "GDS[[digits]] record"
        URL url = null;

        Pattern pat = Pattern.compile( GSE_RECORD_REGEXP );

        Collection<String> associatedSeriesAccession = new HashSet<String>();

        try {
            url = new URL( ENTREZ_GEO_QUERY_URL_BASE + datasetAccession + ENTREZ_GEO_QUERY_URL_SUFFIX );
            URLConnection conn = url.openConnection();
            conn.connect();

            try (InputStream is = conn.getInputStream();
                    BufferedReader br = new BufferedReader( new InputStreamReader( is ) );) {

                String line = null;
                while ( ( line = br.readLine() ) != null ) {
                    Matcher mat = pat.matcher( line );
                    if ( mat.find() ) {
                        String capturedAccession = mat.group( 1 );
                        associatedSeriesAccession.add( capturedAccession );
                    }
                }
                is.close();
            }
        } catch ( MalformedURLException e ) {
            log.error( e, e );
            throw new RuntimeException( "Invalid URL " + url, e );
        } catch ( IOException e ) {
            log.error( e, e );
            throw new RuntimeException( "Could not get data from remote server", e );
        }

        if ( associatedSeriesAccession.size() == 0 ) {
            throw new IllegalStateException( "No GSE found for " + datasetAccession );
        }

        return associatedSeriesAccession;

    }

    /**
     * Given a GEO dataset it, find all GDS ids that are associated with it.
     * 
     * @param seriesAccession
     * @return
     */
    public Collection<String> findGDSforGDS( String datasetAccession ) {
        return findGDSforGSE( findGSEforGDS( datasetAccession ) );
    }

    /**
     * Given GEO series ids, find all associated data sets.
     * 
     * @param seriesAccession
     * @return a collection of associated GDS accessions. If no GDS is found, the collection will be empty.
     */
    public static Collection<String> findGDSforGSE( Collection<String> seriesAccessions ) {
        /*
         * go from GSE to GDS, using screen scraping.
         */
        // http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gds&term=GSE674[Accession]&cmd=search
        // grep on "GDS[[digits]] record"
        Collection<String> associatedDatasetAccessions = new HashSet<String>();

        for ( String seriesAccession : seriesAccessions ) {
            associatedDatasetAccessions.addAll( findGDSforGSE( seriesAccession ) );
        }
        return associatedDatasetAccessions;

    }

    static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    /**
     * @param seriesAccession
     * @return GDSs that correspond to the given series. It will be empty if there is no GDS matching.
     */
    public static Collection<String> findGDSforGSE( String seriesAccession ) {

        Collection<String> associatedDatasetAccessions = new HashSet<String>();
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
            XPathExpression xgds = xpath
                    .compile( "/eSummaryResult/DocSum[Item/@Name=\"entryType\" and (Item=\"GDS\")]/Item[@Name=\"GDS\"][1]/text()" );

            DocumentBuilder builder = factory.newDocumentBuilder();

            /*
             * Bug 2690. There must be a better way.
             */
            details = details.replaceAll( "encoding=\"UTF-8\"", "" );
            try (StringInputStream sis = new StringInputStream( StringUtils.trim( details ) );) {

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
     * Try to line up samples across datasets contained in a series.
     * 
     * @param series
     * @return
     */
    public GeoSampleCorrespondence findGSECorrespondence( GeoSeries series ) {
        Collection<GeoDataset> datasets = series.getDatasets();
        if ( datasets != null && datasets.size() > 0 ) {
            fillAccessionMaps( datasets );

            // make sure all samples are accounted for - just informative
            Collection<GeoSample> missed = new HashSet<GeoSample>();
            for ( GeoSample sample : series.getSamples() ) {
                if ( !this.accToDataset.containsKey( sample.getGeoAccession() ) ) {
                    missed.add( sample );
                }
            }
            if ( !missed.isEmpty() ) {
                log.warn( "There were one or more samples missing from the datasets: "
                        + StringUtils.join( missed, " | " ) );
            }
            return findGSECorrespondence( datasets );
        }

        int numPlatforms = fillAccessionMaps( series );
        return findCorrespondence( numPlatforms );
    }

    /**
     * Try to line up samples across datasets.
     * 
     * @param dataSets
     */
    public GeoSampleCorrespondence findGSECorrespondence( Collection<GeoDataset> dataSets ) {

        if ( dataSets == null ) return null;
        if ( dataSets.size() == 0 ) {
            throw new IllegalArgumentException( "No datasets!" );
        }

        checkPlatformsMatchSeries( dataSets );

        fillAccessionMaps( dataSets );
        int numDatasets = dataSets.size();
        return findCorrespondence( numDatasets );
    }

    /**
     * See bug 1672 for why this is needed.
     * 
     * @param dataSets
     */
    private void checkPlatformsMatchSeries( Collection<GeoDataset> dataSets ) {
        for ( GeoDataset dataset : dataSets ) {
            boolean found = false;
            GeoPlatform platform = dataset.getPlatform();

            if ( dataset.getSeries().size() == 0 ) continue;

            Collection<GeoPlatform> seenPlatforms = new HashSet<GeoPlatform>();

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
                    log.warn( dataset + " is associated with wrong platform? " + platform
                            + ", switching it to use series platform " + seenPlatforms.iterator().next() );
                    dataset.setPlatform( seenPlatforms.iterator().next() );
                } else {
                    /*
                     * Maybe there is a way to handle this, but not worth it. Dataset uses the wrong platform.
                     */
                    throw new IllegalStateException(
                            platform
                                    + " on dataset "
                                    + dataset
                                    + " is not used at all by associated series, can't determine correct platform as series uses more than one." );
                }
            }
        }

    }

    /**
     * This is the main point where comparisons are made.
     * 
     * @param numDatasetsOrPlatforms
     * @param accToTitle
     * @param accToDataset
     * @return
     */
    private GeoSampleCorrespondence findCorrespondence( int numDatasetsOrPlatforms ) {
        GeoSampleCorrespondence result = new GeoSampleCorrespondence();

        result.setAccToTitleMap( accToTitle );

        final List<String> sampleAccs = new ArrayList<String>( accToDataset.keySet() );
        assert sampleAccs.size() > 0;

        if ( numDatasetsOrPlatforms <= 1 || !this.doSampleMatching ) {
            log.debug( "Each bioassay will get a distinct biomaterial" );
            for ( String sample : sampleAccs ) {
                result.addCorrespondence( sample, null );
            }
            return result;
        }

        String commonPrefix = StringUtil.commonPrefix( accToTitle.values() );
        if ( commonPrefix != null ) {
            log.debug( "Common prefix = " + commonPrefix );
            commonPrefix = commonPrefix.toLowerCase();
        }
        String commonSuffix = StringUtil.commonSuffix( accToTitle.values() );
        if ( commonSuffix != null ) {
            log.debug( "Common suffix = " + commonSuffix );
            commonSuffix = commonSuffix.toLowerCase();
        }

        // using the sorted order helps find the right matches.
        Collections.sort( sampleAccs );

        Map<String, Collection<String>> alreadyMatched = new HashMap<String, Collection<String>>();

        // do it by data set, so we constrain comparing items in _this_ data set to ones in _other_ data sets (or other
        // platforms)
        // The inner loops are just to get the samples in the data set (platform) being considered.
        Collection<String> alreadyTestedDatasetsOrPlatforms = new HashSet<String>();

        List<String> dataSets = new ArrayList<String>();
        dataSets.addAll( new HashSet<String>( accToDataset.values() ) );

        List<String> platforms = new ArrayList<String>();
        platforms.addAll( new HashSet<String>( accToPlatform.values() ) );

        List<String> valuesToUse;
        LinkedHashMap<String, String> accToDatasetOrPlatform;

        if ( dataSets.size() > 0 ) {
            valuesToUse = dataSets;
            sortDataSets( sampleAccs, valuesToUse );
            accToDatasetOrPlatform = accToDataset;
            result.setAccToDatasetOrPlatformMap( accToDataset );
            log.debug( dataSets.size() + " datasets" );
        } else {
            valuesToUse = platforms;
            sortPlatforms( sampleAccs, valuesToUse );
            accToDatasetOrPlatform = accToPlatform;
            result.setAccToDatasetOrPlatformMap( accToPlatform );
            log.debug( platforms.size() + " platforms" );
        }

        // we start with the smallest dataset/platform.

        Collection<String> allMatched = new HashSet<String>();
        for ( String datasetOrPlatformA : valuesToUse ) {
            alreadyTestedDatasetsOrPlatforms.add( datasetOrPlatformA );
            log.debug( "Finding matches for samples in " + datasetOrPlatformA );

            // for each sample in this data set...
            for ( int j = 0; j < sampleAccs.size(); j++ ) {

                boolean wasTied = false;
                String targetAcc = sampleAccs.get( j );

                // skip samples that are not in this data set.
                if ( !accToDataset.get( targetAcc ).equals( datasetOrPlatformA ) ) {
                    continue;
                }
                if ( allMatched.contains( targetAcc ) ) continue;

                if ( !accToTitle.containsKey( targetAcc ) ) {
                    continue;
                }
                String targetTitle = accToTitle.get( targetAcc ).toLowerCase();
                String targetSecondaryTitle = null;
                if ( accToSecondaryTitle.get( targetAcc ) != null ) {
                    targetSecondaryTitle = accToSecondaryTitle.get( targetAcc ).toLowerCase();
                }

                log.debug( "Target: " + targetAcc + " (" + datasetOrPlatformA + ") " + targetTitle
                        + ( targetSecondaryTitle == null ? "" : " a.k.a " + targetSecondaryTitle ) );
                if ( StringUtils.isBlank( targetTitle ) )
                    throw new IllegalArgumentException( "Can't have blank titles for samples" );

                Collection<String> bonusWords = getMicroarrayStringsToMatch( targetTitle );

                // log.debug( bonusWords.size() + " bonus words" );

                /*
                 * For each of the other data sets
                 */
                for ( String datasetOrPlatformB : valuesToUse ) {

                    // if ( alreadyTestedDatasets.contains( datasetB ) ) {
                    // log.debug( "Skip self" );
                    // continue;
                    // }
                    if ( datasetOrPlatformB.equals( datasetOrPlatformA ) ) {
                        continue;
                    }

                    // initialize data structure.
                    if ( alreadyMatched.get( targetAcc ) == null ) {
                        alreadyMatched.put( targetAcc, new HashSet<String>() );
                    }

                    /*
                     * Keep us from getting multiple matches.
                     */
                    if ( alreadyMatched.get( targetAcc ).contains( datasetOrPlatformB ) ) {
                        continue;
                    }

                    // find the best match in this data set.
                    double mindistance = Double.MAX_VALUE;
                    String bestMatch = null;
                    String bestMatchAcc = null;

                    int numTested = 0;
                    for ( int i = 0; i < sampleAccs.size(); i++ ) {

                        String testAcc = sampleAccs.get( i );

                        if ( allMatched.contains( testAcc ) ) continue;

                        boolean shouldTest = shouldTest( accToDatasetOrPlatform, alreadyMatched, datasetOrPlatformA,
                                targetAcc, datasetOrPlatformB, testAcc );

                        if ( !shouldTest ) continue;

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

                        double bonus = 0.0;
                        bonusWords.addAll( getMicroarrayStringsToMatch( testTitle ) );
                        for ( String n : bonusWords ) {
                            if ( testTitle.contains( n ) ) {
                                log.debug( testTitle + " gets a bonus in matching " + targetTitle );
                                bonus = 1; // this basically means we discount that difference.
                                break;
                            }
                        }

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
                        trimmedTest = trimmedTest.replaceAll( PUNCTUATION_REGEXP, "" );
                        trimmedTarget = trimmedTarget.replaceAll( PUNCTUATION_REGEXP, "" );

                        // Computing the distance
                        double distance = computeDistance( trimmedTest, trimmedTarget );

                        distance -= bonus;

                        double normalizedDistance = distance / Math.max( trimmedTarget.length(), trimmedTest.length() );

                        double secondaryDistance = Double.MAX_VALUE;
                        if ( targetSecondaryTitle != null && testSecondaryTitle != null ) {
                            secondaryDistance = computeDistance( targetSecondaryTitle, testSecondaryTitle );

                            if ( secondaryDistance < distance ) {
                                distance = secondaryDistance;
                                normalizedDistance = distance
                                        / Math.max( targetSecondaryTitle.length(), testSecondaryTitle.length() );
                            }
                        }

                        if ( !meetsMinimalThreshold( normalizedDistance ) ) {
                            continue;
                        }

                        // better than last one?
                        if ( distance > mindistance ) {
                            // log.debug( "Didn't beat best previous match, " + bestMatch );
                            continue;
                        }

                        // handle ties
                        if ( distance == mindistance ) {
                            wasTied = true;
                            /*
                             * Try to resolve the tie. Messy, yes.
                             */
                            double prefixWeightedDistanceA = StringDistance.prefixWeightedHammingDistance( targetAcc,
                                    bestMatchAcc, 1.0 );
                            double prefixWeightedDistanceB = StringDistance.prefixWeightedHammingDistance( targetAcc,
                                    testAcc, 1.0 );
                            if ( prefixWeightedDistanceA == prefixWeightedDistanceB ) {
                                double suffixWeightedDistanceA = StringDistance.suffixWeightedHammingDistance(
                                        targetAcc, bestMatchAcc, 1.0 );
                                double suffixWeightedDistanceB = StringDistance.suffixWeightedHammingDistance(
                                        targetAcc, testAcc, 1.0 );
                                if ( prefixWeightedDistanceA == prefixWeightedDistanceB ) {
                                    continue; // still tied, keep old one
                                } else if ( suffixWeightedDistanceA < suffixWeightedDistanceB ) {
                                    // new one is better.
                                    mindistance = distance;
                                    bestMatch = testTitle;
                                    bestMatchAcc = testAcc;
                                    log.debug( "Current best match (tie broken): "
                                            + testAcc
                                            + " ("
                                            + datasetOrPlatformB
                                            + ") "
                                            + testTitle
                                            + ( testSecondaryTitle == null ? "" : " a.k.a " + testSecondaryTitle
                                                    + ", distance = " + distance ) );
                                    wasTied = false;
                                }
                                if ( suffixWeightedDistanceA > suffixWeightedDistanceB ) {
                                    // old one is still better.
                                    wasTied = false;
                                    continue;
                                }
                            } else if ( prefixWeightedDistanceA > prefixWeightedDistanceB ) {
                                // new one is better.
                                mindistance = distance;
                                bestMatch = testTitle;
                                bestMatchAcc = testAcc;
                                log.debug( "Current best match (tie broken): "
                                        + testAcc
                                        + " ("
                                        + datasetOrPlatformB
                                        + ") "
                                        + testTitle
                                        + ( testSecondaryTitle == null ? "" : " a.k.a " + testSecondaryTitle
                                                + ", distance = " + distance ) );
                                wasTied = false;
                            } else if ( prefixWeightedDistanceA < prefixWeightedDistanceB ) {
                                wasTied = false;
                                continue; // old best is still better.
                            }
                        } else {
                            // definite new winner no tie
                            mindistance = distance;
                            bestMatch = testTitle;
                            bestMatchAcc = testAcc;
                            log.debug( "Current best match: "
                                    + testAcc
                                    + " ("
                                    + datasetOrPlatformB
                                    + ") "
                                    + testTitle
                                    + ( testSecondaryTitle == null ? "" : " a.k.a " + testSecondaryTitle
                                            + ", distance = " + distance ) );
                            wasTied = false;
                        }

                    } // end loop over samples in second data set.
                    log.debug( "Tested " + numTested + " samples" );

                    /*
                     * Now have the best hit for sample from the outer dataset, in the inner data set.
                     */
                    if ( bestMatchAcc == null || wasTied ) {
                        if ( log.isDebugEnabled() )
                            log.debug( "No match found in "
                                    + datasetOrPlatformB
                                    + " for "
                                    + targetAcc
                                    + "\t"
                                    + targetTitle
                                    + " ("
                                    + datasetOrPlatformA
                                    + ") (This can happen if sample was not run on all the platforms used; or if there were ties that could not be broken; or when we were unable to match)" );
                        result.addCorrespondence( targetAcc, null );
                        allMatched.add( targetAcc );
                    } else {
                        if ( log.isDebugEnabled() )
                            log.debug( "Match:\n" + targetAcc + "\t" + targetTitle + " ("
                                    + accToDataset.get( targetAcc ) + ")" + "\n" + bestMatchAcc + "\t" + bestMatch
                                    + " (" + accToDataset.get( bestMatchAcc ) + ")" + " (Distance: " + mindistance
                                    + ")" );
                        result.addCorrespondence( targetAcc, bestMatchAcc );
                        alreadyMatched.get( bestMatchAcc ).add( datasetOrPlatformA );
                        alreadyMatched.get( targetAcc ).add( datasetOrPlatformB );
                        allMatched.add( targetAcc );
                        allMatched.add( bestMatchAcc );
                    }

                } // loop second data sets

            } // loop over samples in first data set
        } // loop over data sets

        log.debug( result );
        return result;
    }

    private void sortDataSets( final List<String> sampleAccs, List<String> dataSets ) {
        Collections.sort( dataSets, new Comparator<String>() {
            @Override
            public int compare( String arg0, String arg1 ) {
                int numSamples0 = 0;
                int numSamples1 = 0;
                for ( int j = 0; j < sampleAccs.size(); j++ ) {
                    String targetAcc = sampleAccs.get( j );

                    // skip samples that are not in this data set.
                    if ( accToDataset.get( targetAcc ).equals( arg0 ) ) {
                        numSamples0++;
                    } else if ( accToDataset.get( targetAcc ).equals( arg1 ) ) {
                        numSamples1++;
                    }
                }

                if ( numSamples0 == numSamples1 ) {
                    return 0;
                } else if ( numSamples0 < numSamples1 ) {
                    return -1;
                } else {
                    return 1;
                }
            }
        } );
    }

    private void sortPlatforms( final List<String> sampleAccs, List<String> platforms ) {
        Collections.sort( platforms, new Comparator<String>() {
            @Override
            public int compare( String arg0, String arg1 ) {
                int numSamples0 = 0;
                int numSamples1 = 0;
                for ( int j = 0; j < sampleAccs.size(); j++ ) {
                    String targetAcc = sampleAccs.get( j );

                    // skip samples that are not in this data set.
                    if ( accToPlatform.get( targetAcc ).equals( arg0 ) ) {
                        numSamples0++;
                    } else if ( accToPlatform.get( targetAcc ).equals( arg1 ) ) {
                        numSamples1++;
                    }
                }

                if ( numSamples0 == numSamples1 ) {
                    return 0;
                } else if ( numSamples0 < numSamples1 ) {
                    return -1;
                } else {
                    return 1;
                }
            }
        } );
    }

    /**
     * Identify stop-strings relating to microarray names.
     * 
     * @param title
     * @return
     */
    private Collection<String> getMicroarrayStringsToMatch( String title ) {
        Collection<String> result = new HashSet<String>();
        for ( String key : microarrayNameStrings.keySet() ) {
            if ( title.contains( key ) ) {
                for ( String value : microarrayNameStrings.get( key ) ) {
                    if ( title.contains( value ) ) {
                        result.add( value );
                    }
                }
            }
        }
        return result;
    }

    /**
     * Implements constraints on samples to test.
     * 
     * @param accToDatasetOrPlatform (depending on which we are using, platforms or data sets)
     * @param alreadyMatched
     * @param allmatched
     * @param datasetA
     * @param targetAcc
     * @param datasetB
     * @param testAcc
     * @return
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
            log.debug( testAcc + " From wrong organism" );
            shouldTest = false;
        }
        return shouldTest;
    }

    private boolean meetsMinimalThreshold( double normalizedDistance ) {
        if ( normalizedDistance > SIMILARITY_THRESHOLD ) {
            return false;
        }
        return true;
    }

    /**
     * compute the distance.
     * 
     * @param trimmedTest
     * @param trimmedTarget
     * @return
     */
    private int computeDistance( String trimmedTest, String trimmedTarget ) {

        return StringDistance.editDistance( trimmedTarget, trimmedTest );

    }

    /**
     * @param geoSeries
     * @return
     */
    public static Map<GeoPlatform, List<GeoSample>> getPlatformSampleMap( GeoSeries geoSeries ) {
        Map<GeoPlatform, List<GeoSample>> platformSamples = new HashMap<GeoPlatform, List<GeoSample>>();

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
     * @param dataSets
     */
    private void fillAccessionMaps( Collection<GeoDataset> dataSets ) {

        for ( GeoDataset dataset : dataSets ) {
            GeoPlatform platform = dataset.getPlatform();
            assert platform != null;
            platform.getOrganisms().add( dataset.getOrganism() );
            if ( dataset.getSubsets().size() == 0 ) {
                assert dataset.getSeries().size() > 0;
                for ( GeoSeries series : dataset.getSeries() ) {
                    for ( GeoSample sample : series.getSamples() ) {

                        if ( sample.getPlatforms().size() == 0 ) sample.addPlatform( platform );
                        fillAccessionMap( sample, dataset );
                    }
                }
            } else {
                for ( GeoSubset subset : dataset.getSubsets() ) {
                    for ( GeoSample sample : subset.getSamples() ) {

                        if ( sample.getPlatforms().size() == 0 ) sample.addPlatform( platform );

                        fillAccessionMap( sample, dataset );
                    }
                }
            }
        }
    }

    /**
     * This is used if there are no 'datasets' (GDS) to work with; we just use platforms.
     * 
     * @param series
     * @param accToTitle
     * @param accToOwneracc
     * @param accToOrganism
     * @return
     */
    private int fillAccessionMaps( GeoSeries series ) {

        Map<GeoPlatform, List<GeoSample>> platformSamples = getPlatformSampleMap( series );

        for ( GeoPlatform platform : platformSamples.keySet() ) {
            for ( GeoSample sample : platformSamples.get( platform ) ) {
                assert sample != null : "Null sample for platform " + platform.getDescription();
                fillAccessionMap( sample, platform );
            }
        }

        return platformSamples.keySet().size();
    }

    /**
     * @param sample
     * @param accToTitle
     * @param accToDataset
     */
    private void fillAccessionMap( GeoSample sample, GeoData owner ) {
        String title = sample.getTitle();
        if ( StringUtils.isNotBlank( title ) ) {
            accToTitle.put( sample.getGeoAccession(), title );
        }
        accToDataset.put( sample.getGeoAccession(), owner.getGeoAccession() );
        accToSecondaryTitle.put( sample.getGeoAccession(), sample.getTitleInDataset() ); // could be null.
        String organism = getSampleOrganism( sample );
        if ( StringUtils.isNotBlank( organism ) ) {
            accToOrganism.put( sample.getGeoAccession(), organism );
        }
    }

    /**
     * @param sample
     * @return
     */
    private String getSampleOrganism( GeoSample sample ) {
        Collection<GeoPlatform> platforms = sample.getPlatforms();
        assert platforms.size() > 0 : sample + " had no platform assigned";
        GeoPlatform platform = platforms.iterator().next();
        Collection<String> organisms = platform.getOrganisms();
        assert organisms.size() > 0;
        String organism = organisms.iterator().next();
        return organism;
    }

    /**
     * Used to help ignore identifiers of microarrays in sample titles.
     */
    private static Map<String, Collection<String>> microarrayNameStrings = new HashMap<String, Collection<String>>();

    static {
        // note : all lower case!
        microarrayNameStrings.put( "u133", new HashSet<String>() );
        microarrayNameStrings.put( "u95", new HashSet<String>() );
        microarrayNameStrings.put( "u74", new HashSet<String>() );
        microarrayNameStrings.put( "v2", new HashSet<String>() );
        microarrayNameStrings.put( "chip", new HashSet<String>() );
        microarrayNameStrings.get( "u133" ).add( "u133A" );
        microarrayNameStrings.get( "u133" ).add( "u133B" );
        microarrayNameStrings.get( "u95" ).add( "u95A" );
        microarrayNameStrings.get( "u95" ).add( "u95B" );
        microarrayNameStrings.get( "u95" ).add( "u95C" );
        microarrayNameStrings.get( "u95" ).add( "u95D" );
        microarrayNameStrings.get( "u95" ).add( "u95E" );
        microarrayNameStrings.get( "u74" ).add( "u74A" );
        microarrayNameStrings.get( "u74" ).add( "u74B" );
        microarrayNameStrings.get( "u74" ).add( "u74C" );
        microarrayNameStrings.get( "v2" ).add( "av2" );
        microarrayNameStrings.get( "v2" ).add( "av2" );
        microarrayNameStrings.get( "v2" ).add( "av2" );
        microarrayNameStrings.get( "chip" ).add( "chip a" );
        microarrayNameStrings.get( "chip" ).add( "chip b" );
        microarrayNameStrings.get( "chip" ).add( "chip c" );
        microarrayNameStrings.get( "chip" ).add( "chipa" );
        microarrayNameStrings.get( "chip" ).add( "chipb" );
        microarrayNameStrings.get( "chip" ).add( "chipc" );
    }

}

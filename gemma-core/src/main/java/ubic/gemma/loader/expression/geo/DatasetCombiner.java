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
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.math.StringDistance;
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

    private final double LENGTH_DIFFERENCE_THRESHOLD_TO_TRIGGER_TRIMMING = 1.2;
    /**
     * 
     */
    private static final String GSE_RECORD_REGEXP = "(GSE\\d+)\\srecord";
    /**
     * 
     */
    private static final String ENTREZ_GEO_QUERY_URL_SUFFIX = "[Accession]&cmd=search";
    /**
     * 
     */
    private static final String ENTREZ_GEO_QUERY_URL_BASE = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gds&term=";

    /**
     * Threshold normalized similarity between two strings before we bother to make a match. The normalized similarity
     * is the ratio between the unnormalized edit distance and the length of the longer of the two strings. This is used
     * as a maximum distance (the pair of descriptors must be at least this close).
     */
    private final double SIMILARITY_THRESHOLD = 0.5;

    /**
     * Use d for short strings.
     * 
     * @see SIMILARITY_THRESHOLD
     * @see SHORT_STRING_THRESHOLD
     */
    private final double SHORT_STRING_SIMILARITY_THRESHOLD = 0.5;

    /**
     * At this length a string is considered "short".
     */
    private final int SHORT_STRING_THRESHOLD = 20;

    public DatasetCombiner() {
    }

    private static Log log = LogFactory.getLog( DatasetCombiner.class.getName() );

    LinkedHashMap<String, String> accToPlatform = new LinkedHashMap<String, String>();
    LinkedHashMap<String, String> accToTitle = new LinkedHashMap<String, String>();
    LinkedHashMap<String, String> accToDataset = new LinkedHashMap<String, String>();
    LinkedHashMap<String, String> accToOrganism = new LinkedHashMap<String, String>();
    LinkedHashMap<String, String> accToSecondaryTitle = new LinkedHashMap<String, String>();

    /**
     * Given a GDS, find the corresponding GSE.
     * 
     * @param datasetAccession
     * @return
     */
    public static String findGSEforGDS( String datasetAccession ) {
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
            InputStream is = conn.getInputStream();
            BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
            String line = null;
            while ( ( line = br.readLine() ) != null ) {
                Matcher mat = pat.matcher( line );

                if ( mat.find() ) {
                    String capturedAccession = mat.group( 1 );
                    associatedSeriesAccession.add( capturedAccession );
                }
            }
            is.close();
        } catch ( MalformedURLException e ) {
            log.error( e, e );
            throw new RuntimeException( "Invalid URL " + url, e );
        } catch ( IOException e ) {
            log.error( e, e );
            throw new RuntimeException( "Could not get data from remote server", e );
        }

        if ( associatedSeriesAccession.size() > 1 ) {
            throw new UnsupportedOperationException( "Multiple GSE per GDS not supported." );
        }

        if ( associatedSeriesAccession.size() == 0 ) {
            throw new IllegalStateException( "No GSE found for " + datasetAccession );
        }

        return associatedSeriesAccession.iterator().next();

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
     * Given a GEO series id, find all associated data sets.
     * 
     * @param seriesAccession
     * @return a collection of associated GDS accessions. If no GDS is found, the collection will be empty.
     */
    public static Collection<String> findGDSforGSE( String seriesAccession ) {
        /*
         * go from GSE to GDS, using screen scraping.
         */
        // http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gds&term=GSE674[Accession]&cmd=search
        // grep on "GDS[[digits]] record"
        URL url = null;

        Pattern pat = Pattern.compile( "(GDS\\d+)\\srecord" );

        Collection<String> associatedDatasetAccessions = new HashSet<String>();

        try {
            url = new URL( ENTREZ_GEO_QUERY_URL_BASE + seriesAccession + ENTREZ_GEO_QUERY_URL_SUFFIX );

            URLConnection conn = url.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
            String line = null;
            while ( ( line = br.readLine() ) != null ) {
                Matcher mat = pat.matcher( line );

                if ( mat.find() ) {
                    String capturedAccession = mat.group( 1 );
                    associatedDatasetAccessions.add( capturedAccession );
                }
            }
            is.close();
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( "Invalid URL " + url, e );
        } catch ( UnknownHostException e ) {
            throw new RuntimeException( "Could not connect to remote server", e );
        } catch ( IOException e ) {
            throw new RuntimeException( "Could not get data from remote server", e );
        }

        return associatedDatasetAccessions;

    }

    /**
     * Try to line up samples across datasets contained in a series.
     * 
     * @param series
     * @return
     */
    public GeoSampleCorrespondence findGSECorrespondence( GeoSeries series ) {
        if ( series.getDatasets() != null && series.getDatasets().size() > 0 ) {
            return findGSECorrespondence( series.getDatasets() );
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

        int numDatasets = dataSets.size();

        fillAccessionMaps( dataSets );

        return findCorrespondence( numDatasets );
    }

    /**
     * This is the main point where comparisons are made.
     * 
     * @param numDatasets
     * @param accToTitle
     * @param accToDataset
     * @return
     */
    private GeoSampleCorrespondence findCorrespondence( int numDatasets ) {
        GeoSampleCorrespondence result = new GeoSampleCorrespondence();

        result.setAccToTitleMap( accToTitle );
        result.setAccToDatasetMap( accToDataset );
        // allocate matrix.
        double[][] matrix = new double[accToTitle.keySet().size()][accToTitle.keySet().size()];
        for ( int i = 0; i < matrix.length; i++ ) {
            Arrays.fill( matrix[i], -1.0 );
        }

        List<String> sampleAccs = new ArrayList<String>( accToTitle.keySet() );

        // using the sorted order helps find the right matches.
        Collections.sort( sampleAccs );

        Map<String, Collection<String>> alreadyMatched = new HashMap<String, Collection<String>>();

        // do it by data set, so we constrain comparing items in _this_ data set to ones in _other_ data sets.
        // The inner loops are just to get the samples in the data set being considered.
        Collection<String> alreadyTestedDatasets = new HashSet<String>();

        SortedSet<String> dataSets = new TreeSet<String>();

        dataSets.addAll( accToDataset.values() );
        log.info( dataSets.size() + " datasets" );

        if ( numDatasets == 1 ) {
            for ( String sample : sampleAccs ) {
                result.addCorrespondence( sample, null );
            }
            return result;
        }
        for ( String datasetA : dataSets ) {
            alreadyTestedDatasets.add( datasetA );
            log.debug( "Finding matches for samples in " + datasetA );

            // for each sample in this data set...
            for ( int j = 0; j < sampleAccs.size(); j++ ) {
                String targetAcc = sampleAccs.get( j );

                // skip samples that are not in this data set.
                if ( !accToDataset.get( targetAcc ).equals( datasetA ) ) {
                    continue;
                }

                String targetTitle = accToTitle.get( targetAcc ).toLowerCase();
                String targetSecondaryTitle = accToSecondaryTitle.get( targetAcc ).toLowerCase();

                log.debug( "Target: " + targetAcc + " (" + datasetA + ") " + targetTitle
                        + ( targetSecondaryTitle == null ? "" : " a.k.a " + targetSecondaryTitle ) );
                if ( StringUtils.isBlank( targetTitle ) )
                    throw new IllegalArgumentException( "Can't have blank titles for samples" );

                Collection<String> bonusWords = getMicroarrayStringsToMatch( targetTitle );

                // log.debug( bonusWords.size() + " bonus words" );

                /*
                 * For each of the other data sets
                 */
                for ( String datasetB : dataSets ) {

                    // if ( alreadyTestedDatasets.contains( datasetB ) ) {
                    // log.debug( "Skip self" );
                    // continue;
                    // }
                    if ( datasetB.equals( datasetA ) ) {
                        continue;
                    }

                    // initialize data structure.
                    if ( alreadyMatched.get( targetAcc ) == null ) {
                        alreadyMatched.put( targetAcc, new HashSet<String>() );
                    }

                    /*
                     * Keep us from getting multiple matches.
                     */
                    if ( alreadyMatched.get( targetAcc ).contains( datasetB ) ) {
                        continue;
                    }

                    // find the best match in this data set.
                    double mindistance = Double.MAX_VALUE;
                    String bestMatch = null;
                    String bestMatchAcc = null;

                    int numTested = 0;
                    for ( int i = 0; i < sampleAccs.size(); i++ ) {

                        String testAcc = sampleAccs.get( i );

                        boolean shouldTest = shouldTest( accToDataset, accToOrganism, alreadyMatched, datasetA,
                                targetAcc, datasetB, testAcc );

                        if ( !shouldTest ) continue;

                        numTested++;

                        String testTitle = accToTitle.get( testAcc ).toLowerCase();
                        String testSecondaryTitle = accToSecondaryTitle.get( testAcc ).toLowerCase();

                        if ( StringUtils.isBlank( testTitle ) )
                            throw new IllegalArgumentException( "Can't have blank titles for samples" );

                        double bonus = 0.0;
                        for ( String n : bonusWords ) {
                            if ( testTitle.contains( n ) ) {
                                // log.debug( testTitle + " gets a bonus in matching " + targetTitle );
                                bonus = 1.0; // this basically means we discount that difference.
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
                        if ( Math.max( testTitle.length(), targetTitle.length() )
                                / Math.min( testTitle.length(), targetTitle.length() ) > LENGTH_DIFFERENCE_THRESHOLD_TO_TRIGGER_TRIMMING ) {
                            if ( testTitle.length() > targetTitle.length() ) {
                                trimmedTest = testTitle.substring( 0, targetTitle.length() );
                                // log.debug( "Trimmed test title to " + trimmedTest );
                            } else {
                                trimmedTarget = targetTitle.substring( 0, testTitle.length() );
                                // log.debug( "Trimmed target title to " + trimmedTarget );
                            }
                        }

                        // Computing the distance
                        double distance = computeDistance( matrix, j, i, trimmedTest, trimmedTarget );

                        distance -= bonus;

                        double secondaryDistance = computeDistance( matrix, j, i, targetSecondaryTitle,
                                testSecondaryTitle );

                        if ( secondaryDistance < distance ) {
                            distance = secondaryDistance;
                        }

                        double normalizedDistance = ( double ) distance
                                / Math.max( trimmedTarget.length(), trimmedTest.length() );

                        if ( !meetsMinimalThreshold( testAcc, trimmedTest, trimmedTarget, distance, normalizedDistance ) ) {
                            continue;
                        }

                        // better than last one?
                        if ( distance > mindistance ) {
                            // log.debug( "Didn't beat best previous match, " + bestMatch );
                            continue;
                        }

                        // handle ties
                        if ( distance == mindistance ) {
                            log.warn( "Tie for match to " + targetAcc + ": " + bestMatchAcc + " and " + testAcc );
                            /*
                             * Try to resolve the tie. Messy, yes. FIXME, clean up.
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
                                            + datasetB
                                            + ") "
                                            + testTitle
                                            + ( testSecondaryTitle == null ? "" : " a.k.a " + testSecondaryTitle
                                                    + ", distance = " + distance ) );
                                }
                                if ( suffixWeightedDistanceA > suffixWeightedDistanceB ) {
                                    // old one is still better.
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
                                        + datasetB
                                        + ") "
                                        + testTitle
                                        + ( testSecondaryTitle == null ? "" : " a.k.a " + testSecondaryTitle
                                                + ", distance = " + distance ) );
                            } else if ( prefixWeightedDistanceA < prefixWeightedDistanceB ) {
                                continue; // old best is still better.
                            }
                        } else {
                            // clear new winner no tie
                            mindistance = distance;
                            bestMatch = testTitle;
                            bestMatchAcc = testAcc;
                            log.debug( "Current best match: "
                                    + testAcc
                                    + " ("
                                    + datasetB
                                    + ") "
                                    + testTitle
                                    + ( testSecondaryTitle == null ? "" : " a.k.a " + testSecondaryTitle
                                            + ", distance = " + distance ) );
                        }

                    } // end loop over samples in second data set.
                    log.debug( "Tested " + numTested + " samples" );

                    /*
                     * Now have the best hit for outer dataset, in the inner data set.
                     */
                    if ( bestMatchAcc == null ) {
                        log.debug( "No match found in " + datasetB + " for " + targetAcc + "\t" + targetTitle + " ("
                                + datasetA + ") (This can happen if sample was not run on all the platforms used)" );
                        result.addCorrespondence( targetAcc, null );
                    } else {
                        if ( log.isDebugEnabled() )
                            log.debug( "Match:\n" + targetAcc + "\t" + targetTitle + " ("
                                    + accToDataset.get( targetAcc ) + ")" + "\n" + bestMatchAcc + "\t" + bestMatch
                                    + " (" + accToDataset.get( bestMatchAcc ) + ")" + " (Distance: " + mindistance
                                    + ")" );
                        result.addCorrespondence( targetAcc, bestMatchAcc );
                        alreadyMatched.get( bestMatchAcc ).add( datasetA );
                        alreadyMatched.get( targetAcc ).add( datasetB );
                    }

                } // loop second data sets

            } // loop over samples in first data set
        } // loop over data sets

        log.debug( result );
        return result;
    }

    private Collection<String> getMicroarrayStringsToMatch( String targetTitle ) {
        Collection<String> result = new HashSet<String>();
        boolean found = false;
        for ( String key : microarrayNameStrings.keySet() ) {
            if ( targetTitle.contains( key ) ) {
                for ( String value : microarrayNameStrings.get( key ) ) {
                    if ( found ) {
                        result.add( value );
                    }
                    if ( targetTitle.contains( value ) ) {
                        found = true;
                    }
                }
            }
            if ( found ) {
                break;
            }
        }
        return result;
    }

    /**
     * Implements constraints on samples to test.
     * 
     * @param accToDataset
     * @param accToOrganism
     * @param alreadyMatched
     * @param allmatched
     * @param datasetA
     * @param targetAcc
     * @param datasetB
     * @param testAcc
     * @return
     */
    private boolean shouldTest( LinkedHashMap<String, String> accToDataset,
            LinkedHashMap<String, String> accToOrganism, Map<String, Collection<String>> alreadyMatched,
            String datasetA, String targetAcc, String datasetB, String testAcc ) {
        boolean shouldTest = true;

        // initialize data structure.
        if ( alreadyMatched.get( testAcc ) == null ) {
            alreadyMatched.put( testAcc, new HashSet<String>() );
        }

        // screen out samples from other data sets.
        if ( !accToDataset.get( testAcc ).equals( datasetB ) ) {
            shouldTest = false;
        }

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

    private boolean meetsMinimalThreshold( String testAcc, String trimmedTest, String trimmedTarget, double distance,
            double normalizedDistance ) {
        // log.debug( testAcc + "\n" + trimmedTest + "\n" + trimmedTarget + " distance = " + distance );
        if ( Math.min( trimmedTarget.length(), trimmedTest.length() ) <= SHORT_STRING_THRESHOLD
                && normalizedDistance > SHORT_STRING_SIMILARITY_THRESHOLD ) {
            // log.debug( testAcc + " Didn't meet short string threshold, score for '" + trimmedTest + "' vs '"
            // + trimmedTarget + "' was " + normalizedDistance );
            return false;
        } else if ( normalizedDistance > SIMILARITY_THRESHOLD ) {
            // log.debug( testAcc + " Didn't meet threshold, score for '" + trimmedTest + "' vs '" + trimmedTarget
            // + "' was " + normalizedDistance );
            return false;
        }
        return true;
    }

    /**
     * compute and store the distance.
     * 
     * @param matrix
     * @param j
     * @param i
     * @param trimmedTest
     * @param trimmedTarget
     * @return
     */
    private double computeDistance( double[][] matrix, int j, int i, String trimmedTest, String trimmedTarget ) {

        double distance = -1.0;
        if ( matrix[i][j] < 0 ) {
            // if ( Math.max( trimmedTarget.length(), trimmedTest.length() ) > SHORT_STRING_THRESHOLD ) {
            distance = StringDistance.editDistance( trimmedTarget, trimmedTest );
            // } else {
            // distance = StringDistance.prefixWeightedHammingDistance( trimmedTarget, trimmedTest, 0.8 );
            // }
            // log.debug( "\n" + trimmedTarget + "\n" + trimmedTest + " " + distance );
            // matrix[j][i] = distance;
            // matrix[i][j] = distance;
        } else {
            // distance = matrix[i][j];
        }
        return distance;
    }

    /**
     * @param geoSeries
     * @return
     */
    public Map<GeoPlatform, List<GeoSample>> getPlatformSampleMap( GeoSeries geoSeries ) {
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
     * @param accToTitle
     * @param accToDataset
     * @param accToOrganism
     */
    private void fillAccessionMaps( Collection<GeoDataset> dataSets ) {
        for ( GeoDataset dataset : dataSets ) {
            for ( GeoSubset subset : dataset.getSubsets() ) {
                for ( GeoSample sample : subset.getSamples() ) {
                    fillAccessionMap( sample, dataset );
                }
            }
        }
    }

    /**
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
        if ( StringUtils.isBlank( title ) ) {
            return; // the same sample may show up more than once with a blank title.
        }
        accToTitle.put( sample.getGeoAccession(), title );
        accToDataset.put( sample.getGeoAccession(), owner.getGeoAccession() );
        accToSecondaryTitle.put( sample.getGeoAccession(), sample.getTitleInDataset() ); // could be null.
        String organism = getSampleOrganism( sample );

        accToOrganism.put( sample.getGeoAccession(), organism );
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

    private static Map<String, Collection<String>> microarrayNameStrings = new HashMap<String, Collection<String>>();

    static {
        microarrayNameStrings.put( "U133", new HashSet<String>() );
        microarrayNameStrings.put( "U95", new HashSet<String>() );
        microarrayNameStrings.put( "U74", new HashSet<String>() );
        microarrayNameStrings.put( "v2", new HashSet<String>() );
        microarrayNameStrings.put( "Chip", new HashSet<String>() );
        microarrayNameStrings.get( "U133" ).add( "U133A" );
        microarrayNameStrings.get( "U133" ).add( "U133B" );
        microarrayNameStrings.get( "U95" ).add( "U95A" );
        microarrayNameStrings.get( "U95" ).add( "U95B" );
        microarrayNameStrings.get( "U95" ).add( "U95C" );
        microarrayNameStrings.get( "U95" ).add( "U95D" );
        microarrayNameStrings.get( "U95" ).add( "U95E" );
        microarrayNameStrings.get( "U74" ).add( "U74A" );
        microarrayNameStrings.get( "U74" ).add( "U74B" );
        microarrayNameStrings.get( "U74" ).add( "U74C" );
        microarrayNameStrings.get( "v2" ).add( "Av2" );
        microarrayNameStrings.get( "v2" ).add( "Bv2" );
        microarrayNameStrings.get( "v2" ).add( "Cv2" );
        microarrayNameStrings.get( "Chip" ).add( "Chip A" );
        microarrayNameStrings.get( "Chip" ).add( "Chip B" );
        microarrayNameStrings.get( "Chip" ).add( "Chip C" );
    }

}

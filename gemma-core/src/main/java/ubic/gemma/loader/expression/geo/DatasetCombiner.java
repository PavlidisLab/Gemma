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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.math.StringDistance;
import ubic.gemma.loader.expression.geo.model.GeoDataset;
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
 * <li>Rarely, there can be two series, as well as two data sets, for the situation described above. I haven't seen one
 * of these yet.</li>
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
    private static final double SIMILARITY_THRESHOLD = 0.2;

    private DatasetCombiner() {
        // nobody can instantiate this class.
    }

    private static Log log = LogFactory.getLog( DatasetCombiner.class.getName() );

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
    public static Collection<String> findGDSforGDS( String datasetAccession ) {
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
    public static GeoSampleCorrespondence findGSECorrespondence( GeoSeries series ) {
        return findGSECorrespondence( series.getDatasets() );
    }

    /**
     * Try to line up samples across datasets.
     * 
     * @param dataSets
     */
    public static GeoSampleCorrespondence findGSECorrespondence( Collection<GeoDataset> dataSets ) {

        if ( dataSets == null ) return null;

        GeoSampleCorrespondence result = new GeoSampleCorrespondence();
        LinkedHashMap<String, String> accToTitle = new LinkedHashMap<String, String>();
        LinkedHashMap<String, String> accToDataset = new LinkedHashMap<String, String>();

        // get all the 'title's of the GSMs.
        for ( GeoDataset dataset : dataSets ) {
            for ( GeoSubset subset : dataset.getSubsets() ) {
                for ( GeoSample sample : subset.getSamples() ) {

                    assert sample != null : "Null sample for subset " + subset.getDescription();

                    String title = sample.getTitle();
                    if ( StringUtils.isBlank( title ) ) {
                        continue; // the same sample may show up more than once with a blank title.
                    }
                    accToTitle.put( sample.getGeoAccession(), title );
                    accToDataset.put( sample.getGeoAccession(), dataset.getGeoAccession() );
                }
            }
        }

        // allocate matrix.
        int[][] matrix = new int[accToTitle.keySet().size()][accToTitle.keySet().size()];
        for ( int i = 0; i < matrix.length; i++ ) {
            Arrays.fill( matrix[i], -1 );
        }

        // this is purely for making tests.
        // StringBuilder buf = new StringBuilder();
        // for ( String acc : accToTitle.keySet() ) {
        // buf.append( "\"" + acc + "\"," );
        // }
        // System.err.println( buf );

        List<String> sampleAccs = new ArrayList<String>( accToTitle.keySet() );

        // using the sorted order helps find the right matches.
        Collections.sort( sampleAccs );

        // do pairwise comparisons of all samples.
        for ( int j = 0; j < sampleAccs.size(); j++ ) {
            String targetAcc = sampleAccs.get( j );
            int mindistance = Integer.MAX_VALUE;
            String bestMatch = null;
            String bestMatchAcc = null;
            String iTitle = accToTitle.get( targetAcc );

            if ( StringUtils.isBlank( iTitle ) )
                throw new IllegalArgumentException( "Can't have blank titles for samples" );

            for ( int i = 0; i < sampleAccs.size(); i++ ) {
                if ( i == j ) continue;

                String testAcc = sampleAccs.get( i );

                String jTitle = accToTitle.get( testAcc );
                if ( StringUtils.isBlank( jTitle ) )
                    throw new IllegalArgumentException( "Can't have blank titles for samples" );

                int distance = -1;
                if ( matrix[i][j] < 0 ) {
                    distance = StringDistance.editDistance( iTitle, jTitle );
                    matrix[j][i] = distance;
                    matrix[i][j] = distance;
                } else {
                    distance = matrix[i][j];
                }

                double normalizedDistance = ( double ) distance / Math.max( iTitle.length(), jTitle.length() );

                // make sure match is to sample in another data set, as well as being a good match.
                if ( normalizedDistance < SIMILARITY_THRESHOLD && distance <= mindistance
                        && accToDataset.get( targetAcc ) != accToDataset.get( testAcc ) ) {
                    mindistance = distance;
                    bestMatch = jTitle;
                    bestMatchAcc = testAcc;
                }
            }

            assert targetAcc != null;
            result.addCorrespondence( targetAcc, bestMatchAcc );

            if ( dataSets.size() > 1 ) {
                if ( bestMatchAcc == null ) {
                    log.warn( "No match found for:\n" + targetAcc + "\t" + iTitle + "\n" );
                } else {
                    log.info( "Match:\n" + targetAcc + "\t" + iTitle + "\n" + bestMatchAcc + "\t" + bestMatch
                            + " (Distance: " + mindistance + ")\n" );
                }
            }
        }

        return result;
    }
}

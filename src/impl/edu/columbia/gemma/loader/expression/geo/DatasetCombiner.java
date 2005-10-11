/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.loader.expression.geo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import baseCode.math.StringDistance;

import edu.columbia.gemma.loader.expression.geo.model.GeoDataset;
import edu.columbia.gemma.loader.expression.geo.model.GeoSample;
import edu.columbia.gemma.loader.expression.geo.model.GeoSubset;

/**
 * Class to handle cases where there are multiple GEO dataset for a single actual experiment. This can occur in at least
 * two ways:
 * <ol>
 * <li>There is a single GSE (e.g., GSE674) but two datasets (GDS472, GDS473). This can happen when there are two
 * different microarrays used such as the "A" and B" HG-U133 Affymetrix arrays. (Each GDS can only refer to a single
 * platform)</li>
 * <li>Rarely, there can be two series, as well as two data sets, for the situation described above. I haven't seen one
 * of these yet!</li>
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
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class DatasetCombiner {

    private static Log log = LogFactory.getLog( DatasetCombiner.class.getName() );

    /**
     * Given a GEO series id, find all associated data sets.
     * 
     * @param seriesAccession
     * @return a collection of associated GDS accessions.
     */
    public static Collection<String> findGDSGrouping( String seriesAccession ) {
        /*
         * go from GDS-->GSE then use the Entrez e-utils to get the GDS's for the GSE. If there are multiple GDS's, we
         * do something
         */
        // http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gds&term=GSE674[Accession]&cmd=search
        // grep on "GDS[[digits]] record"
        URL url;

        Pattern pat = Pattern.compile( "(GDS\\d+)\\srecord" );

        Collection<String> associatedDatasetAccessions = new HashSet<String>();

        try {
            url = new URL( "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gds&term=" + seriesAccession
                    + "[Accession]&cmd=search" );

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
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        return associatedDatasetAccessions;

    }

    /**
     * Try to line up samples across datasets.
     * 
     * @param dataSets
     */
    public static GeoSampleCorrespondence findGSECorrespondence( Collection<GeoDataset> dataSets ) {

        if ( dataSets == null || dataSets.size() < 2 )
            throw new IllegalArgumentException( "Must be multiple data sets" );

        GeoSampleCorrespondence result = new GeoSampleCorrespondence();
        LinkedHashMap<String, String> accToTitle = new LinkedHashMap<String, String>();

        // get all the 'title's of the GSMs.
        for ( GeoDataset dataset : dataSets ) {
            Collection<GeoSubset> subsets = dataset.getSubsets();
            for ( GeoSubset subset : subsets ) {
                Collection<GeoSample> samples = subset.getSamples();
                for ( GeoSample sample : samples ) {
                    String description = sample.getTitle();
                    accToTitle.put( sample.getGeoAccesssion(), description );
                }
            }
        }

        // assuming there aren't thousands of samples....
        int[][] matrix = new int[accToTitle.keySet().size()][accToTitle.keySet().size()];
        for ( int i = 0; i < matrix.length; i++ ) {
            Arrays.fill( matrix[i], -1 );
        }

        List<String> sampleAccs = new ArrayList<String>( accToTitle.keySet() );
        Collections.sort( sampleAccs );

        Set<String> used = new HashSet<String>();

        for ( int j = 0; j < sampleAccs.size(); j++ ) {
            String testAcc = sampleAccs.get( j );
            if ( used.contains( testAcc ) ) continue;

            int mindistance = Integer.MAX_VALUE;
            String bestMatch = null;
            String bestMatchAcc = null;
            String iTitle = accToTitle.get( testAcc );

            for ( int i = 0; i < sampleAccs.size(); i++ ) {
                if ( i == j ) continue;
                String jTitle = accToTitle.get( sampleAccs.get( i ) );
                int distance = -1;
                if ( matrix[i][j] < 0 ) {
                    distance = StringDistance.editDistance( iTitle, jTitle );
                    matrix[j][i] = distance;
                    matrix[i][j] = distance;
                } else {
                    distance = matrix[i][j];
                }

                if ( distance <= mindistance && !used.contains( bestMatchAcc ) ) {
                    mindistance = distance;
                    bestMatch = jTitle;
                    bestMatchAcc = sampleAccs.get( i );
                }
            }

            if ( used.contains( bestMatchAcc ) ) {
                throw new IllegalStateException( bestMatchAcc + " is already the best match for a sample!" );
            }

            used.add( bestMatchAcc );
            result.addCorrespondence( testAcc, bestMatchAcc );

            log.debug( "Seeking match for " + iTitle + ": found " + bestMatch );
            log.debug( testAcc + " <====> " + bestMatchAcc );
        }

        return result;
    }
}

class GeoSampleCorrespondence {

    Map<String, Collection<String>> map = new HashMap<String, Collection<String>>();

    /**
     * @param gsmNumber
     * @return Collection of sample accession values that correspond to the argument.
     */
    public Collection<String> getCorrespondingSamples( String gsmNumber ) {
        return this.map.get( gsmNumber );
    }

    public void addCorrespondence( String gsmNumberA, String gsmNumberB ) {
        if ( !map.containsKey( gsmNumberA ) ) map.put( gsmNumberA, new HashSet<String>() );
        if ( !map.containsKey( gsmNumberB ) ) map.put( gsmNumberB, new HashSet<String>() );
        map.get( gsmNumberA ).add( gsmNumberB );
        map.get( gsmNumberB ).add( gsmNumberA );
    }

}

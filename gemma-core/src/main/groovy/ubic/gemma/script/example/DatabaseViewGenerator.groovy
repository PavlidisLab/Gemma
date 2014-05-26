/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.script.example
 
import ubic.gemma.script.framework.SpringSupport;
import ubic.gemma.util.Settings;

import java.io.File;
import java.util.Collection;
import java.util.zip.GZIPOutputStream;

import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * From ubc.gemma.analysis.report.DatabaseViewGenerator
 */
sx = new SpringSupport();
taxonService = sx.getBean("taxonService");
geneService = sx.getBean("geneService");
differentialExpressionResultService = sx.getBean("differentialExpressionResultService")
differentialExpressionAnalysisService = sx.getBean("differentialExpressionAnalysisService")
gdeService = sx.getBean("geneDifferentialExpressionService")
compositeSequenceService = sx.getBean("compositeSequenceService")
expressionExperimentService = sx.getBean("expressionExperimentService")

//TODO Change me
//limit = 4
limit = 0

THRESH_HOLD = 0.01;
DATASET_SUMMARY_VIEW_BASENAME = "DatasetSummary";
DATASET_TISSUE_VIEW_BASENAME = "DatasetTissue";
DATASET_DIFFEX_VIEW_BASENAME = "DatasetDiffEx";
VIEW_DIR = Settings.getString( "gemma.appdata.home" ) + File.separatorChar + "dataFiles" + File.separatorChar;
VIEW_FILE_SUFFIX = ".view.txt.gz";

/**
 * @param datasetDiffexViewBasename
 * @return
 */
private File getViewFile( String datasetDiffexViewBasename ) {
    return getOutputFile( datasetDiffexViewBasename + VIEW_FILE_SUFFIX );
}

/*
 * (non-Javadoc)
 *
 * @see ubic.gemma.analysis.report.DatabaseViewGenerator#getOutputFile(java.lang.String)
 */
@Override
public File getOutputFile( String filename ) {
    String fullFilePath = VIEW_DIR + filename;
    File f = new File( fullFilePath );

    if ( f.exists() ) {
        return f;
    }

    File parentDir = f.getParentFile();
    if ( !parentDir.exists() ) parentDir.mkdirs();
    return f;
}

/**
 * @param probeAnalysisResult
 * @return
 */
private String formatDiffExResult( ExpressionExperiment ee,
        DifferentialExpressionAnalysisResult probeAnalysisResult, String factorName, String factorURI,
        String baselineDescription ) {

    CompositeSequence cs = probeAnalysisResult.getProbe();

    Collection<Gene> genes = compositeSequenceService.getGenes( cs );

    if ( genes.isEmpty() || genes.size() > 1 ) {
        return null;
    }

    Gene g = genes.iterator().next();

    if ( g.getNcbiGeneId() == null ) return null;

    Collection<ContrastResult> contrasts = probeAnalysisResult.getContrasts();

    StringBuilder buf = new StringBuilder();
    for ( ContrastResult cr : contrasts ) {
        FactorValue factorValue = cr.getFactorValue();
		
        String direction = cr.getLogFoldChange() < 0 ? "-" : "+";

        String factorValueDescription = "null";
        try {
			factorValueDescription = ExperimentalDesignUtils.prettyString( factorValue );
		} catch(Exception e) {
			e.printStackTrace()
		}
        String formatted = String.format( "%d\t%s\t%s\t%d\t%s\t%s\t%s\t%s\t%s\n", ee.getId(), ee.getShortName(), g
                    .getNcbiGeneId().toString(), g.getId(), factorName, factorURI, baselineDescription,
                    factorValueDescription, direction );
        buf.append( formatted );
    }

    return buf.toString();
}
        
/*
* Get handle to output file
*/
File file = getViewFile( DATASET_DIFFEX_VIEW_BASENAME );
System.out.println( "Writing to " + file );
Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) );

/*
* Load all the data sets
*/Collection<ExpressionExperiment> experiments = expressionExperimentService.loadAll();

/*
* For each gene that is differentially expressed, print out a line per contrast
*/
writer.write( "GemmaDsId\tEEShortName\tGeneNCBIId\tGemmaGeneId\tFactor\tFactorURI\tBaseline\tContrasting\tDirection\n" );
int i = 0;
String lastFormatted = "";
for ( ExpressionExperiment ee : experiments ) {
	
    //ee = expressionExperimentService.thawLite( ee );
    ee = expressionExperimentService.thaw( ee );
    
    Collection<DifferentialExpressionAnalysis> results = differentialExpressionAnalysisService.getAnalyses( ee );
    if ( results == null || results.isEmpty() ) {
    	System.out.println( "No differential expression results found for " + ee );
    	continue;
    }
    
    if ( results.size() > 1 ) {
    	/*
    	 * FIXME. Should probably skip for this purpose.
    	 */
    }
    
    System.out.println( "Processing: " + ee.getShortName() );
    
    try {
		for ( DifferentialExpressionAnalysis analysis : results ) {
		
			// this might take a while ...
			analysis = differentialExpressionAnalysisService.thawFully(analysis)
			
			for ( ExpressionAnalysisResultSet ears : analysis.getResultSets() ) {
		
				ears = differentialExpressionResultService.thaw( ears );
		
				FactorValue baselineGroup = ears.getBaselineGroup();
		
				if ( baselineGroup == null ) {
					// System.out.println( "No baseline defined for " + ee ); // interaction
					continue;
				}
		
				if ( ExperimentalDesignUtils.isBatch( baselineGroup.getExperimentalFactor() ) ) {
					continue;
				}
		
				String baselineDescription = ExperimentalDesignUtils.prettyString( baselineGroup );
		
				// Get the factor category name
				String factorName = "";
				String factorURI = "";
		
				for ( ExperimentalFactor ef : ears.getExperimentalFactors() ) {
					factorName += ef.getName() + ",";
					if ( ef.getCategory() instanceof VocabCharacteristic ) {
						factorURI += ( ( VocabCharacteristic ) ef.getCategory() ).getCategoryUri() + ",";
					}
				}
				factorName = StringUtils.removeEnd( factorName, "," );
				factorURI = StringUtils.removeEnd( factorURI, "," );
		
				if ( ears.getResults() == null || ears.getResults().isEmpty() ) {
					System.out.println( "No  differential expression analysis results found for " + ee );
					continue;
				}
		
				// Generate probe details
				for ( DifferentialExpressionAnalysisResult dear : ears.getResults() ) {
		
					if ( dear == null ) {
						System.out.println( "Missing results for " + ee + " skipping to next. " );
						continue;
					}
		
					if ( dear.getCorrectedPvalue() == null || dear.getCorrectedPvalue() > THRESH_HOLD ) continue;
		
					String formatted = formatDiffExResult( ee, dear, factorName, factorURI, baselineDescription );
					
					if ( StringUtils.isNotBlank( formatted ) && !formatted.equals(lastFormatted) ) writer.write( formatted );
					lastFormatted = formatted;
		
				} // dear loop
			} // ears loop
		} // analysis loop
    } catch(Exception e) {
		e.printStackTrace()
		System.out.println "Error occured while processing experiment " + ee.getShortName()
	}
	
	i++
	
    if (i % 100 == 0) {
        System.out.println "${new Date()}: Processed $i experiments ...";
    }
    
    if ( i > limit && limit > 0 ) break;

}// EE loop
writer.close();

System.out.println "${new Date()}: Written $i experiments to $file.";

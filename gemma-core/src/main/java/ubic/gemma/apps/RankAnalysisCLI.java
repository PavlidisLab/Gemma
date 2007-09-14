package ubic.gemma.apps;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Locale;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.basecode.dataStructure.matrix.AbstractNamedMatrix;
import ubic.basecode.io.writer.MatrixWriter;
import ubic.gemma.analysis.preprocess.DedvRankService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

public class RankAnalysisCLI extends
		AbstractGeneExpressionExperimentManipulatingCLI {
	
	private DedvRankService dedvRankService;
	
	private String outFilePrefix;
	
	protected void buildOptions() {
		super.buildOptions();
		
		Option outFilePrefixOption = OptionBuilder.isRequired().hasArg().withDescription("File prefix for saved rank matrix files").withArgName("File prefix").withLongOpt("outFilePrefix").create('o');
		addOption(outFilePrefixOption);
	}
	
	protected void processOptions() {
		super.processOptions();
		
		outFilePrefix = getOptionValue('o');
		
		dedvRankService = (DedvRankService) getBean("dedvRankService");
	}

	@Override
	protected Exception doWork(String[] args) {
		processCommandLine("RankAnalysisCLI", args);
		Collection<ExpressionExperiment> ees;
		try {
			 ees = getExpressionExperiments(taxon);
		} catch (IOException e) {
			return e;
		}
		
		Collection<Gene> genes = geneService.loadGenes(taxon);
		AbstractNamedMatrix rankMatrix = dedvRankService.getRankMatrix(genes, ees, DedvRankService.Method.MEDIAN);
		
		// gene names
		Collection<Gene> rowGenes = rankMatrix.getRowNames();
		try {
			PrintWriter out = new PrintWriter(new FileWriter(outFilePrefix
					+ ".row_names.txt"));
			for (Gene gene : rowGenes) {
				String s = gene.getOfficialSymbol();
				if (s == null)
					s = gene.getId().toString();
				out.println(s);
			}
			out.close();
		} catch (IOException exc) {
			return exc;
		}

		// expression experiment names
		Collection<ExpressionExperiment> colEes = rankMatrix.getColNames();
		try {
			PrintWriter out = new PrintWriter(new FileWriter(outFilePrefix
					+ ".col_names.txt"));
			for (ExpressionExperiment ee : colEes) {
				out.println(ee.getShortName());
			}
			out.close();
		} catch (IOException exc) {
			return exc;
		}
		
		DecimalFormat formatter = (DecimalFormat) DecimalFormat
				.getNumberInstance(Locale.US);
		formatter.applyPattern("0.0000");
		formatter.getDecimalFormatSymbols().setNaN("NaN");
		try {
			MatrixWriter out = new MatrixWriter(outFilePrefix + ".txt", formatter);
			out.writeMatrix(rankMatrix, false);
		} catch (IOException e) {
			return e;
		}
		return null;
	}
	
	public static void main(String[] args) {
		RankAnalysisCLI analysis = new RankAnalysisCLI();
		Exception e = analysis.doWork(args);
		if (e != null)
			log.error(e.getMessage());
	}

}

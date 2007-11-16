package ubic.gemma.apps;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix3DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.io.writer.MatrixWriter;
import ubic.gemma.analysis.linkAnalysis.CoexpressionAnalysisService;
import ubic.gemma.analysis.linkAnalysis.CoexpressionAnalysisService.CoexpressionMatrices;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;

public class CorrelationAnalysisCLI extends
		AbstractGeneCoexpressionManipulatingCLI {
	private String outFilePrefix;

	private CoexpressionAnalysisService coexpressionAnalysisService;

	private FilterConfig filterConfig;

	private int kMax;

	public CorrelationAnalysisCLI() {
		super();
		filterConfig = new FilterConfig();
		kMax = 0;
	}

	@Override
	protected void buildOptions() {
		super.buildOptions();
		Option outputFileOption = OptionBuilder.hasArg().isRequired()
				.withArgName("File prefix").withDescription(
						"File prefix for saving the output").withLongOpt(
						"outFilePrefix").create('o');
		addOption(outputFileOption);

		Option kMaxOption = OptionBuilder.hasArg().withArgName("k")
				.withDescription("Select the kth largest value").withType(
						Integer.class).withLongOpt("kValue").create('k');
		addOption(kMaxOption);
	}

	@Override
	protected void processOptions() {
		super.processOptions();
		if (hasOption('o')) {
			this.outFilePrefix = getOptionValue('o');
		}
		if (hasOption('k')) {
			this.kMax = getIntegerOptionValue('k');
		}
		initBeans();
	}

	protected void initBeans() {
		coexpressionAnalysisService = (CoexpressionAnalysisService) this
				.getBean("coexpressionAnalysisService");
		eeService = (ExpressionExperimentService) this
				.getBean("expressionExperimentService");
	}

	@Override
	protected Exception doWork(String[] args) {
		Exception exc = processCommandLine("CorrelationAnalysis", args);
		if (exc != null) {
			return exc;
		}

		Collection<ExpressionExperiment> ees;
		Collection<Gene> queryGenes, targetGenes;
		try {
			ees = getExpressionExperiments(taxon);
			queryGenes = getQueryGenes();
			targetGenes = getTargetGenes();
		} catch (IOException e) {
			return e;
		}

		// calculate matrices
		CoexpressionMatrices matrices = coexpressionAnalysisService
				.calculateCoexpressionMatrices(ees, queryGenes, targetGenes,
						filterConfig);
		DenseDoubleMatrix3DNamed correlationMatrix = matrices
				.getCorrelationMatrix();
//		DenseDoubleMatrix3DNamed sampleSizeMatrix = matrices
//				.getSampleSizeMatrix();

//		DoubleMatrixNamed maxCorrelationMatrix = coexpressionAnalysisService
//				.getMaxCorrelationMatrix(correlationMatrix, kMax);
//		DoubleMatrixNamed pValMatrix = coexpressionAnalysisService
//				.calculateMaxCorrelationPValueMatrix(maxCorrelationMatrix,
//						kMax, ees);
//		DoubleMatrixNamed effectSizeMatrix = coexpressionAnalysisService
//				.calculateEffectSizeMatrix(correlationMatrix, sampleSizeMatrix);

		// get row/col name maps
		Map<Gene, String> geneNameMap = matrices.getGeneNameMap();
		Map<ExpressionExperiment, String> eeNameMap = matrices.getEeNameMap();

		DecimalFormat formatter = (DecimalFormat) DecimalFormat
				.getNumberInstance(Locale.US);
		formatter.applyPattern("0.0000");
		DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
		symbols.setNaN("NaN");
		formatter.setDecimalFormatSymbols(symbols);
		
		try {
			MatrixWriter matrixOut;
			matrixOut = new MatrixWriter(outFilePrefix + ".corr.txt",
					formatter);
			matrixOut.setSliceNameMap(eeNameMap);
			matrixOut.setRowNameMap(geneNameMap);
			matrixOut.setColNameMap(geneNameMap);
			matrixOut.writeMatrix(correlationMatrix, false);
			matrixOut.close();
			
			PrintWriter out = new PrintWriter(new FileWriter(outFilePrefix + ".corr.row_names.txt"));
			List rows = correlationMatrix.getRowNames();
			for (Object row : rows) {
				out.println(row);
			}
			out.close();
			
			out = new PrintWriter(new FileWriter(outFilePrefix + ".corr.col_names.txt"));
			Collection<ExpressionExperiment> cols = correlationMatrix.getSliceNames();
			for (ExpressionExperiment ee : cols) {
				out.println(ee.getShortName());
			}
			out.close();

//			out = new MatrixWriter(outFilePrefix + ".max_corr.txt", formatter,
//					geneNameMap, geneNameMap);
//			out.writeMatrix(maxCorrelationMatrix, true);
//			out.close();
//
//			out = new MatrixWriter(outFilePrefix + ".max_corr.pVal.txt",
//					formatter, geneNameMap, geneNameMap);
//			out.writeMatrix(pValMatrix, true);
//			out.close();
//
//			out = new MatrixWriter(outFilePrefix + ".effect_size.txt",
//					formatter, geneNameMap, geneNameMap);
//			out.writeMatrix(effectSizeMatrix, true);
//			out.close();

		} catch (IOException e) {
			return e;
		}

		return null;
	}

	public static void main(String[] args) {
		CorrelationAnalysisCLI analysis = new CorrelationAnalysisCLI();
		StopWatch watch = new StopWatch();
		watch.start();
		log.info("Starting Correlation Analysis");
		Exception exc = analysis.doWork(args);
		if (exc != null) {
			log.error(exc.getMessage());
		}
		log.info("Finished analysis in " + watch);
	}
}

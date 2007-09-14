package ubic.gemma.apps;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.HashMap;
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

	public CorrelationAnalysisCLI() {
		super();
		filterConfig = new FilterConfig();
	}

	@Override
	protected void buildOptions() {
		super.buildOptions();
		Option outputFileOption = OptionBuilder.hasArg().isRequired()
				.withArgName("outFilePrefix").withDescription(
						"File prefix for saving the output").withLongOpt(
						"outFilePrefix").create('o');
		addOption(outputFileOption);
	}

	@Override
	protected void processOptions() {
		super.processOptions();
		if (hasOption('o')) {
			this.outFilePrefix = getOptionValue('o');
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

		DoubleMatrixNamed maxCorrelationMatrix = coexpressionAnalysisService
				.getMaxCorrelationMatrix(correlationMatrix, 0);
		DoubleMatrixNamed correlationMatrix2D = coexpressionAnalysisService
				.foldCoexpressionMatrix(correlationMatrix);
		DoubleMatrixNamed pValMatrix = coexpressionAnalysisService
				.calculateMaxCorrelationPValueMatrix(maxCorrelationMatrix, 0,
						ees);

		// get row/col name maps
		Map<Gene, String> geneNameMap = matrices.getGeneNameMap();
		Map<ExpressionExperiment, String> eeNameMap = matrices.getEeNameMap();

		String topLeft = "GenePair";
		DecimalFormat formatter = (DecimalFormat) DecimalFormat
				.getNumberInstance(Locale.US);
		formatter.applyPattern("0.0000");
		DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
		symbols.setNaN("");
		formatter.setDecimalFormatSymbols(symbols);
		try {
			MatrixWriter out = new MatrixWriter(outFilePrefix + ".corr.txt",
					formatter);
			out.setColNameMap(eeNameMap);
			out.setTopLeft(topLeft);
			out.writeMatrix(correlationMatrix2D, true);
			out.close();

			out = new MatrixWriter(outFilePrefix + ".max_corr.txt", formatter,
					geneNameMap, geneNameMap);
			out.writeMatrix(maxCorrelationMatrix, true);
			out.close();

			out = new MatrixWriter(outFilePrefix + ".max_corr.pVal.txt",
					formatter, geneNameMap, geneNameMap);
			out.writeMatrix(pValMatrix, true);
			out.close();

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

package ubic.gemma.apps;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.analysis.linkAnalysis.CoexpressionAnalysisService;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

public class GeneExpressionProfileWriterCLI extends
		AbstractGeneCoexpressionManipulatingCLI {
	
	private ArrayDesignService adService;
	private CoexpressionAnalysisService coexpAnalysisService;
	
	private String outFilePrefix;
	
	protected void buildOptions() {
		super.buildOptions();

		Option outFilePrefixOption = OptionBuilder.isRequired().hasArg()
				.withDescription("File prefix for saved expression profiles")
				.withArgName("File prefix").withLongOpt("outFilePrefix")
				.create('o');
		addOption(outFilePrefixOption);
	}
	
	protected void processOptions() {
		super.processOptions();
		outFilePrefix = getOptionValue('o');
		
		adService = (ArrayDesignService) getBean("arrayDesignService");
		coexpAnalysisService = (CoexpressionAnalysisService) getBean("coexpressionAnalysisService");
	}

	@Override
	protected Exception doWork(String[] args) {
		processCommandLine("ExpressionProfileWriterCLI", args);
		
		Collection<Gene> genes;
		Collection<ExpressionExperiment> ees;
		try {
			genes = getQueryGenes();
			ees = getExpressionExperiments(taxon);
		} catch (IOException e) {
			return e;
		}
		
		FilterConfig filterConfig = new FilterConfig();
		for (ExpressionExperiment ee : ees) {
			Collection<ArrayDesign> ads = eeService.getArrayDesignsUsed(ee);
			Collection<CompositeSequence> css = new HashSet<CompositeSequence>();
			for (ArrayDesign ad : ads) {
				css.addAll(adService.loadCompositeSequences(ad));
			}
			Map<Gene, Collection<CompositeSequence>> gene2css = coexpAnalysisService.getGene2CsMap(css);
			ExpressionDataDoubleMatrix dataMatrix = coexpAnalysisService.getExpressionDataMatrix(ee, filterConfig);
			try {
				String fileName = outFilePrefix + "." + ee.getShortName() + ".txt";
				PrintWriter out = new PrintWriter(new FileWriter(fileName));
				for (Gene gene : genes) {
					Collection<CompositeSequence> c = gene2css.get(gene);
					for (CompositeSequence cs : c) {
						Double[] row = dataMatrix.getRow(cs);
						if (row == null) {
							log.error("Cannot get data from data matrix for " + gene.getOfficialSymbol() + " (" + cs.getName() + ")");
							continue;
						}
						StringBuffer buf = new StringBuffer();
						buf.append(gene.getOfficialSymbol() + "\t" + cs.getName() + "\t");
						for (Double d : row) {
							if (d == null)
								buf.append("NaN");
							else
								buf.append(d);
							buf.append("\t");
						}
						buf.deleteCharAt(buf.length() - 1);
						out.println(buf.toString());
					}
				}
				out.close();
			} catch (IOException e) {
				return e;
			}
		}
		
		return null;
	}
	
	public static void main(String[] args) {
		GeneExpressionProfileWriterCLI cli = new GeneExpressionProfileWriterCLI();
		Exception e = cli.doWork(args);
		if (e != null)
			log.error(e.getMessage());
	}

}

package ubic.gemma.apps;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.analysis.preprocess.filter.ExpressionExperimentFilter;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.MatrixWriter;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public class ExpressionDataMatrixWriterCLI extends
		AbstractGeneExpressionExperimentManipulatingCLI {
	
	private DesignElementDataVectorService dedvService;
	
	private String outFileName;
	protected void buildOptions() {
		super.buildOptions();
		Option outputFileOption = OptionBuilder.hasArg().isRequired()
				.withArgName("outFilePrefix").withDescription(
						"File prefix for saving the output").withLongOpt(
						"outFilePrefix").create('o');
		addOption(outputFileOption);
	}
	
	protected void processOptions() {
		super.processOptions();
		
		outFileName = getOptionValue('o');
		
		dedvService = (DesignElementDataVectorService) getBean("designElementDataVectorService");
		
	}
	
	@Override
	protected Exception doWork(String[] args) {
		processCommandLine("expressionDataMatrixWriterCLI", args);
		
		Collection<ExpressionExperiment> ees;
		try {
    		ees = getExpressionExperiments(null);
		} catch (IOException e) {
			return e;
		}
		FilterConfig filterConfig = new FilterConfig();
		
		for (ExpressionExperiment ee : ees) {
            // get quantitation types
            Collection<QuantitationType> qts;
            qts = ( Collection<QuantitationType> ) eeService.getPreferredQuantitationType( ee );
            if ( qts.size() < 1 ) {
                return null;
            }

            // get dedvs to build expression data matrix
            Collection<DesignElementDataVector> dedvs;
            dedvs = eeService.getDesignElementDataVectors(ee, qts);
            dedvService.thaw(dedvs);
            ExpressionExperimentFilter filter = new ExpressionExperimentFilter( ee, eeService.getArrayDesignsUsed( ee ),
                    filterConfig );
            ExpressionDataDoubleMatrix dataMatrix = filter.getFilteredMatrix( dedvs );
            
            try {
                MatrixWriter out = new MatrixWriter();
                PrintWriter writer = new PrintWriter(outFileName);
                out.write(writer, dataMatrix, true, false);
            } catch (IOException e) {
            	return e;
            }
		}
		
		return null;
	}
	
	public static void main(String[] args) {
		ExpressionDataMatrixWriterCLI cli = new ExpressionDataMatrixWriterCLI();
		Exception exc = cli.doWork(args);
		if (exc != null) {
			log.error(exc.getMessage());
		}
	}

}

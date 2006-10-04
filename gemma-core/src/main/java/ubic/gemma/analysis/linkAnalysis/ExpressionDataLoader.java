package ubic.gemma.analysis.linkAnalysis;

/*
* @author xiangwan
*/

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.bio.geneset.GeneAnnotations;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.analysis.diff.ExpressionDataManager;
import ubic.gemma.loader.expression.geo.GeoConverter;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;


public class ExpressionDataLoader{

    final String actualExperimentsPath = "C:/TestData/";
    final String analysisResultsPath = "C:/Results/";
    
    protected ExpressionExperiment experiment = null;
    protected String experimentName = null;
    protected static final Log log = LogFactory.getLog( ExpressionDataManager.class );
    protected GeneAnnotations geneAnnotations = null; 
    protected int uniqueItems = 0;
    protected Collection <DesignElementDataVector> designElementDataVectors = null;

	public ExpressionDataLoader(String paraExperimentName) {
		// TODO Auto-generated constructor stub
		this.experimentName = paraExperimentName;
		
        GeoDatasetService gds = new GeoDatasetService();
        GeoConverter geoConverter = new GeoConverter();
       // gds.setConverter(geoConverter);
       
        //if you want to read the experiments locally
        //gds.setGenerator( new GeoDomainObjectGeneratorLocal(actualExperimentsPath));
        
        //if you want to read the experiments from the geo repository
        //gds.setGenerator(new GeoDomainObjectGenerator());
        this.experiment = (ExpressionExperiment)gds.fetchAndLoad(experimentName);
        if(this.experiment != null) this.getValidDesignmentDataVector();
	}
	public ExpressionDataLoader(String paraExperimentName, String paraGOFile)
	{
		this(paraExperimentName);
        Set rowsToUse = new HashSet( this.getActiveProbeIdSet() );
        try
        {
        	this.geneAnnotations = new GeneAnnotations(this.actualExperimentsPath+paraGOFile, rowsToUse, null, null);
        	this.uniqueItems = this.geneAnnotations.numGenes();
        	log.info("Unique Genes: " + this.uniqueItems);
        }
        catch(IOException e)
        {
			log.error("Error in reading GO File");
        }
		
	}
	public ExpressionDataLoader(ExpressionExperiment paraExperiment)
	{
		this.experiment = paraExperiment;
        if(this.experiment != null)
        {
        	this.getValidDesignmentDataVector();
        	this.experimentName = this.experiment.getName();
        }
	}
	private Collection<String> getActiveProbeIdSet()
	{
		Collection probeIdSet = new HashSet<String>();
		for(DesignElementDataVector dataVector:this.designElementDataVectors)
		{
			DesignElement designElement = dataVector.getDesignElement();
			String probeId = ((CompositeSequence)designElement).getName();
			probeIdSet.add(probeId);
		}


		return probeIdSet;
	}
	private void getValidDesignmentDataVector()
	{
		Collection <DesignElementDataVector> dataVectors = this.experiment.getDesignElementDataVectors();
		this.designElementDataVectors = new HashSet <DesignElementDataVector>();
		for(DesignElementDataVector dataVector:dataVectors)
		{
			if(dataVector.getQuantitationType().getName().trim().equals("VALUE")
				&&
			   dataVector.getQuantitationType().getRepresentation().toString().trim().equals("DOUBLE"))
				this.designElementDataVectors.add(dataVector);
		}
	}
	public void WriteExpressionDataToFile(String paraFileName)
	{
		BufferedWriter writer = null;
		try 
		{
			writer = new BufferedWriter(new FileWriter(this.analysisResultsPath+ paraFileName));
		}
		catch( IOException e)
		{
			log.error("File for output expression data " + this.analysisResultsPath+ paraFileName + "could not be opened");
		}
		Collection <DesignElementDataVector> dataVectors = this.experiment.getDesignElementDataVectors();

		try
		{
			writer.write("Experiment Name: "+this.experimentName + "\n");
			writer.write("Accession: "+ this.experiment.getAccession().getAccession() + "\n");
			writer.write("Name: "+ this.experiment.getName() + "\n");
			writer.write("Description: "+ this.experiment.getDescription() + "\n");
			writer.write("Source: "+ this.experiment.getSource() + "\n");

			for(DesignElementDataVector dataVector:this.designElementDataVectors)
			{
				DesignElement designElement = dataVector.getDesignElement();
				CompositeSequence compSequence = (CompositeSequence)designElement;
				String probId = ((CompositeSequence)designElement).getName();
				byte[] expressionByteData = dataVector.getData();
				ByteArrayConverter byteConverter = new ByteArrayConverter();
				double [] expressionData = byteConverter.byteArrayToDoubles(expressionByteData);
				writer.write(probId + "\t");
				for(int i = 0; i < expressionData.length; i++)
					writer.write(expressionData[i] + "\t");
				writer.write(dataVector.getQuantitationType().getName()+"\t");
				writer.write(dataVector.getQuantitationType().getRepresentation()+"\t");
				writer.write(dataVector.getQuantitationType().getScale().getValue()+"\t");
				writer.write(dataVector.getQuantitationType().getType().getValue()+"\t");
				writer.write("\n");
			}
			writer.close();
		}
		catch( IOException e)
		{
			log.error("Error in write data into file");
		}
	}
}

package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.analysis.coexpression.GeneCoExpressionAnalysis;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationTypeEnum;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.arrayDesign.TechnologyTypeEnum;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.util.AbstractSpringAwareCLI;

public class ComputeDEVRankingCli extends AbstractSpringAwareCLI {

    public static final int MIN = 1;
    public static final int MAX = 2;
    public static final int MEDIAN = 3;
    public static final int MEAN = 4;
    private int method = MAX;
    private boolean needIntensityForRanking = false;;
    private ExpressionDataDoubleMatrix signalDataA, signalDataB, bkgDataA, bkgDataB;
    private boolean channelANeedsReconstruction = false;
    private double  signalToNoiseThreshold = 2.0;
    private static Log log = LogFactory.getLog( ComputeDEVRankingCli.class.getName() );
    
    private ExpressionExperimentService eeService = null;
	private DesignElementDataVectorService devService = null;
	
    private String geneExpressionList = null;
    private String geneExpressionFile = null;

	@Override
	protected void buildOptions() {
		// TODO Auto-generated method stub
		Option geneFileOption = OptionBuilder.hasArg().withArgName( "dataSet" ).withDescription(
		"Short name of the expression experiment to analyze (default is to analyze all found in the database)" )
		.withLongOpt( "dataSet" ).create( 'g' );
		addOption( geneFileOption );

		Option geneFileListOption = OptionBuilder.hasArg().withArgName( "list of Gene Expression file" )
		.withDescription(
		"File with list of short names of expression experiments (one per line; use instead of '-g')" )
		.withLongOpt( "listfile" ).create( 'f' );
		addOption( geneFileListOption );

	}
    protected void processOptions() {
        super.processOptions();

        if ( hasOption( 'g' ) ) {
            this.geneExpressionFile = getOptionValue( 'g' );
        }
        if ( hasOption( 'f' ) ) {
            this.geneExpressionList = getOptionValue( 'f' );
        }
    }

    private void validate( ExpressionDataDoubleMatrix signalChannelA,
            ExpressionDataDoubleMatrix signalChannelB, ExpressionDataDoubleMatrix bkgChannelA,
            ExpressionDataDoubleMatrix bkgChannelB, double signalToNoiseThreshold ) {
        // not exhaustive...
        if ( signalChannelA == null || signalChannelB == null ) {
            throw new IllegalArgumentException( "Must have data matrices" );
        }

        if ( ( bkgChannelA != null && bkgChannelA.rows() == 0 ) || ( bkgChannelB != null && bkgChannelB.rows() == 0 ) ) {
            throw new IllegalArgumentException( "Background values must not be empty when non-null" );
        }

        if ( !( signalChannelA.rows() == signalChannelB.rows() ) ) {
            throw new IllegalArgumentException( "Collection sizes must match" );
        }

        if ( ( bkgChannelA != null && bkgChannelB != null ) && bkgChannelA.rows() != bkgChannelB.rows() )
            throw new IllegalArgumentException( "Collection sizes must match" );

        if ( signalToNoiseThreshold <= 0.0 ) {
            throw new IllegalArgumentException( "Signal-to-noise threshold must be greater than zero" );
        }

        int numSamplesA = signalChannelA.columns();
        int numSamplesB = signalChannelB.columns();

        if ( numSamplesA != numSamplesB) {
            throw new IllegalArgumentException( "Number of samples doesn't match!" );
        }

    }
    private double computeIntensity( double signalToNoiseThreshold, Double sigAV, Double sigBV, Double bkgAV, Double bkgBV ) {
    	double intensity = 0.0;
        if ( ( sigAV == null && sigBV == null ) || ( sigAV.isNaN() && sigBV.isNaN() ) ) return 0.0;
        if( sigAV > bkgAV * signalToNoiseThreshold || sigBV > bkgBV * signalToNoiseThreshold){
        	intensity = (Math.log(sigAV - bkgAV) + Math.log(sigBV - bkgBV))/2;
        }
        return intensity;
    }

	private DesignElementDataVector getIntensityDataVector(DesignElementDataVector dev){
        DesignElementDataVector vect = DesignElementDataVector.Factory.newInstance();
        DesignElement designElement = dev.getDesignElement();
        
        vect.setQuantitationType( dev.getQuantitationType() );
        vect.setExpressionExperiment( dev.getExpressionExperiment() );
        vect.setDesignElement( dev.getDesignElement() );
        vect.setBioAssayDimension( dev.getBioAssayDimension() );
        
        ByteArrayConverter converter = new ByteArrayConverter();
        byte[] bytes = dev.getData();
		double[] prefRow = converter.byteArrayToDoubles( bytes );
        Double[] signalA = signalDataA != null ? signalDataA.getRow( designElement ) : null;
        Double[] signalB = signalDataB != null ? signalDataB.getRow( designElement ) : null;
        Double[] bkgA = null;
        Double[] bkgB = null;

        if ( bkgDataA != null ) bkgA = bkgDataA.getRow( designElement );
        if ( bkgDataB != null ) bkgB = bkgDataB.getRow( designElement );

        double[] intensityValues = new double[prefRow.length];
        for ( int col = 0; col < prefRow.length; col++ ) {

            // If the "preferred" value is already missing, we retain that.
            double pref = prefRow[col];
            if ( Double.isNaN(pref) ) {
                intensityValues[col] = 0.0;
                continue;
            }

            Double bkgAV = 0.0;
            Double bkgBV = 0.0;

            if ( bkgA != null ) bkgAV = bkgA[col];

            if ( bkgB != null ) bkgBV = bkgB[col];

            Double sigAV = signalA[col] == null ? 0.0 : signalA[col];

            /*
             * Put the background value back on.
             */
            if ( channelANeedsReconstruction ) {
                sigAV = sigAV + bkgAV;
            }
            Double sigBV = signalB[col] == null ? 0.0 : signalB[col];

            double intensity = computeIntensity( signalToNoiseThreshold, sigAV, sigBV, bkgAV, bkgBV );
            intensityValues[col] = intensity;
        }
        vect.setData( converter.doubleArrayToBytes( intensityValues) );
        return vect;
	}
	private boolean loadExpressionDataMatrix(ExpressionExperiment ee){
        Collection<DesignElementDataVector> allVectors = ee.getDesignElementDataVectors();
        //Collection<BioAssayDimension> dimensions = new HashSet<BioAssayDimension>();
//        for ( DesignElementDataVector vector : allVectors ) {
//            ArrayDesign adUsed = vector.getBioAssayDimension().getBioAssays().iterator().next().getArrayDesignUsed();
//            dimensions.add( vector.getBioAssayDimension() );
//        }

        QuantitationType signalChannelA = null;
        QuantitationType signalChannelB = null;
        QuantitationType backgroundChannelA = null;
        QuantitationType backgroundChannelB = null;
        QuantitationType bkgSubChannelA = null;

        for ( DesignElementDataVector vector : allVectors ) {
            QuantitationType qType = vector.getQuantitationType();
            String name = qType.getName();
            if ( name.equals( "CH1B_MEDIAN" ) || name.equals( "CH1_BKD" )
            		|| name.toLowerCase().matches( "b532[\\s_\\.](mean|median)" )
            		|| name.equals( "BACKGROUND_CHANNEL 1MEDIAN" ) || name.equals( "G_BG_MEDIAN" )
            		|| name.equals( "Ch1BkgMedian" ) || name.equals( "ch1.Background" ) || name.equals( "CH1_BKG_MEAN" )
            		|| name.equals( "CH1_BKD_ Median" ) ) {
            	backgroundChannelA = qType;
            } else if ( name.equals( "CH2B_MEDIAN" ) || name.equals( "CH2_BKD" )
            		|| name.toLowerCase().matches( "b635[\\s_\\.](mean|median)" )
            		|| name.equals( "BACKGROUND_CHANNEL 2MEDIAN" ) || name.equals( "R_BG_MEDIAN" )
            		|| name.equals( "Ch2BkgMedian" ) || name.equals( "ch2.Background" ) || name.equals( "CH2_BKG_MEAN" )
            		|| name.equals( "CH2_BKD_ Median" ) ) {
            	backgroundChannelB = qType;
            } else if ( name.matches( "CH1(I)?_MEDIAN" ) || name.matches( "CH1(I)?_MEAN" ) || name.equals( "RAW_DATA" )
            		|| name.toLowerCase().matches( "f532[\\s_\\.](mean|median)" )
            		|| name.equals( "SIGNAL_CHANNEL 1MEDIAN" ) || name.toLowerCase().matches( "ch1_smtm" )
            		|| name.equals( "G_MEAN" ) || name.equals( "Ch1SigMedian" ) || name.equals( "ch1.Intensity" )
            		|| name.equals( "CH1_SIG_MEAN" ) || name.equals( "CH1_ Median" ) 
            		|| name.toUpperCase().matches("\\w{2}\\d{3}_CY3")) {
            	signalChannelA = qType;
            } else if ( name.matches( "CH2(I)?_MEDIAN" ) || name.matches( "CH2(I)?_MEAN" )
            		|| name.equals( "RAW_CONTROL" ) || name.toLowerCase().matches( "f635[\\s_\\.](mean|median)" )
            		|| name.equals( "SIGNAL_CHANNEL 2MEDIAN" ) || name.toLowerCase().matches( "ch2_smtm" )
            		|| name.equals( "R_MEAN" ) || name.equals( "Ch2SigMedian" ) || name.equals( "ch2.Intensity" )
            		|| name.equals( "CH2_SIG_MEAN" ) || name.equals( "CH2_ Median" ) 
            		|| name.toUpperCase().matches("\\w{2}\\d{3}_CY5")) {
            	signalChannelB = qType;
            } else if ( name.matches( "CH1D_MEAN" ) ) {
            	bkgSubChannelA = qType; // specific for SGD data bug
            }
            if ( signalChannelA != null && signalChannelB != null && backgroundChannelA != null
            		&& backgroundChannelB != null) {
            	break; // no need to go through them all.
            }
        }
        this.channelANeedsReconstruction = false;
        if ( signalChannelA == null || signalChannelB == null ) {
            /*
             * Okay, this can happen for some Stanford data sets where the CH1 data was not submitted. But we can
             * sometimes reconstruct the values from the background
             */

            if ( signalChannelB != null && bkgSubChannelA != null && backgroundChannelA != null ) {
                log.info( "Invoking work-around for missing channel 1 intensities" );
                channelANeedsReconstruction = true;
            } else {
                //throw new IllegalStateException( "Could not find signals for both channels: " + "Channel A =" + signalChannelA + ", Channel B=" + signalChannelB );
            	log.info("Could not find signals for both channels: " + "Channel A =" + signalChannelA + ", Channel B=" + signalChannelB);
            	return false;
            }
        }
        if ( backgroundChannelA == null || backgroundChannelB == null ) {
            log.warn( "No background values found, proceeding with raw signals" );
        }

        if ( backgroundChannelA != null ) {
        	bkgDataA = new ExpressionDataDoubleMatrix( ee,  backgroundChannelA );
        }

        if ( backgroundChannelB != null ) {
        	bkgDataB = new ExpressionDataDoubleMatrix( ee,  backgroundChannelB );
        }

        if ( channelANeedsReconstruction ) {
        	// use background-subtracted data and add bkg back on later.
        	assert bkgDataA != null;
        	assert bkgSubChannelA != null;
        	signalDataA = new ExpressionDataDoubleMatrix( ee,  bkgSubChannelA );
        } else if ( signalChannelA != null ) {
        	signalDataA = new ExpressionDataDoubleMatrix( ee,  signalChannelA );
        }

        if ( signalChannelB != null ) {
        	signalDataB = new ExpressionDataDoubleMatrix( ee,  signalChannelB );
        }

		return true;
	}
	private double getValueForRank(DesignElementDataVector para_dev){
		DesignElementDataVector oneDev = para_dev;
		ByteArrayConverter bac = new ByteArrayConverter();
		double valueForRank = Double.NaN;
		if(oneDev == null) return 0.0;
        if(this.needIntensityForRanking){
        		oneDev = this.getIntensityDataVector(para_dev);
        		if(oneDev == null) return 0.0;
        }
        
        
		byte[] bytes = oneDev.getData();
		double[] val = bac.byteArrayToDoubles( bytes );
		DoubleArrayList valList = new DoubleArrayList( new double[val.length] );
    
		for ( int i = 0; i < val.length; i++ ) {
			if(Double.isNaN( val[i] ))
				valList.set( i, 0 );
			else
				valList.set( i, val[i] );
		}
		switch ( method ) {
		case MIN: 
			valueForRank =  Descriptive.min( valList );
			break;
		case MAX: 
			valueForRank =  Descriptive.max( valList );
			break;
		case MEAN: 
			valueForRank =  Descriptive.mean( valList );
			break;
		case MEDIAN:
			valueForRank =  Descriptive.median( valList );
			break;
		}
		if(Double.isNaN(valueForRank)) valueForRank = 0.0;
		return valueForRank;
	}

	private String computeDevRankForExpressionExperiment(ExpressionExperiment ee){
		this.bkgDataA = this.bkgDataB = this.signalDataA = this.signalDataB = null;
    	this.eeService.thaw(ee);
        ArrayDesign arrayDesign = ( ArrayDesign ) this.eeService.getArrayDesignsUsed( ee ).iterator().next();
       
		QuantitationType preferedQT = null;
		Collection<QuantitationType> allQT = this.eeService.getQuantitationTypes(ee);
		for(QuantitationType qt:allQT){
			if(qt.getIsPreferred()){
				preferedQT = qt;
				break;
			}
		}
		if(preferedQT == null) return "No Preferred QT";
		else{
			log.info("Preferred QT " + preferedQT.getId() + " for EE " + ee.getShortName());
		}

		this.needIntensityForRanking = false;
        TechnologyType currentEETechType = arrayDesign.getTechnologyType();
        if ( currentEETechType.equals( TechnologyTypeEnum.TWOCOLOR ) || currentEETechType.equals( TechnologyType.DUALMODE ) ) {
//        	if(preferedQT.getType().equals(StandardQuantitationTypeEnum.RATIO))
        	{
        		System.err.println("Current one is two color array design which needs the raw signal for ranking");
        		this.needIntensityForRanking = true;
        		if(!loadExpressionDataMatrix(ee)) return "Load EE: " + ee.getName() + "error";
                try{
                	validate( signalDataA, signalDataB, bkgDataA, bkgDataB, signalToNoiseThreshold );
                }catch(Exception e){
                	return e.getMessage();
                }
        	}
        }

		Collection<DesignElementDataVector> vectors = this.devService.findAllForMatrix( ee, preferedQT );
		this.devService.thaw(vectors);

		DoubleArrayList rankList = new DoubleArrayList( new double[vectors.size()] );
		int index = 0;
		for(DesignElementDataVector vector:vectors){
			double valueForRank = this.getValueForRank(vector);
			rankList.set(index++, valueForRank);
		}
		rankList.sort();
		for(DesignElementDataVector dev:vectors){
				double valueForRank = this.getValueForRank(dev);
				int pos = rankList.binarySearch(valueForRank);
				double rank = Double.NaN;
				if(pos >= 0 && pos < rankList.size()) rank = (double)pos/(double)rankList.size();
				dev.setRank(rank);
		}
		this.devService.update(vectors);
		log.info("Successfully computing the rank for " + ee.getShortName());
		return null;
	}

	@Override
	protected Exception doWork(String[] args) {
		// TODO Auto-generated method stub
        Exception err = processCommandLine( "DEV Ranking Calculator ", args );
        if ( err != null ) {
            return err;
        }
        this.eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        this.devService = ( DesignElementDataVectorService ) this.getBean( "designElementDataVectorService" );
        
        ExpressionExperiment expressionExperiment = null;
        if ( this.geneExpressionFile == null ) {
            Collection<String> errorObjects = new HashSet<String>();
            Collection<String> persistedObjects = new HashSet<String>();
            if ( this.geneExpressionList == null ) {
                Collection<ExpressionExperiment> all = eeService.loadAll();
                log.info( "Total ExpressionExperiment: " + all.size() );
                for ( ExpressionExperiment ee : all ) {
                    try {
                        String info = this.computeDevRankForExpressionExperiment( ee );
                        if ( info == null ) {
                            persistedObjects.add( ee.toString() );
                        } else {
                            errorObjects.add( ee.getShortName() + " contains errors: " + info );
                        }
                    } catch ( Exception e ) {
                        errorObjects.add( ee + ": " + e.getMessage() );
                        e.printStackTrace();
                        log.error( "**** Exception while processing " + ee + ": " + e.getMessage() + " ********" );
                    }
                }
            } else {
                try {
                    InputStream is = new FileInputStream( this.geneExpressionList );
                    BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
                    String shortName = null;
                    while ( ( shortName = br.readLine() ) != null ) {
                        if ( StringUtils.isBlank( shortName ) ) continue;
                        expressionExperiment = eeService.findByShortName( shortName );

                        if ( expressionExperiment == null ) {
                            errorObjects.add( shortName + " is not found in the database! " );
                            continue;
                        }
                        try {
                            String info = this.computeDevRankForExpressionExperiment( expressionExperiment );
                            if ( info == null ) {
                                persistedObjects.add( expressionExperiment.toString() );
                            } else {
                                errorObjects.add( expressionExperiment.getShortName() + " contains errors: " + info );
                            }
                        } catch ( Exception e ) {
                            errorObjects.add( expressionExperiment + ": " + e.getMessage() );
                            e.printStackTrace();
                            log.error( "**** Exception while processing " + expressionExperiment + ": "
                                    + e.getMessage() + " ********" );
                        }
                    }
                } catch ( Exception e ) {
                    return e;
                }
            }
            summarizeProcessing( errorObjects, persistedObjects );
        } else {
            expressionExperiment = eeService.findByShortName( this.geneExpressionFile );
            if ( expressionExperiment == null ) {
                log.info( this.geneExpressionFile + " is not loaded yet!" );
                return null;
            }
            String info = this.computeDevRankForExpressionExperiment( expressionExperiment );
            if ( info != null ) {
                log.info( expressionExperiment + " contains errors: " + info );
            }
        }
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
        ComputeDEVRankingCli computing = new ComputeDEVRankingCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = computing.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( "Elapsed time: " + watch.getTime() / 1000 + " seconds" );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
	}

}

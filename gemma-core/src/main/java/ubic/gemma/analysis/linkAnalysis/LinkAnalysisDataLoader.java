package ubic.gemma.analysis.linkAnalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ubic.basecode.bio.geneset.GeneAnnotations;
import ubic.basecode.dataStructure.matrix.DoubleMatrix2DNamedFactory;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.persistence.Persister;
import ubic.gemma.persistence.PersisterHelper;

/**
 * This class is to set the analysis parameters for linkAnalysis.
 * @author xiangwan
 */
public class LinkAnalysisDataLoader extends ExpressionDataLoader {
    
	private DoubleMatrixNamed dataMatrix = null;
    
    
	public LinkAnalysisDataLoader(String paraExperimentName,String goFile) {
		// TODO Auto-generated constructor stub
		super(paraExperimentName, goFile);
		this.dataMatrix = this.vectorsToDoubleMatrix(this.designElementDataVectors);
	}
	
    private DoubleMatrixNamed vectorsToDoubleMatrix( Collection<DesignElementDataVector> vectors ) {
        if ( vectors == null || vectors.size() == 0 ) {
            return null;
        }

        ByteArrayConverter bac = new ByteArrayConverter();

        List<BioAssay> bioAssays = ( List<BioAssay> ) vectors.iterator().next().getBioAssayDimension().getBioAssays();

        assert bioAssays.size() > 0 : "Empty BioAssayDimension for the vectors";

        DoubleMatrixNamed matrix = DoubleMatrix2DNamedFactory.fastrow( vectors.size(), bioAssays.size() );

        // Use BioMaterial names to represent the column in the matrix (as it can span multiple BioAssays)
        for ( BioAssay assay : bioAssays ) {
            StringBuilder buf = new StringBuilder();
            List<BioMaterial> bms = new ArrayList<BioMaterial>( assay.getSamplesUsed() );
            // Collections.sort( bms ); // FIXME this should use a sort.
            for ( BioMaterial bm : bms ) {
                buf.append( bm.getName() );
            }
            matrix.addColumnName( buf.toString() );
        }

        int rowNum = 0;
        for ( DesignElementDataVector vector : vectors ) {
            String name = vector.getDesignElement().getName();
            matrix.addRowName( name );
            byte[] bytes = vector.getData();
            double[] vals = bac.byteArrayToDoubles( bytes );
            assert vals.length == bioAssays.size() : "Number of values in vector (" + vals.length
                    + ") don't match number of Bioassays (" + bioAssays.size() + ")";
            for ( int i = 0; i < vals.length; i++ ) {
                matrix.setQuick( rowNum, i, vals[i] );
            }
            rowNum++;
        }
        return matrix;
    }
	public DoubleMatrixNamed getDataMatrix() { return this.dataMatrix;}
	public GeneAnnotations getGeneAnnotations() { return this.geneAnnotations;};
}

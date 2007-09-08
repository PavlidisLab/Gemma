package ubic.gemma.analysis.coexpression;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;

/**
 * Stores the expression profile data.
 * 
 * @author xwan
 * @author Raymond
 */
public class ExpressionProfile {
	private static ByteArrayConverter bac = new ByteArrayConverter();

	private Double[] val;
	private Double rank;
	

	private long id;

	/**
	 * Construct an ExpressionProfile from the specified DesignElementDataVector
	 * 
	 * @param dedv -
	 *            vector to convert
	 */
	public ExpressionProfile(DesignElementDataVector dedv) {
		this.id = dedv.getId();
		byte[] bytes = dedv.getData();
		double[] v = bac.byteArrayToDoubles(bytes);
        for (int i = 0; i < v.length; i++) {
            val[i] = v[i];
        }
		rank = dedv.getRank();
	}
    
    public ExpressionProfile(long id, Double rank, Double[] values) {
        this.id = id;
        this.val = values;
        this.rank = rank;
    }

	/**
	 * Get the ID of the vector
	 * 
	 * @return the vector ID
	 */
	public long getId() {
		return this.id;
	}

	public int getNumValidSamples() {
		int num = 0;
		for (Double d : val) {
			if (d == null || !Double.isNaN(d))
				num++;
		}
		return num;
	}
	
	public int getNumSamples() {
		return (val == null)? 0 : val.length;
	}

	/**
	 * Get the relative expression level of the expression profile: k/n where k
	 * is the rank of the expression level and n is the number of expression
	 * profiles for that quantitation type
	 * 
	 * @return relative expression level
	 */
	public Double getRank() {
		return rank;
	}
	
	public Double[] getExpressionLevels() {
		return val;
	}
}

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

	private double[] val;

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
		val = bac.byteArrayToDoubles(bytes);
		rank = dedv.getRank();
	}

	/**
	 * Get the ID of the vector
	 * 
	 * @return the vector ID
	 */
	public long getId() {
		return this.id;
	}

	public int getNumSamples() {
		int num = 0;
		for (double d : val) {
			if (!Double.isNaN(d))
				num++;
		}
		return num;
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

}

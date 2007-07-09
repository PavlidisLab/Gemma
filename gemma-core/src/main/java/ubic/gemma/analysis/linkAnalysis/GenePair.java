package ubic.gemma.analysis.linkAnalysis;

import cern.colt.list.LongArrayList;
import cern.colt.list.ObjectArrayList;
import cern.colt.map.AbstractLongObjectMap;
import cern.colt.map.OpenLongObjectHashMap;

public class GenePair implements Comparable<GenePair> {
	private long firstId;

	private long secondId;

	private Double effectSize;

	private AbstractLongObjectMap eeEpMap;

	private Double maxCorrelation;
	
	private Integer linkCount;

	/**
	 * Construct a gene pair with the specified pair of IDs and count
	 * 
	 * @param id1 -
	 *            ID of first gene
	 * @param id2 -
	 *            ID of second gene
	 */
	public GenePair(long id1, long id2) {
		this.firstId = id1;
		this.secondId = id2;
		this.effectSize = null;
		this.maxCorrelation = null;
		this.linkCount = null;
		eeEpMap = new OpenLongObjectHashMap();
	}

	/**
	 * Add a correlation
	 * 
	 * @param eeId -
	 *            expression experiment ID
	 * @param correlation
	 */
	public void addCorrelation(long eeId, long epId1, long epId2, double correlation) {
		eeEpMap.put(eeId, new ExpressionProfileCorrelation(epId1, epId2, correlation));
		if (maxCorrelation == null || correlation > maxCorrelation) {
			maxCorrelation = new Double(correlation);
		}
	}

	/**
	 * Get a correlation for a specified expression experiment
	 * 
	 * @param eeId -
	 *            expression experiment ID
	 * @return correlation of the expression experiment
	 */
	public Double getCorrelation(long eeId) {
		ExpressionProfileCorrelation epCorr = (ExpressionProfileCorrelation) eeEpMap.get(eeId);
		return (epCorr != null)? epCorr.correlation : null;
	}

	public Double getMaxCorrelation() {
		return maxCorrelation;
	}
	
	public Long getFirstDedvId(long eeId) {
		ExpressionProfileCorrelation epCorr = (ExpressionProfileCorrelation) eeEpMap.get(eeId);
		return (epCorr != null)? epCorr.dedvId1 : null;
	}
	
	public Long getSecondDedvId(long eeId) {
		ExpressionProfileCorrelation epCorr = (ExpressionProfileCorrelation) eeEpMap.get(eeId);
		return (epCorr != null)? epCorr.dedvId2 : null;
	}

	/**
	 * Get the of correlations
	 * 
	 * @return list of correlations (Double)
	 */
	public ObjectArrayList getCorrelations() {
		return eeEpMap.values();
	}

	/**
	 * Get the list of expression experiment IDs
	 * 
	 * @return list of expression experiment IDs
	 */
	public LongArrayList getEEIDs() {
		return eeEpMap.keys();
	}

	public int compareTo(GenePair o) {
		return -effectSize.compareTo(o.effectSize);
	}

	public Double getEffectSize() {
		return effectSize;
	}

	public void setEffectSize(Double effectSize) {
		this.effectSize = effectSize;
	}

	public long getFirstId() {
		return firstId;
	}

	public void setFirstId(long firstId) {
		this.firstId = firstId;
	}

	public long getSecondId() {
		return secondId;
	}

	public void setSecondId(long secondId) {
		this.secondId = secondId;
	}

	public Integer getLinkCount() {
		return linkCount;
	}

	public void setLinkCount(Integer linkCount) {
		this.linkCount = linkCount;
	}
	
	protected class ExpressionProfileCorrelation {
		public long dedvId1;
		public long dedvId2;
		public double correlation;
		
		public ExpressionProfileCorrelation(long dedvId1, long dedvId2, double correlation) {
			this.dedvId1 = dedvId1;
			this.dedvId2 = dedvId2;
			this.correlation = correlation;
		}
	}
}
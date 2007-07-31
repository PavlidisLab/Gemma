package ubic.gemma.analysis.coexpression;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import cern.colt.matrix.impl.DenseObjectMatrix3D;

public class CorrelationMatrix {
	private DenseObjectMatrix3D matrix;
	private Map<Long, Integer> eeId2SliceIndexMap;
	private Map<Long, Integer> qGeneId2RowIndexMap;
	private Map<Long, Integer> tGeneId2ColIndexMap;

	public CorrelationMatrix(Collection<ExpressionExperiment> EEs,
			Collection<Long> queryGeneIds, Collection<Long> targetGeneIds) {
		eeId2SliceIndexMap = new HashMap<Long, Integer>();
		int index = 0;
		for (ExpressionExperiment ee : EEs)
			eeId2SliceIndexMap.put(ee.getId(), index++);
		qGeneId2RowIndexMap = createId2IndexMap(queryGeneIds);
		tGeneId2ColIndexMap = createId2IndexMap(targetGeneIds);
		matrix = new DenseObjectMatrix3D(EEs.size(), queryGeneIds.size(),
				targetGeneIds.size());
	}

	public void setCorrelation(long eeId, long queryGeneId, long targetGeneId,
			double correlation, int sampleSize, long dedvId1, long dedvId2) {
		Element el = new Element(correlation, sampleSize, dedvId1, dedvId2);
		Integer slice = eeId2SliceIndexMap.get(eeId);
		Integer row = qGeneId2RowIndexMap.get(queryGeneId);
		Integer col = tGeneId2ColIndexMap.get(targetGeneId);
		if (slice == null || row == null || col == null)
			throw new ArrayIndexOutOfBoundsException();
		matrix.setQuick(slice, row, col, el);
	}
	
	public boolean hasCorrelation(long eeId) {
		Integer slice = eeId2SliceIndexMap.get(eeId);
		if (slice == null) return false;
		for (int i = 0; i < matrix.rows(); i++) {
			for (int j = 0; j < matrix.columns(); j++) {
				if (matrix.get(slice, i, j) != null) return true;
			}
		}
		return false;
	}

	public double[] getCorrelations(long queryGeneId, long targetGeneId) {
		double[] correlations = new double[matrix.slices()];
		for (int i = 0; i < matrix.slices(); i++) {
			Element el = (Element) matrix.get(i, qGeneId2RowIndexMap
					.get(queryGeneId), tGeneId2ColIndexMap.get(targetGeneId));
			if (el != null)
				correlations[i] = el.correlation;
		}
		return correlations;
	}
	
	public double[] getSampleSizes(long queryGeneId, long targetGeneId) {
		double[] sampleSizes = new double[matrix.slices()];
		for (int i = 0; i < matrix.slices(); i++) {
			Element el = (Element) matrix.get(i, qGeneId2RowIndexMap
					.get(queryGeneId), tGeneId2ColIndexMap.get(targetGeneId));
			if (el != null)
				sampleSizes[i] = el.sampleSize;
		}
		return sampleSizes;
	}

	public Collection<Long> getExpressionExperimentIds() {
		return eeId2SliceIndexMap.keySet();
	}

	public Collection<Long> getQueryGeneIds() {
		return qGeneId2RowIndexMap.keySet();
	}

	public Collection<Long> getTargetGeneIds() {
		return tGeneId2ColIndexMap.keySet();
	}

	public int getNumQueryGenes() {
		return matrix.rows();
	}

	public int getNumTargetGenes() {
		return matrix.columns();
	}

	public int getNumExpressionExperiments() {
		return matrix.slices();
	}

	public double getCorrelation(long eeId, long queryGeneId, long targetGeneId) {
		return getMatrixElement(eeId, queryGeneId, targetGeneId).correlation;
	}

	public long getDedvId1(long eeId, long queryGeneId, long targetGeneId) {
		return getMatrixElement(eeId, queryGeneId, targetGeneId).dedvId1;
	}

	public long getDedvId2(long eeId, long queryGeneId, long targetGeneId) {
		return getMatrixElement(eeId, queryGeneId, targetGeneId).dedvId2;
	}

	private Element getMatrixElement(long eeId, long queryGeneId,
			long targetGeneId) {
		Integer slice = eeId2SliceIndexMap.get(eeId);
		Integer row = qGeneId2RowIndexMap.get(queryGeneId);
		Integer col = tGeneId2ColIndexMap.get(targetGeneId);
		if (slice == null || row == null || col == null)
			throw new ArrayIndexOutOfBoundsException();
		return (Element) matrix.get(slice, row, col);

	}

	private Map<Long, Integer> createId2IndexMap(Collection<Long> ids) {
		Map<Long, Integer> id2IndexMap = new HashMap<Long, Integer>();
		int index = 0;
		for (Long id : ids)
			id2IndexMap.put(id, index++);
		return id2IndexMap;
	}

	protected class Element {
		double correlation;
		int sampleSize;
		long dedvId1;
		long dedvId2;

		public Element(double correlation, int sampleSize, long dedvId1, long dedvId2) {
			this.correlation = correlation;
			this.dedvId1 = dedvId1;
			this.dedvId2 = dedvId2;
			this.sampleSize = sampleSize;
		}

	}
}

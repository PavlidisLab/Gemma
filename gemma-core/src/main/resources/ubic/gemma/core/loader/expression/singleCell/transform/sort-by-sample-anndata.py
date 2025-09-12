#
# Sort an AnnData object by sample
#

import sys

try:
    import anndata
    from scipy.sparse import isspmatrix_csr
except ImportError as e:
    print('You need anndata and scipy to run this script. Install it with "pip install anndata".')
    raise e

try:
    input_file, output_file, sample_column_name = sys.argv[1:]
except Exception as e:
    print('Usage: python sort-by-sample-anndata.py input_file output_file')
    raise e

print("Reading HDF5 from " + input_file + "...")
df = anndata.read_h5ad(input_file)
print("Sorting by " + sample_column_name + "...")
# we need a copy, otherwise we would be sorting a view
df = df[:, df.var.sort_values(sample_column_name).index].copy()
if isspmatrix_csr(df.X):
    print("Sorting CSR matrix indices from /X...")
    df.X.sort_indices()
for layer in df.layers:
    if isspmatrix_csr(df.layers[layer]):
        print("Sorting CSR matrix indices from /layers/" + layer + "...")
        df.layers[layer].sort_indices()
print("Writing result to " + output_file + "...")
df.write_h5ad(output_file)

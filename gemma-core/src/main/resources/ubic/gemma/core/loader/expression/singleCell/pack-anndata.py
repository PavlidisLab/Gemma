#
# Eliminate zeroes from an AnnData object
#

import sys

try:
    import anndata
    from scipy.sparse import issparse, isspmatrix_csr
except ImportError as e:
    print('You need anndata and scipy to run this script. Install it with "pip install anndata".')
    raise e

try:
    input_file, output_file = sys.argv[1:]
except Exception as e:
    print('Usage: python pack-anndata.py input_file output_file')
    raise e

print("Reading HDF5 from " + input_file + "...")
df = anndata.read_h5ad(input_file)
if issparse(df.X) and isspmatrix_csr(df.X):
    print("Eliminating zeroes from /X...")
    df.X.eliminate_zeros()
for layer in df.layers:
    if issparse(df.layers[layer]) and isspmatrix_csr(df.X):
        print("Eliminating zeroes from /layers/" + layer + "...")
        df.layers[layer].eliminate_zeros()
print("Writing result to " + output_file + "...")
df.write_h5ad(output_file)

#
# Transpose an AnnData object
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
    print('Usage: python transpose-anndata.py input_file output_file')
    raise e

print("Reading HDF5 from " + input_file + "...")
df = anndata.read_h5ad(input_file).transpose()
# make sure that the main data and layers are efficiently accessible by row by
# either 1) stored in CSR or 2) being stored in row-major format.
if issparse(df.X) and not isspmatrix_csr(df.X):
    print("Rewriting /X to CSR...")
    df.X = df.X.tocsr()
for layer in df.layers:
    if issparse(df.layers[layer]) and not isspmatrix_csr(df.layers[layer]):
        print("Rewriting /layers/" + layer + " to CSR...")
        df.layers[layer] = df.layers[layer].tocsr()
print("Writing result to " + output_file + "...")
df.write_h5ad(output_file)

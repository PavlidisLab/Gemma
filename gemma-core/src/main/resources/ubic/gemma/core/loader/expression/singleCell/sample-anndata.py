#
# Sample an AnnData object
#

import sys

try:
    import anndata
    import numpy as np
except ImportError as e:
    print('You need anndata and scipy to run this script. Install it with "pip install anndata".')
    raise e

# ensure that our samples are reproducible
np.random.seed(123)

try:
    input_file, output_file, number_of_cells, number_of_genes = sys.argv[1:]
except Exception as e:
    print('Usage: python sample-anndata.py input_file output_file number_of_cells number_of_genes')
    raise e

number_of_cells = int(number_of_cells)
number_of_genes = int(number_of_genes)

print("Reading HDF5 from " + input_file + "...")
df = anndata.read_h5ad(input_file)
# keep indices sorted, so we don't have to sort the column indices for sparse matrices
genes_ix = np.random.permutation(df.shape[0])[:number_of_genes]
genes_ix.sort()
cells_ix = np.random.permutation(df.shape[1])[:number_of_cells]
cells_ix.sort()
df = df[genes_ix, cells_ix]
print("Writing result to " + output_file + "...")
df.write_h5ad(output_file)

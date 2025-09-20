from __future__ import annotations
import sys
sys.path.insert(0, "/space/opt/cellranger-9.0.1/lib/python")
import cellranger
import os
import numpy as np
import scipy.io
from cellranger import cr_io
from cellranger.feature_ref import FeatureReference
from cellranger.feature_ref import FeatureDef
from cellranger.mtx_to_matrix_converter import *
import pandas as pd
import scipy.io as sp_io
from six import ensure_binary, ensure_str
import cellranger.feature_ref
from cellranger.feature_ref import FeatureDef, FeatureReference
from cellranger.library_constants import GENE_EXPRESSION_LIBRARY_TYPE
from cellranger.matrix import FEATURES_TSV_GZ, CountMatrix 
import cellranger.cell_calling
import cellranger.cell_calling_helpers
from cellranger.cell_calling_helpers import call_initial_cells, call_additional_cells, FilterMethod
from cellranger.utils import get_gem_group_from_barcode
import cellranger.rna.matrix as rna_matrix

np.random.seed(42)
import pickle

GENES_TSV = "genes.tsv"


def detect_prefix(directory, filename="barcodes.tsv.gz"):
    for f in os.listdir(directory):
        if f.endswith(filename):
            prefix = f[:-len(filename)] # get everything before filename, which defaults to barcodes.tsv.gz            
            return prefix
    return None

def _find_file_with_prefix(directory, filename, prefix=None):
    if prefix:
        prefixed = os.path.join(directory, f"{prefix}{filename}")
        return prefixed
    normal = os.path.join(directory, filename)
    return normal

def load_mtx(mtx_dir, genome_name, prefix=None): 
    
    legacy_fn = _find_file_with_prefix(mtx_dir, GENES_TSV, prefix)
    v3_fn = _find_file_with_prefix(mtx_dir, FEATURES_TSV_GZ, prefix)
    if os.path.exists(legacy_fn):
        return from_legacy_mtx(mtx_dir, genome_name, prefix)

    if os.path.exists(v3_fn):
        return from_v3_mtx(mtx_dir, genome_name, prefix)

    raise OSError(f"Not a valid path to a feature-barcode mtx directory: '{mtx_dir!s}'")


def from_legacy_mtx(genome_dir, genome_name, prefix = None):
    barcodes_tsv = ensure_binary(_find_file_with_prefix(genome_dir, "barcodes.tsv", prefix))
    genes_tsv = ensure_binary(_find_file_with_prefix(genome_dir, GENES_TSV, prefix))
    matrix_mtx = ensure_binary(_find_file_with_prefix(genome_dir, "matrix.mtx", prefix))
    # ...rest of your function unchanged...
    
    for filepath in [barcodes_tsv, genes_tsv, matrix_mtx]:
        if not os.path.exists(filepath):
            raise OSError(f"Required file not found: {filepath}")
    barcodes = pd.read_csv(
        barcodes_tsv.encode(),
        delimiter="\t",
        header=None,
        usecols=[0],
        dtype=bytes,
        converters={0: ensure_binary},
    ).values.squeeze()
    genes = pd.read_csv(
        genes_tsv.encode(),
        delimiter="\t",
        header=None,
        usecols=[0],
        dtype=bytes,
        converters={0: ensure_binary},
    ).values.squeeze()
    tags = {"genome": genome_name}
    feature_defs = [
        FeatureDef(idx, gene_id, None, GENE_EXPRESSION_LIBRARY_TYPE, tags)
        for (idx, gene_id) in enumerate(genes)
    ]
    feature_ref = FeatureReference(feature_defs, [])

    matrix = sp_io.mmread(matrix_mtx)
    mat = CountMatrix(feature_ref, barcodes, matrix)
    return mat


def from_v3_mtx(genome_dir, genome_name, prefix = None):

    barcodes_tsv = ensure_str(_find_file_with_prefix(genome_dir, "barcodes.tsv.gz", prefix))
    features_tsv = ensure_str(_find_file_with_prefix(genome_dir, FEATURES_TSV_GZ, prefix))
    matrix_mtx = ensure_str(_find_file_with_prefix(genome_dir, "matrix.mtx.gz", prefix))
    
    for filepath in [barcodes_tsv, features_tsv, matrix_mtx]:
        if not os.path.exists(filepath):
            raise OSError(f"Required file not found: {filepath}")
    barcodes = pd.read_csv(
        barcodes_tsv, delimiter="\t", header=None, usecols=[0], dtype=bytes
    ).values.squeeze()
    features = pd.read_csv(features_tsv, delimiter="\t", header=None)

    feature_defs = []
    tags = {"genome": genome_name}
    for idx, (_, r) in enumerate(features.iterrows()):
        fd = FeatureDef(idx, r[0], r[1], r[2], tags)
        feature_defs.append(fd)

    feature_ref = FeatureReference(feature_defs, [])

    matrix = sp_io.mmread(matrix_mtx)
    # make csc
    matrix = matrix.tocsc()
    mat = CountMatrix(feature_ref, barcodes, matrix)
    return mat

def detect_gem_groups(mtx):
    gem_groups = set()
    for bc in mtx.bcs:
        gem_group = get_gem_group_from_barcode(bc)
        gem_groups.add(gem_group)
        
    gem_groups = sorted(gem_groups)
    return gem_groups


def save_features_tsv(feature_ref, base_dir, compress):
    """Save a FeatureReference to a tsv file."""
    out_features_fn = os.path.join(ensure_binary(base_dir), b"features.tsv")
    if compress:
        out_features_fn += b".gz"

    with cr_io.open_maybe_gzip(out_features_fn, "wb") as f:
        for feature_def in feature_ref.feature_defs:
            f.write(feature_def.id if isinstance(feature_def.id, bytes) else feature_def.id.encode())
            f.write(b"\t")
            f.write(ensure_binary(feature_def.name))
            f.write(b"\t")
            f.write(ensure_binary(feature_def.feature_type))
            f.write(b"\n")


def main():
    mtx_dir = sys.argv[1] 
    outdir = sys.argv[2] 
    genome_name = sys.argv[3]
    # make chem optional
    if len(sys.argv) > 4:
        chem = sys.argv[4]
    else:
        chem = None
    #
    if not mtx_dir and not outdir and not genome_name:
        raise ValueError("Both mtx_dir, outdir and genome_name must be provided.")

    prefix = detect_prefix(mtx_dir, "barcodes.tsv.gz")
    if prefix is None:
        print(f"Detected prefix: {prefix}")
    mtx = load_mtx(mtx_dir=mtx_dir, genome_name=genome_name, prefix=prefix)

    gem_groups = detect_gem_groups(mtx)
    print(f"Detected gem groups: {gem_groups}")
    sample = prefix if prefix else os.path.basename(mtx_dir)

    filtered_metrics_groups, filtered_bcs_groups = call_initial_cells(
        matrix=mtx,
        genomes=[genome_name],
        sample=sample,
        unique_gem_groups=gem_groups,
        method=FilterMethod.ORDMAG_NONAMBIENT, 
        recovered_cells=None,
        cell_barcodes=None,
        force_cells=None,
        feature_types=["Gene Expression"],
        chemistry_description=chem,
        target_features=None,
        has_cmo_data=False,
        num_probe_barcodes=None
    )


    filtered_bcs_groups, nonambient_summary, emptydrops_threshold = call_additional_cells(
        matrix=mtx,
        unique_gem_groups=gem_groups,
        genomes=[genome_name],
        filtered_bcs_groups=filtered_bcs_groups,
        feature_types=["Gene Expression"],
        chemistry_description=chem,
        probe_barcode_sample_id=None,
        num_probe_barcodes=None,
        emptydrops_minimum_umis=500
    )

    # filter CountMatrix to only include filtered barcodes

    for (gem_group, genome), barcodes in filtered_bcs_groups.items():
        filtered_matrix = CountMatrix.select_barcodes_by_seq(mtx, barcodes)
        print(len(filtered_matrix.bcs))
        CountMatrix.save_mex(filtered_matrix, base_dir = outdir, save_features_func=save_features_tsv)
        
        
if __name__ == "__main__":
    main()


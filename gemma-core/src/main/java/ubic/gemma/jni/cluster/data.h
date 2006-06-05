/* The C clustering library for cDNA microarray data.
 * Copyright (C) 2002 Michiel Jan Laurens de Hoon.
 *
 * This library was written at the Laboratory of DNA Information Analysis,
 * Human Genome Center, Institute of Medical Science, University of Tokyo,
 * 4-6-1 Shirokanedai, Minato-ku, Tokyo 108-8639, Japan.
 * Contact: mdehoon@c2b2.columbia.edu
 * 
 * Permission to use, copy, modify, and distribute this software and its
 * documentation with or without modifications and for any purpose and
 * without fee is hereby granted, provided that any copyright notices
 * appear in all copies and that both those copyright notices and this
 * permission notice appear in supporting documentation, and that the
 * names of the contributors or copyright holders not be used in
 * advertising or publicity pertaining to distribution of the software
 * without specific prior permission.
 * 
 * THE CONTRIBUTORS AND COPYRIGHT HOLDERS OF THIS SOFTWARE DISCLAIM ALL
 * WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL THE
 * CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY SPECIAL, INDIRECT
 * OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS
 * OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE
 * OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE
 * OR PERFORMANCE OF THIS SOFTWARE.
 * 
 */

#include <stdio.h> /* contains the FILE declaration */

/*============================================================================*/
/* Function declaration                                                       */
/*============================================================================*/

int GetRows(void);
int GetColumns(void);
char* Load(FILE* file);
/* Load in data from tab-delimited text file */
void Save(FILE* outputfile, int geneID, int arrayID);
void SelectSubset(int useRows, const int use[]);
void LogTransform(void);
void AdjustGenes(int MeanCenter, int MedianCenter, int Normalize);
void AdjustArrays(int MeanCenter, int MedianCenter, int Normalize);
int FilterRow(int Row, int bStd, int bPercent, int bAbsVal, int bMaxMin,
  double absVal, double percent, double std, int numberAbs, double maxmin);
const char* CalculateWeights(double GeneCutoff, double GeneExponent,
  char GeneDist, double ArrayCutoff, double ArrayExponent, char ArrayDist);
int HierarchicalCluster(FILE* file, char metric, int transpose, char method);
int GeneKCluster(int k, int nTrials, char method, char dist, int* NodeMap);
int ArrayKCluster(int k, int nTrials, char method, char dist, int* NodeMap);
void SaveGeneKCluster(FILE* outputfile, int k, const int* NodeMap);
void SaveArrayKCluster(FILE* outputfile, int k, const int* NodeMap);
void PerformSOM(FILE* GeneFile, int GeneXDim, int GeneYDim, int GeneIters,
  double GeneTau, char GeneMetric, FILE* ArrayFile, int ArrayXDim,
  int ArrayYDim, int ArrayIters, double ArrayTau, char ArrayMetric);
void PerformPCA(FILE* genefile, FILE* arrayfile);
void Free(void);

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

/* This file contains C code needed for Cluster 3.0, particularly file reading
 * and data handling. It is platform-independent; platform-dependent code is
 * located in windows/gui.c (Microsoft Windows), in mac/Controller.m (Mac OS X),
 * and in x11/gui.c (X11 using Motif).
 * 
 * Michiel de Hoon, (mdehoon@c2b2.columbia.edu).
 * University of Tokyo, Human Genome Center.
 * 2003.01.10.
*/

/*============================================================================*/
/* Header files                                                               */
/*============================================================================*/

/* Standard C header files */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

/* Local header files */
#include "data.h"
#include "cluster.h" /* The C clustering library */


/*============================================================================*/
/* Data declaration                                                           */
/*============================================================================*/

static int _rows = 0;
static int _columns = 0;
static double* _geneweight = NULL;
static double* _arrayweight = NULL;
static double* _geneorder = NULL;  /* Saves gene order in the data file */
static double* _arrayorder = NULL; /* Saves array order in the data file */
static int* _geneindex = NULL;   /* Set by clustering methods for file output */
static int* _arrayindex = NULL;  /* Set by clustering methods for file output */
static char* _uniqID = NULL;     /* Stores UNIQID identifier in the data file */
static char** _geneuniqID = NULL;
static char** _genename = NULL;
static char** _arrayname = NULL;
static double** _data = NULL;
static int** _mask = NULL;

/*============================================================================*/
/* Utility routines                                                           */
/*============================================================================*/

static char* GetLine(FILE* inputfile)
/* The function GetLine reads one line from the inputfile, and returns it as a
 * null-terminated string. If inputfile is at EOF, a null pointer is returned.
 * The calling routine should free the char* returned by GetLine.
 */
{ int c;
  int n = 0;
  int size = 1023;
  char* line = malloc((size+1)*sizeof(char));
  while ((c = getc(inputfile))!=EOF && c!='\r' && c!='\n')
  { if (n == size)
    { size *= 2;
      line = realloc(line,(size+1)*sizeof(char));
    }
    line[n] = c;
    n++;
  }
  if (c=='\r')
  { c = getc(inputfile);
    if (c!='\n' && c!=EOF) ungetc(c,inputfile);
  }
  if (n==0 && c==EOF)
  { free(line);
    return 0;
  }
  line[n] = '\0';
  line = realloc(line,(n+1)*sizeof(char));
  return line;
}

static char* MakeID (const char* name, int i)
{ int n;
  char* ID;
  int ndigits = 1;
  int remainder = i;
  while (remainder/=10) ndigits++; /* Count how many digits there are in i */
  n = strlen(name) + ndigits + 2;
  /* One more for the X, and one more for the \0 termination character */
  ID = malloc(n*sizeof(char));
  sprintf (ID, "%s%dX",name,i);
  return ID;
}

static void ResetIndex (char which)
{ if (which=='g')
  { int row;
    for (row=0; row < _rows; row++) _geneindex[row] = row;
    sort (_rows, _geneorder, _geneindex);
  }
  if (which=='a')
  { int column;
    for (column=0; column < _columns; column++) _arrayindex[column] = column;
    sort (_columns, _arrayorder, _arrayindex);
  }
  return;
}

static void SetClusterIndex (char which, int k, int* clusterid)
{ int i;
  int cluster;
  int counter = 0;
  int* index = NULL;
  if (which=='g')
  { index = malloc(_rows*sizeof(int));
    for (i=0; i<_rows; i++) index[i] = i;
    sort (_rows, _geneorder, index);
    for (cluster = 0; cluster < k; cluster++)
    { for (i = 0; i < _rows; i++)
      { const int j = index[i];
        if (clusterid[j]==cluster)
        { _geneindex[counter] = j;
          counter++;
        }
      }
    }
  }
  if (which=='a')
  { index = malloc(_columns*sizeof(int));
    for (i=0; i<_columns; i++) index[i] = i;
    sort (_columns, _arrayorder, index);
    for (cluster = 0; cluster < k; cluster++)
    { for (i = 0; i < _columns; i++)
      { const int j = index[i];
        if (clusterid[j]==cluster)
        { _arrayindex[counter] = j;
          counter++;
        }
      }
    }
  }
  free(index);
  return;
}

static void
TreeSort(const char which, const int nNodes, const double* order,
         const double* nodeorder, const int* nodecounts, int NodeElement[][2])
{ const int nElements = nNodes + 1;
  int i;
  double* neworder = calloc(nElements,sizeof(double)); /* initialized to 0.0 */
  int* clusterids = malloc(nElements*sizeof(int));
  for (i = 0; i < nElements; i++) clusterids[i] = i;
  for (i = 0; i < nNodes; i++)
  { const int i1 = NodeElement[i][0];
    const int i2 = NodeElement[i][1];
    const double order1 = (i1<0) ? nodeorder[-i1-1] : order[i1];
    const double order2 = (i2<0) ? nodeorder[-i2-1] : order[i2];
    const int count1 = (i1<0) ? nodecounts[-i1-1] : 1;
    const int count2 = (i2<0) ? nodecounts[-i2-1] : 1;
    /* If order1 and order2 are equal, their order is determined by 
     * the order in which they were clustered */
    if (i1<i2)
    { const double increase = (order1<order2) ? count1 : count2;
      int j;
      for (j = 0; j < nElements; j++)
      { const int clusterid = clusterids[j];
        if (clusterid==i1 && order1>=order2) neworder[j] += increase;
        if (clusterid==i2 && order1<order2) neworder[j] += increase;
        if (clusterid==i1 || clusterid==i2) clusterids[j] = -i-1;
      }
    }
    else
    { const double increase = (order1<=order2) ? count1 : count2;
      int j;
      for (j = 0; j < nElements; j++)
      { const int clusterid = clusterids[j];
        if (clusterid==i1 && order1>order2) neworder[j] += increase;
        if (clusterid==i2 && order1<=order2) neworder[j] += increase;
        if (clusterid==i1 || clusterid==i2) clusterids[j] = -i-1;
      }
    }
  }
  free(clusterids);
  if (which=='g')
  { for (i=0; i<_rows; i++) _geneindex[i] = i;
    sort(_rows, neworder, _geneindex);
  }
  if (which=='a')
  { for (i=0; i<_columns; i++) _arrayindex[i] = i;
    sort(_columns, neworder, _arrayindex);
  }
  free(neworder);
  return;
}

static void
PerformGeneSOM(FILE* file, int XDim, int YDim, int iterations, double tau,
               char metric)
{ int i,j,k;
  int* clusterid;
  int* index;
  int (*Group)[2] = malloc(_rows*sizeof(int[2]));
  double*** Nodes = malloc(XDim*YDim*_columns*sizeof(double**));
  for (i = 0; i < XDim; i++)
  { Nodes[i] = malloc(YDim*_columns*sizeof(double*));
    for (j = 0; j < YDim; j++) Nodes[i][j] = malloc(_columns*sizeof(double));
  }

  somcluster(_rows, _columns, _data, _mask, _arrayweight, 0,
    XDim, YDim, tau, iterations, metric, Nodes, Group);

  clusterid = malloc(_rows*sizeof(int));
  for (i=0; i<_rows; i++) clusterid[i] = Group[i][0] * YDim + Group[i][1];
  free(Group);

  index = malloc(_columns*sizeof(int));
  for (k=0; k<_columns; k++) index[k] = k;
  sort (_columns, _arrayorder, index);
  fputs ("NODE", file);
  for (i=0; i<_columns; i++) fprintf (file, "\t%s", _arrayname[index[i]]);
  putc ('\n', file);
  for (i=0; i<XDim; i++)
  { for (j=0; j<YDim; j++)
    { fprintf (file, "NODE(%d,%d)", i, j);
      for (k=0; k<_columns; k++) fprintf (file, "\t%f", Nodes[i][j][index[k]]);
      putc ('\n', file);
    }
  }
  free(index);

  for (i=0;i<XDim;i++)
  { for (j=0; j<YDim; j++) free(Nodes[i][j]);
    free(Nodes[i]);
  }
  free(Nodes);

  SetClusterIndex ('g', XDim * YDim, clusterid);
  free(clusterid);
}

static void
PerformArraySOM(FILE* file, int XDim, int YDim, int iterations, double tau,
                char metric)
{ int i,j,k;
  int* clusterid;
  int (*Group)[2] = malloc(_columns*sizeof(int[2]));
  double*** Nodes = malloc(XDim*YDim*_rows*sizeof(double**));
  for (i = 0; i < XDim; i++)
  { Nodes[i] = malloc(YDim*_rows*sizeof(double*));
    for (j = 0; j < YDim; j++) Nodes[i][j] = malloc(_rows*sizeof(double));
  }

  somcluster(_rows, _columns, _data, _mask, _geneweight, 1,
    XDim, YDim, tau, iterations, metric, Nodes, Group);

  clusterid = malloc(_columns*sizeof(int));
  for (i=0; i<_columns; i++)
    clusterid[i] = Group[i][0] * YDim + Group[i][1];
  free(Group);

  fprintf (file, "%s\t", _uniqID);
  for (i=0; i<XDim; i++)
    for (j=0; j<YDim; j++) fprintf(file, "\tNODE(%d,%d)", i, j);
  putc ('\n', file);

  for (k=0;k<_rows;k++)
  { int index = _geneindex[k];
    fprintf (file, "%s\t", _geneuniqID[index]);
    if (_genename[index]) fputs (_genename[index], file);
    else fputs (_geneuniqID[index], file);
    for (i=0; i<XDim; i++)
      for (j=0; j<YDim; j++) fprintf (file, "\t%f", Nodes[i][j][index]);
    putc ('\n', file);
  }

  for (i=0;i<XDim;i++)
  { for (j=0; j<YDim; j++) free(Nodes[i][j]);
    free(Nodes[i]);
  }
  free(Nodes);
  SetClusterIndex ('a', XDim * YDim, clusterid);
  free(clusterid);
}

/*============================================================================*/
/* Data handling routines                                                     */
/*============================================================================*/

void Free(void)
{ int row, column;
  for (row = 0; row < _rows; row++)
  { free (_data[row]);
    free (_mask[row]);
    free (_geneuniqID[row]);
    free (_genename[row]);
  }
  free (_data);
  free (_mask);
  free (_geneuniqID);
  free (_genename);
  for (column = 0; column < _columns; column++)
    free (_arrayname[column]);
  free (_arrayname);
  free (_geneorder);
  free (_arrayorder);
  free (_geneindex);
  free (_arrayindex);
  free (_geneweight);
  free (_arrayweight);
  free (_uniqID);
  _genename = NULL;
  _geneuniqID = NULL;
  _geneweight = NULL;
  _geneorder = NULL;
  _geneindex = NULL;
  _arrayname = NULL;
  _arrayweight = NULL;
  _arrayorder = NULL;
  _arrayindex = NULL;
  _data = NULL;
  _mask = NULL;
  _uniqID = NULL;
  _rows = 0;
  _columns = 0;
}

int GetRows(void)
{ return _rows;
}

int GetColumns(void)
{ return _columns;
}

char* Load (FILE* file)
/* Load in data from tab-delimited text file.
 * If an error occurs, an error message is returned.
 * All error messages are allocated with malloc, even if not strictly
 * necessary. The reason is that any error message then can (and should) be
 * safely freed. */
{ int row, column;           /* Counters for data matrix */
  int fileRow, fileColumn;   /* Counters for rows and columns in the file */
  int n;
  int nFileColumns;
  char* line;
  char* s;

  int geneNameColumn = -1;
  int geneWeightColumn = -1;
  int geneOrderColumn = -1;
  int arrayWeightRow = -1;
  int arrayOrderRow = -1;


  /* Deallocate previously allocated space */
  Free();

  /* Parse header line (first line) to find out what the columns are */
  fileColumn = 1;
  /* One more column than tabs */
  line=GetLine(file);
  if(!line)
  { const char text[] = "Error: Attempt to read empty file";
    const int m = strlen(text) + 1;
    char* error = malloc(m*sizeof(char));
    strcpy(error,text);
    return error;
  }
  while(line[0]=='\0') /* Ignore completely empty lines */
  { free(line);
    line = GetLine(file);
    if(!line)
    { const char text[] = "Error: Failed to find first line in file";
      const int m = strlen(text) + 1;
      char* error = malloc(m*sizeof(char));
      strcpy(error,text);
      return error;
    }
  }
  strtok(line, "\t");
  while ((s = strtok(0, "\t")))
  { if (!strcmp(s,"NAME")) geneNameColumn = fileColumn;
    else if (!strcmp(s,"GWEIGHT")) geneWeightColumn = fileColumn;
    else if (!strcmp(s,"GORDER")) geneOrderColumn = fileColumn;
    else _columns++;
    fileColumn++;
  }
  free(line);
  nFileColumns = fileColumn;
  if (nFileColumns < 2) 
  { const char text[] = "Error: less than two columns found in the file";
    const int m = strlen(text) + 1;
    char* error = malloc(m*sizeof(char));
    strcpy(error,text);
    _columns = 0;
    return error;
  }

  /* Check if the other rows in the file have the same number of columns */
  fileRow = 1;
  while ((line = GetLine(file)))
  { if (line[0]=='\0') free(line); /* Ignore completely empty lines */
    else
    /* Parse the first column to find out what the rows contain */
    { fileColumn = 1; /* One more columns than tabs */
      for (s=line; (*s)!='\0'; s++) if(*s=='\t') fileColumn++;
      s = strtok(line,"\t"); /* Returns NULL if line consists of tabs only. */
      if (s)
      { if(!strcmp(line,"EWEIGHT")) arrayWeightRow=fileRow;
        else if (!strcmp(line,"EORDER")) arrayOrderRow=fileRow;
        else if (line[0]=='\t') s = NULL; /* no gene name found */ 
        else _rows++;
      }
      fileRow++;
      free(line);
      if (s==NULL)
      { int n = 1024;
        char* text = malloc(n*sizeof(char));
        sprintf (text, "Error reading line %d: Gene name is missing", fileRow);
	n = strlen(text) + 1;
        text = realloc(text,n*sizeof(char));
        _rows = 0;
        _columns = 0;
	return text;
      }
      if (fileColumn < nFileColumns)
      { int n = 1024;
        char* text = malloc(n*sizeof(char));
        sprintf (text,
                 "Error reading line %d: only %d columns available (%d needed)",
                 fileRow, fileColumn, nFileColumns);
	n = strlen(text) + 1;
        text = realloc(text,n*sizeof(char));
        _rows = 0;
        _columns = 0;
	return text;
      }
      if (fileColumn > nFileColumns)
      { int n = 1024;
        char* text = malloc(n*sizeof(char));
        sprintf (text,
                 "Error reading line %d: %d columns given (%d needed)",
                 fileRow, fileColumn, nFileColumns);
	n = strlen(text) + 1;
        text = realloc(text,n*sizeof(char));
        _rows = 0;
        _columns = 0;
	return text;
      }
    }
  }

  /* Read the first line into a string */
  fseek (file, 0, SEEK_SET);
  line = GetLine(file);
  
  /* Save which word the user used instead of UniqID */
  s = strtok(line,"\t");
  if(!s)
  { char text[] = "Error finding UniqID keyword";
    const int m = strlen(text) + 1;
    char* error = malloc(m*sizeof(char));
    strcpy(error,text);
    _rows = 0;
    _columns = 0;
    free(line);
    return error;
  }
  n = strlen(s);
  _uniqID = malloc((n+1)*sizeof(char));
  strcpy(_uniqID, s);

  /* Allocate space for array names (experiment names) and save them */
  _arrayname = malloc(_columns*sizeof(char*));
  column = 0;
  fileColumn = 1;
  while (column < _columns)
  { s = strtok(NULL,"\t");
    n = strlen(s);
    if (fileColumn!=0 &&
        fileColumn!=geneNameColumn &&
        fileColumn!=geneWeightColumn &&
        fileColumn!=geneOrderColumn)
    { _arrayname[column] = malloc((n+1)*sizeof(char));
      strcpy(_arrayname[column],s);
      column++;
    }
    fileColumn++;
  }
  free(line);

  /* Allocate space for array weights */
  _arrayweight = malloc(_columns*sizeof(double));
  _arrayorder = malloc(_columns*sizeof(double));
  _arrayindex = malloc(_columns*sizeof(int));
  for (column = 0; column < _columns; column++)
  { _arrayweight[column] = 1.;
    _arrayorder[column] = column;
  }

  /* Allocate space for data */
  _data = malloc(_rows*sizeof(double*));
  _mask = malloc(_rows*sizeof(int*));
  for (row = 0; row < _rows; row++)
  { _data[row] = malloc(_columns*sizeof(double));
    _mask[row] = malloc(_columns*sizeof(int));
  }

  /* Allocate space for gene quantities */
  _geneweight = malloc(_rows*sizeof(double));
  _geneorder = malloc(_rows*sizeof(double));
  _geneindex = malloc(_rows*sizeof(int));
  _geneuniqID = malloc(_rows*sizeof(char*));
  _genename = malloc(_rows*sizeof(char*));

  /* Unless a NAME column exists, fill the gene names with NULL */
  if (geneNameColumn == -1)
    for (row = 0; row < _rows; row++) _genename[row] = NULL;
  /* Unless a GWEIGHT column exists, 
   * fill the gene weights with the default value */
  if (geneWeightColumn == -1)
    for (row = 0; row < _rows; row++) _geneweight[row] = 1.;
  /* Unless a GORDER column exist, set the gene order to the default value */
  if (geneOrderColumn == -1)
    for (row = 0; row < _rows; row++) _geneorder[row] = row;

  /* Read in gene data */
  row = 0;
  fseek (file, 0, SEEK_SET);
  free(GetLine(file)); /* Skip header line */
  fileRow = 1;
  while ((line=GetLine(file)))
  { if (strlen(line) > 1) /* Ignore completely empty lines */
    { if (fileRow==arrayWeightRow)
      { column = 0;
	fileColumn = 1;
        /* Skipping UNIQID column */
        s = strtok(line,"\t");
        while (column < _columns)
        { char* error = NULL;
	  s = strtok(NULL,"\t");
	  /* strtok skips empty columns, i.e. \t\t */
	  _arrayweight[column] = 0; /* Default value */
          if(s && s[0]!='\0') /* This may occur if the last value is missing */
	  { const double number = strtod(s, &error);
	    if (!(*error)) _arrayweight[column] = number;
          }
          column++;
	  fileColumn++;
        }
      }
      else if (fileRow==arrayOrderRow)
      { column = 0;
	fileColumn = 1;
        /* Skipping UNIQID column */
        s = strtok(line,"\t");
        while (column < _columns)
        { char* error = NULL;
	  s = strtok(NULL,"\t");
	  /* strtok skips empty columns, i.e. \t\t */
	  _arrayorder[column] = 0; /* Default value */
          if(s && s[0]!='\0') /* This may occur if the last value is missing */
	  { const double number = strtod(s, &error);
	    if (!(*error)) _arrayorder[column] = number;
          }
          column++;
	  fileColumn++;
        }
      }
      else
      { char* next;
        column = 0;
	fileColumn = 0;
	s = line;
	do
	{ next = strchr(s, '\t');
          if (next) *next = '\0';
          if (fileColumn==0)
          { const int n = strlen(s) + 1;
            _geneuniqID[row] = malloc(n*sizeof(char));
            strcpy (_geneuniqID[row],s);
	  }
          else if (fileColumn==geneNameColumn)
          { const int n = strlen(s) + 1;
            _genename[row] = malloc(n*sizeof(char));
            strcpy (_genename[row],s);
	  }
          else if (fileColumn==geneWeightColumn)
          { char* error = NULL;
	    double number = strtod(s, &error);
	    if (!(*error)) _geneweight[row] = number;
	    else _geneweight[row] = 0.;
	  }
          else if (fileColumn==geneOrderColumn)
          { char* error = NULL;
	    double number = strtod(s, &error);
	    if (!(*error)) _geneorder[row] = number;
	    else _geneorder[row] = 0.;
	  }
          else
          { char* error = NULL;
	    _data[row][column] = 0;
            _mask[row][column] = 0;
            if (s[0]!='\0') /* Otherwise it is a missing value */
	    { double number = strtod(s, &error);
	      if (!(*error))
	      { _data[row][column] = number;
                _mask[row][column] = 1;
	      }
            }
            column++;
          }
	  fileColumn++;
	  s = next + 1;
        } while(next);
        row++;
      }
      fileRow++;
    }
    free(line);
  }
  ResetIndex ('g');
  ResetIndex ('a');
  return 0;
}

void Save(FILE* outputfile, int geneID, int arrayID)
{ int row, column;
  if (geneID) fputs ("GID\t", outputfile);
  fputs (_uniqID, outputfile);
  fputs ("\tNAME\tGWEIGHT", outputfile);
  /* Now add headers for data columns */
  for (column = 0; column < _columns; column++)
  { putc('\t', outputfile);
    fputs(_arrayname[_arrayindex[column]], outputfile);
  }
  putc ('\n', outputfile);

  if (arrayID)
  { fputs ("AID", outputfile);
    if (geneID) putc ('\t',outputfile);
    fputs ("\t\t", outputfile);
    for (column = 0; column < _columns; column++)
    { char* ID = MakeID("ARRY",_arrayindex[column]);
      putc ('\t', outputfile);
      fputs (ID, outputfile);
      free (ID);
    }
    putc ('\n', outputfile);
  }

  fputs ("EWEIGHT", outputfile);
  if (geneID) putc ('\t', outputfile);
  fputs ("\t\t", outputfile);
  for (column = 0; column < _columns; column++)
    fprintf (outputfile, "\t%f", _arrayweight[_arrayindex[column]]);
  putc ('\n', outputfile);

  for (row = 0; row < _rows; row++)
  { int index = _geneindex[row];
    if (geneID)
    { char* ID = MakeID("GENE",index);
      fputs (ID, outputfile);
      free (ID);
      putc ('\t', outputfile);
    }

    fputs (_geneuniqID[index], outputfile);
    putc ('\t', outputfile);
    if (_genename[index]) fputs (_genename[index], outputfile);
    else fputs (_geneuniqID[index], outputfile);
    fprintf (outputfile, "\t%f", _geneweight[index]);

    for (column = 0; column < _columns; column++)
    { int columnindex = _arrayindex[column];
      putc ('\t', outputfile);
      if (_mask[index][columnindex])
        fprintf (outputfile, "%f", _data[index][columnindex]);
    }
    putc ('\n', outputfile);
  }
  return;
}

void SelectSubset(int useRows, const int use[])
{ /* Allocate temporary space */
  char** tempID = malloc(_rows*sizeof(char**));
  char** tempName = malloc(_rows*sizeof(char**));
  double* tempOrder = malloc(_rows*sizeof(double));
  double* tempWeight = malloc(_rows*sizeof(double));
  double** tempData = malloc(_rows*sizeof(double*));
  int** tempMask = malloc(_rows*sizeof(int*));
  int counter = 0;

  int row, column;
  for (row = 0; row < _rows; row++)
  { int n = strlen(_geneuniqID[row]) + 1;
    tempData[row] = malloc(_columns*sizeof(double));
    tempMask[row] = malloc(_columns*sizeof(int));
    for (column = 0; column < _columns; column++)
    { tempData[row][column] = _data[row][column];
      tempMask[row][column] = _mask[row][column];
    }
    tempID[row] = malloc(n*sizeof(char));
    strcpy (tempID[row],_geneuniqID[row]);
    if (_genename[row])
    { n = strlen(_genename[row]) + 1;
      tempName[row] = malloc(n*sizeof(char));
      strcpy (tempName[row],_genename[row]);
    }
    else tempName[row] = NULL;
    tempOrder[row] = _geneorder[row];
    tempWeight[row] = _geneweight[row];
  }
  /* Deallocate space previously used */
  for (row = 0; row < _rows; row++)
  { free(_data[row]);
    free(_mask[row]);
    free(_geneuniqID[row]);
    free(_genename[row]);
  }
  free(_data);
  free(_mask);
  free(_geneuniqID);
  free(_genename);
  free(_geneorder);
  free(_geneindex);
  free(_geneweight);
  /* Allocate space that will be used now */
  _geneuniqID = malloc(useRows*sizeof(char*));
  _genename = malloc(useRows*sizeof(char*));
  _geneorder = malloc(useRows*sizeof(double));
  _geneweight = malloc(useRows*sizeof(double));
  _geneindex = malloc(useRows*sizeof(int));
  _data = malloc(useRows*sizeof(double*));
  _mask = malloc(useRows*sizeof(int*));
  for (row = 0; row < useRows; row++)
  { _data[row] = malloc(_columns*sizeof(double));
    _mask[row] = malloc(_columns*sizeof(int));
  }
  for (row = 0; row < _rows; row++)
  { if (use[row])
    { int n = strlen(tempID[row]) + 1;
      for (column = 0; column < _columns; column++)
      { _data[counter][column] = tempData[row][column];
        _mask[counter][column] = tempMask[row][column];
      }
      _geneuniqID[counter] = malloc(n*sizeof(char));
      strcpy (_geneuniqID[counter],tempID[row]);
      if (tempName[row])
      { n = strlen(tempName[row]) + 1;
        _genename[counter] = malloc(n*sizeof(char));
        strcpy (_genename[counter],tempName[row]);
      }
      else _genename[counter] = 0;
      _geneorder[counter] = tempOrder[row];
      _geneweight[counter] = tempWeight[row];
      counter++;
    }
  }
  /* Deallocate temporary data */
  for (row = 0; row < _rows; row++)
  { free(tempData[row]);
    free(tempMask[row]);
    free(tempName[row]);
    free(tempID[row]);
  }
  free(tempData);
  free(tempMask);
  free(tempID);
  free(tempName);
  free(tempOrder);
  free(tempWeight);
  _rows = counter;
  ResetIndex('g');
  return;
}

void LogTransform(void)
{ int row, column;
  for (row = 0; row < _rows; row++)
  { /* Log transformation */
    for (column = 0; column < _columns; column++)
    { if (_mask[row][column] && _data[row][column] > 0)
        _data[row][column] = log(_data[row][column])/log(2.);
      else _mask[row][column]=0;
    }
  }
  return;
}

void AdjustGenes (int MeanCenter, int MedianCenter, int Normalize)
{ int row, column;
  for (row = 0; row < _rows; row++)
  { /* Center genes */
    if (MeanCenter || MedianCenter)
    { int counter = 0;
      double* temp = malloc(_columns*sizeof(double));
      for (column = 0; column < _columns; column++)
      { if (_mask[row][column])
        { temp[counter] = _data[row][column];
          counter++;
        }
      }
      if (counter > 0)
      { if (MeanCenter)
        { double rowmean = mean(counter, temp);
          for (column = 0; column < _columns; column++) 
            if (_mask[row][column]) _data[row][column] -= rowmean;
        }
        else if (MedianCenter)
        { double rowmedian = median(counter, temp);
          for (column = 0; column < _columns; column++)
            if (_mask[row][column]) _data[row][column] -= rowmedian;
        }
      }
      free(temp);
    }
    /* Normalize genes */
    if (Normalize)
    { double ssqu = 0;
      for (column = 0; column < _columns; column++)
      { if (_mask[row][column])
        { double term = _data[row][column];
          ssqu += term*term;
        }
      }
      if (ssqu > 0) /* Avoid dividing by zero */
      { double std = sqrt(ssqu);
        for (column = 0; column < _columns; column++)
          if (_mask[row][column]) _data[row][column] /= std;
      }
    }
  }
}

void AdjustArrays (int MeanCenter, int MedianCenter, int Normalize)
{ int row, column;
  /* Center Arrays */
  if (MeanCenter || MedianCenter)
  { double* temp = malloc(_rows*sizeof(double));
    for (column = 0; column < _columns; column++)
    { int counter = 0;
      for (row = 0; row < _rows; row++)
      { if (_mask[row][column])
        { temp[counter] = _data[row][column];
          counter++;
        }
      }
      if (counter > 0)
      { if (MeanCenter)
        { double columnmean = mean(counter,temp);
          for (row = 0; row < _rows; row++)
            if (_mask[row][column])
              _data[row][column] -= columnmean;
        }
        else if (MedianCenter)
        { double columnmedian = median(counter,temp);
          for (row = 0; row < _rows; row++)
            if (_mask[row][column])
              _data[row][column] -= columnmedian;
        }
      }
    }
    free(temp);
  }

  /* Normalize arrays */
  if (Normalize)
  { for (column = 0; column < _columns; column++)
    { double ssqu = 0;
      for (row = 0; row < _rows; row++)
        if (_mask[row][column])
        { double term = _data[row][column];
          ssqu += term * term;
        }
      if (ssqu > 0) /* Avoid dividing by zero */
      { double std = sqrt(ssqu);
        for (row = 0; row < _rows; row++)
          if (_mask[row][column]) _data[row][column] /= std;
      }
    }
  }
  return;
}

void PerformSOM(FILE* GeneFile, int GeneXDim, int GeneYDim, int GeneIters,
  double GeneTau, char GeneMetric, FILE* ArrayFile, int ArrayXDim,
  int ArrayYDim, int ArrayIters, double ArrayTau, char ArrayMetric)
{ if (GeneIters>0) PerformGeneSOM(GeneFile,
                                  GeneXDim,
                                  GeneYDim,
                                  GeneIters,
                                  GeneTau,
                                  GeneMetric);
  else ResetIndex('g');
  if (ArrayIters>0) PerformArraySOM(ArrayFile,
                                    ArrayXDim,
                                    ArrayYDim,
                                    ArrayIters,
                                    ArrayTau,
                                    ArrayMetric);
  else ResetIndex('a');
  return;
}

int FilterRow (int Row, int bStd, int bPercent, int bAbsVal, int bMaxMin,
  double absVal, double percent, double std, int numberAbs, double maxmin)
{ int Count = 0;
  int CountAbs = 0;
  double Sum = 0;
  double Sum2 = 0;
  double Min = 10000000;
  double Max = -10000000;
  /* Compute some row stats */
  int Column;
  for (Column = 0; Column < _columns; Column++)
  { if (_mask[Row][Column])
    { double value = _data[Row][Column];
      Sum += value;
      Sum2 += value*value;
      Count ++;
      Min = min(value,Min);
      Max = max(value,Max);
      if (fabs(value) >= absVal) CountAbs++;
    }
  }
  /* Filter based on percent values present;
   * remove rows with too many missing values.
   */
  if (bPercent)
  { int number = (int) ceil(percent*_columns/100);
    if (Count < number) return 0;
  }
  /* Remove rows with low SD */
  if (bStd)
  { if (Count > 1)
    { double Ave = Sum / (double) Count;
      double Var = (Sum2 - 2 * Ave * Sum + Count * Ave * Ave)/ (Count-1);
      if (sqrt(Var) < std) return 0;
    }
    else return 0;
  }
  /* Remove rows with too few extreme values */
  if (bAbsVal && CountAbs < numberAbs) return 0;
  /* Remove rows with too small Max-Min */
  if (bMaxMin && Max - Min < maxmin) return 0;
  return 1;
}

const char*
CalculateWeights(double GeneCutoff, double GeneExponent, char GeneDist,
  double ArrayCutoff, double ArrayExponent, char ArrayDist)
{ double* geneweight = NULL;
  double* arrayweight = NULL;
  if (GeneCutoff && GeneExponent && GeneDist)
  { geneweight = calculate_weights(_rows, _columns, _data, _mask,
                                   _arrayweight, 0, GeneDist,
                                   GeneCutoff, GeneExponent);
    if (!geneweight)
      return "Insufficient memory to calculate the row weights";
  }
  if (ArrayCutoff && ArrayExponent && ArrayDist)
  { arrayweight = calculate_weights(_rows, _columns, _data, _mask,
                                    _geneweight, 1, ArrayDist,
                                    ArrayCutoff, ArrayExponent);
    if (!arrayweight)
    { if (geneweight) free(geneweight);
      return "Insufficient memory to calculate the column weights";
    }
  }
  if (geneweight)
  { free(_geneweight);
    _geneweight = geneweight;
  }
  if (arrayweight)
  { free(_arrayweight);
    _arrayweight = arrayweight;
  }
  return NULL;
}


int HierarchicalCluster(FILE* file, char metric, int transpose, char method)
{ int i;
  double* nodeorder;
  int* nodecounts;
  char** nodeID;

  const int nElements = transpose ? _columns : _rows;
  const int nNodes = nElements - 1;
  const double* order = (transpose==0) ? _geneorder : _arrayorder;
  double* weight = (transpose==0) ? _arrayweight : _geneweight;
  const char* keyword = (transpose==0) ? "GENE" : "ARRY";
    
  int (*NodeElement)[2] = malloc(nNodes*sizeof(int[2]));
  double* NodeDistance = malloc(nNodes*sizeof(double));

  /* Perform hierarchical clustering. */
  int result = treecluster(_rows, _columns, _data, _mask, weight, transpose,
    metric, method, NodeElement, NodeDistance, NULL);

  if (!result)
  { free(NodeElement);
    free(NodeDistance);
    return 0;
  }

  if (metric=='e' || metric=='b')
  /* Scale all distances such that they are between 0 and 1 */
  { double scale = 0.0;
    for (i = 0; i < nNodes; i++)
      if (NodeDistance[i] > scale) scale = NodeDistance[i];
    if (scale)
      for (i = 0; i < nNodes; i++) NodeDistance[i] /= scale;
  }

  /* Now we join nodes */
  nodeorder = malloc(nNodes*sizeof(double));
  nodecounts = malloc(nNodes*sizeof(int));
  nodeID = malloc(nNodes*sizeof(char*));

  for (i = 0; i < nNodes; i++)
  { int min1 = NodeElement[i][0];
    int min2 = NodeElement[i][1];
    /* min1 and min2 are the elements that are to be joined */
    double order1;
    double order2;
    int counts1;
    int counts2;
    char* ID1;
    char* ID2;
    nodeID[i] = MakeID ("NODE",i+1);
    if (min1 < 0)
    { int index1 = -min1-1;
      order1 = nodeorder[index1];
      counts1 = nodecounts[index1];
      ID1 = nodeID[index1];
      NodeDistance[i] = max(NodeDistance[i],NodeDistance[index1]);
    }
    else
    { order1 = order[min1];
      counts1 = 1;
      ID1 = MakeID (keyword, min1);
    }
    if (min2 < 0)
    { int index2 = -min2-1;
      order2 = nodeorder[index2];
      counts2 = nodecounts[index2];
      ID2 = nodeID[index2];
      NodeDistance[i] = max(NodeDistance[i],NodeDistance[index2]);
    }
    else
    { order2 = order[min2];
      counts2 = 1;
      ID2 = MakeID (keyword, min2);
    }
 
    fprintf (file, "%s\t%s\t%s\t", nodeID[i], ID1, ID2);
    fprintf (file, "%f\n", 1.0-NodeDistance[i]);

    if (min1>=0) free(ID1);
    if (min2>=0) free(ID2);

    nodecounts[i] = counts1 + counts2;
    nodeorder[i] = (counts1*order1 + counts2*order2)
                          /(counts1 + counts2);
  }

  /* Now set up order based on the tree structure */
  TreeSort ((transpose==0) ? 'g' : 'a', nNodes, order, nodeorder, nodecounts, NodeElement);
  free(nodecounts);

  free(nodeorder);
  for (i = 0; i < nNodes; i++) free(nodeID[i]);
  free(nodeID);
  free(NodeDistance);
  free(NodeElement);

  return 1;
}

int GeneKCluster(int k, int nTrials, char method, char dist, int* NodeMap)
{ int ifound = 0;
  double error;
  kcluster (k, _rows, _columns, _data, _mask,
    _arrayweight, 0, nTrials, method, dist, NodeMap, &error, &ifound);
  SetClusterIndex('g', k, NodeMap);
  return ifound;
}

int ArrayKCluster(int k, int nTrials, char method, char dist, int* NodeMap)
{ int ifound = 0;
  double error;
  kcluster (k, _rows, _columns, _data, _mask,
    _geneweight, 1, nTrials, method, dist, NodeMap, &error, &ifound);
  SetClusterIndex ('a', k, NodeMap);
  return ifound;
}

void
SaveGeneKCluster(FILE* file, int k, const int* NodeMap)
{ int i, cluster;
  int* geneindex = malloc(_rows*sizeof(int));
  fprintf (file, "%s\tGROUP\n", _uniqID);
  for (i=0; i<_rows; i++) geneindex[i] = i;
  sort (_rows,_geneorder,geneindex);
  for (cluster = 0; cluster < k; cluster++)
  { for (i = 0; i < _rows; i++)
    { const int j = geneindex[i];
      if (NodeMap[j]==cluster)
        fprintf (file, "%s\t%d\n", _geneuniqID[j], NodeMap[j]);
    }
  }
  free(geneindex);
}

void
SaveArrayKCluster(FILE* file, int k, const int* NodeMap)
{ int i, cluster;
  int* arrayindex = malloc(_columns*sizeof(int));
  fputs ("ARRAY\tGROUP\n", file);
  for (i=0; i<_columns; i++) arrayindex[i] = i;
  sort (_columns,_arrayorder,arrayindex);
  for (cluster = 0; cluster < k; cluster++)
  { for (i = 0; i < _columns; i++)
    { const int j = arrayindex[i];
      if (NodeMap[j]==cluster)
        fprintf (file, "%s\t%d\n", _arrayname[j], NodeMap[j]);
    }
  }
  free(arrayindex);
}

static void
SaveArrayPCA (FILE* file, double eigenvalues[], double** eigenvectors,
  int order[])
{ int Row, Column;
  fputs ("EIGVALUE", file);
  for (Column=0;Column<_columns;Column++)
    fprintf(file, "\t%s", _arrayname[Column]);
  putc ('\n', file);
  if (_rows > _columns)
  { for (Row=0;Row<_columns;Row++)
    { fprintf (file, "%f", eigenvalues[order[Row]]);
      for (Column=0;Column<_columns;Column++)
        fprintf (file, "\t%f", eigenvectors[Column][order[Row]]);
      putc ('\n', file);
    }
  }
  else
  { for (Row=0;Row<_rows;Row++)
    { fprintf (file, "%f", eigenvalues[order[Row]]);
      for (Column=0;Column<_columns;Column++)
        fprintf (file, "\t%f", eigenvectors[Column][order[Row]]);
      putc ('\n', file);
    }
  }
}

static void
SaveGenePCA (FILE* file, double eigenvalues[], double** eigenvectors,
  int order[])
{ int Row, Column;
  fprintf (file, "%s\tNAME\tGWEIGHT", _uniqID);
  for (Column=0;Column<min(_rows,_columns);Column++)
    fprintf (file, "\t%f", eigenvalues[order[Column]]);
  putc ('\n', file);
  if (_rows>_columns)
  { for (Row=0;Row<_rows;Row++)
    { fprintf (file, "%s\t",_geneuniqID[Row]);
      if (_genename[Row]) fputs (_genename[Row], file);
      else fputs (_geneuniqID[Row], file);
      fprintf (file, "\t%f", _geneweight[Row]);
      for (Column=0;Column<_columns;Column++)
        fprintf (file, "\t%f", eigenvectors[Row][order[Column]]);
      putc ('\n', file);
    }
  }
  else
  { for (Row=0;Row<_rows;Row++)
    { fprintf (file, "%s\t",_geneuniqID[Row]);
      if (_genename[Row]) fputs (_genename[Row], file);
      else fputs (_geneuniqID[Row], file);
      fprintf (file, "\t%f", _geneweight[Row]);
      for (Column=0;Column<_rows;Column++)
        fprintf (file, "\t%f", eigenvectors[Column][order[Row]]);
      putc ('\n', file);
    }
  }
}

void PerformPCA(FILE* genefile, FILE* arrayfile)
{ int Row, Column;
  int* svdorder;
  double* svdsortval;
  int ierr = 0;
  const int nvals = min(_rows,_columns);
  double** u = malloc(_rows*sizeof(double*));
  double** v = malloc(_columns*sizeof(double*));
  double* w = malloc(nvals*sizeof(double));
  if (_rows > _columns)
  { for (Row=0;Row<_rows;Row++)
    { double Mag  = 0;
      u[Row] = malloc(_columns*sizeof(double));
      for (Column=0;Column<_columns;Column++)
      { if (_mask[Row][Column])
        { const double Value = _data[Row][Column];
          Mag += Value*Value;
        }
      }
      Mag = sqrt(Mag);
      if (Mag == 0) Mag = 1;
      for (Column=0;Column<_columns;Column++)
      { if (_mask[Row][Column])
          u[Row][Column] = _data[Row][Column] / Mag;
        else u[Row][Column] = 0;
      }
    }
    for (Row=0;Row<_columns;Row++)
      v[Row] = malloc(_columns*sizeof(double));
    svd(_rows,_columns,u,w,v,&ierr);
  }
  else
  { for (Column=0;Column<_columns;Column++)
      v[Column] = malloc(_rows*sizeof(double));
    for (Row=0;Row<_rows;Row++)
    { double Mag  = 0;
      for (Column=0;Column<_columns;Column++)
      { if (_mask[Row][Column])
        { const double Value = _data[Row][Column];
          Mag += Value*Value;
        }
      }
      Mag = sqrt(Mag);
      if (Mag == 0) Mag = 1;
      for (Column=0;Column<_columns;Column++)
      { if (_mask[Row][Column])
          v[Column][Row] = _data[Row][Column] / Mag;
        else v[Column][Row] = 0;
      }
    }
    for (Row=0;Row<_rows;Row++) u[Row] = malloc(_rows*sizeof(double));
    svd(_columns,_rows,v,w,u,&ierr);
  }
  svdorder = malloc(nvals*sizeof(int));
  svdsortval= malloc(nvals*sizeof(double));
  for (Column=0;Column<nvals;Column++)
  { svdorder[Column] = Column;
    svdsortval[Column] = -w[Column];
  }
  sort(nvals,svdsortval,svdorder);
  SaveArrayPCA(arrayfile, w, v, svdorder);
  SaveGenePCA(genefile, w, u, svdorder);
  for (Row=0;Row<_rows;Row++) free(u[Row]);
  for (Row=0;Row<_columns;Row++) free(v[Row]);
  free(u);
  free(v);
  free(w); 
  free(svdorder);
  free(svdsortval);
}

/*
Software and source code Copyright (C) 1998-2000 Stanford University

Written by Michael Eisen (eisen@genome.stanford.edu)

This software is copyright under the following conditions:

Permission to use, copy, and modify this software and its documentation
is hereby granted to all academic and not-for-profit institutions
without fee, provided that the above copyright notice and this permission
notice appear in all copies of the software and related documentation.
Permission to distribute the software or modified or extended versions
thereof on a not-for-profit basis is explicitly granted, under the above
conditions. However, the right to use this software in conjunction with
for profit activities, and the right to distribute the software or modified or
extended versions thereof for profit are *NOT* granted except by prior
arrangement and written consent of the copyright holders.

Use of this source code constitutes an agreement not to criticize, in any
way, the code-writing style of the author, including any statements regarding
the extent of documentation and comments present.

The software is provided "AS-IS" and without warranty of ank kind, express,
implied or otherwise, including without limitation, any warranty of
merchantability or fitness for a particular purpose.

In no event shall Stanford University or the authors be liable for any special,
incudental, indirect or consequential damages of any kind, or any damages
whatsoever resulting from loss of use, data or profits, whether or not
advised of the possibility of damage, and on any theory of liability,
arising out of or in connection with the use or performance of this software.

This code was written using Borland C++ Builder 4 (Inprise Inc.,
www.inprise.com) and may be subject to certain additional restrictions as a
result.
*/

/*
This program was modified by Michiel de Hoon of the University of Tokyo,
Human Genome Center (mdehoon@c2b2.columbia.edu). The core numerical routines
are now located in the C Clustering Library. This file implements a command-line
interface to the clustering routines in the C Clustering Library.
MdH 2003.07.17.
*/

/*============================================================================*/
/* Header files                                                               */
/*============================================================================*/

/* Standard C header files */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>

/* Local header files */
#include "data.h"      /* Includes data handling and file reading/writing */
                       /* The routines in the C Clustering Library are called */
                       /* from data.c. */

/*============================================================================*/
/* Utility routines                                                           */
/*============================================================================*/

static int load(const char filename[])
{ char* error;
  struct stat filestat;
  FILE* inputfile;
  if (stat(filename, &filestat) ||
      S_ISDIR(filestat.st_mode) ||
      !(inputfile = fopen(filename, "rt")) )
  { printf("Error opening file %s\n", filename);
    return 0;
  }
  error = Load(inputfile);
  fclose(inputfile);
  if (error)
  { printf("Error reading file %s:\n%s\n", filename, error);
    free(error);
    return 0;
  }
  return 1;
}

static void display_help(void)
{ printf ("Cluster 3.0, command-line version.\n");
  printf ("USAGE: cluster [options]\n");
  printf ("options:\n");
  printf ("  -v, --version Version information\n");
  printf ("  -f filename   File loading\n");
  printf ("  -l            Specifies to log-transform the data before clustering\n"
          "                (default is no log-transform)\n");
  printf ("  -cg a|m       Specifies whether to center each row (gene)\n"
          "                in the data\n"
          "                a: Subtract the mean of each row\n"
          "                m: Subtract the median of each row\n"
          "                (default is no centering)\n");
  printf ("  -ng           Specifies to normalize each row (gene) in the data\n"
          "                (default is no normalization)\n");
  printf ("  -ca a|m       Specifies whether to center each column (microarray)\n"
          "                in the data\n"
          "                a: Subtract the mean of each column\n"
          "                m: Subtract the median of each column\n"
          "                (default is no centering)\n");
  printf ("  -na           Specifies to normalize each column (microarray) in the data\n"
          "                (default is no normalization)\n");
  printf ("  -u jobname    Allows you to specify a different name for the output files\n"
          "                (default is derived from the input file name)\n");
  printf ("  -g [0..8]     Specifies the distance measure for gene clustering\n");
  printf ("                0: No gene clustering\n"
          "                1: Uncentered correlation\n"
          "                2: Pearson correlation\n"
          "                3: Uncentered correlation, absolute value\n"
          "                4: Pearson correlation, absolute value\n"
          "                5: Spearman's rank correlation\n"
          "                6: Kendall's tau\n"
          "                7: Euclidean distance\n"
          "                8: City-block distance\n"
          "                (default: 1)\n");
  printf ("  -e [0..8]     Specifies the distance measure for microarray clustering\n");
  printf ("                0: No clustering\n"
          "                1: Uncentered correlation\n"
          "                2: Pearson correlation\n"
          "                3: Uncentered correlation, absolute value\n"
          "                4: Pearson correlation, absolute value\n"
          "                5: Spearman's rank correlation\n"
          "                6: Kendall's tau\n"
          "                7: Euclidean distance\n"
          "                8: City-block distance\n"
          "                (default: 0)\n");
  printf ("  -m [msca]     Specifies which hierarchical clustering method to use\n"
          "                m: Pairwise complete-linkage\n"
          "                s: Pairwise single-linkage\n"
          "                c: Pairwise centroid-linkage\n"
          "                a: Pairwise average-linkage\n"
          "                (default: m)\n");
  printf ("  -k number     Specifies whether to run k-means clustering\n"
          "                instead of hierarchical clustering, and the number\n"
          "                of clusters k to use\n");
  printf ("  -r number     For k-means clustering, the number of times the\n"
          "                k-means clustering algorithm is run\n"
          "                (default: 1)\n");
  printf ("  -s            Specifies to calculate an SOM instead of hierarchical\n"
          "                clustering\n");
  printf ("  -x number     Specifies the horizontal dimension of the SOM grid\n"
          "                (default: 2)\n");
  printf ("  -y number     Specifies the vertical dimension of the SOM grid\n"
          "                (default: 1)\n");
  return;
}

static void display_version(void)
{ printf ("\n"
"Cluster 3.0, command line version, "
"using the C Clustering Library version 1.31.\n"
"\n"
"Cluster was originally written by Michael Eisen (eisen@rana.lbl.gov)\n"
"Copyright 1998-99 Stanford University.\n");
  printf ("\n"
"The command line version of Cluster version 3.0 was created by Michiel de Hoon\n"
"(mdehoon 'AT' c2b2.columbia.edu), together with Seiya Imoto and Satoru Miyano,\n"
"University of Tokyo, Institute of Medical Science, Human Genome Center.\n"
"\n"
"Visit our website at http://bonsai.ims.u-tokyo.ac.jp/~mdehoon/software/cluster\n"
"for GUI-versions of Cluster 3.0 for Windows, Mac OS X, Unix, and Linux,\n"
"as well as Python and Perl interfaces to the C Clustering Library.\n"
"\n");
  return;
}

static char* setjobname(const char* basename, int strip)
{ char* jobname;
  int n = strlen(basename);
  if (strip)
  { char* extension = strrchr(basename, '.');
    if (extension) n -= strlen(extension);
  }
  jobname = malloc((n+1)*sizeof(char));
  strncpy (jobname, basename, n);
  jobname[n] = '\0';
  return jobname;
}

static int readnumber(char word[])
{ char* error = 0;
  long value = strtol(word,&error,0);
  if (*error=='\0') return (int)value;
  else return -1;
}

static char getmetric(int i)
{ switch (i)
  { case 1: return 'u';
    case 2: return 'c';
    case 3: return 'x';
    case 4: return 'a';
    case 5: return 's';
    case 6: return 'k';
    case 7: return 'e';
    case 8: return 'b';
    default: return '\0';
  }
  /* Never get here */
  return '\0';
}

static void
Hierarchical(char genemetric, char arraymetric, char method, char* jobname)
{ FILE* outputfile;
  const int n = strlen(jobname) + strlen(".ext") + 1;
  char* const filename = malloc(n*sizeof(char));
  if (genemetric)
  { sprintf(filename, "%s.gtr", jobname);
    outputfile = fopen(filename, "wt");
    if (!outputfile) printf ("Failed opening output file %s\n", filename);
    else
    { HierarchicalCluster(outputfile, genemetric, 0, method);
      fclose(outputfile);
    }
  }
  if (arraymetric)
  { sprintf(filename, "%s.atr", jobname);
    outputfile = fopen(filename, "wt");
    if (!outputfile) printf ("Failed opening output file %s\n", filename);
    else
    { HierarchicalCluster(outputfile, arraymetric, 1, method);
      fclose(outputfile);
    }
  }
  sprintf(filename, "%s.cdt", jobname);
  outputfile = fopen(filename, "wt");
  if (!outputfile) printf ("Failed opening output file %s\n", filename);
  else
  { Save(outputfile, genemetric!='\0', arraymetric!='\0');
    fclose(outputfile);
  }
  free(filename);
  return;
}

static void KMeans(char genemetric, char arraymetric, int k, int r,
                   int Rows, int Columns, char* jobname)
{ FILE* outputfile;
  char* filename;
  int n = 1 + strlen(jobname) + strlen("_K") + strlen(".ext");
  if (genemetric && Rows < k)
  { printf("More clusters than genes available\n");
    return;
  }
  if (arraymetric && Columns < k)
  { printf("More clusters than microarrays available\n");
    return;
  }
  if (genemetric)
  { int dummy = k;
    do n++; while (dummy/=10);
    n += strlen("_G");
  }
  if (arraymetric)
  { int dummy = k;
    do n++; while (dummy/=10);
    n += strlen("_A");
  }
  filename = malloc(n*sizeof(char));
  if (genemetric)
  { const char method = 'a';
    sprintf (filename, "%s_K_G%d.kgg", jobname, k);
    outputfile = fopen(filename, "wt");
    if (!outputfile) printf ("Failed to open output file %s\n", filename);
    else
    { int* NodeMap = malloc(Rows*sizeof(int));
      GeneKCluster(k, r, method, genemetric, NodeMap);
      SaveGeneKCluster(outputfile, k, NodeMap);
      fclose(outputfile);
      free(NodeMap);
    }
  }
  if (arraymetric)
  { const char method = 'a';
    sprintf (filename, "%s_K_A%d.kag", jobname, k);
    outputfile = fopen(filename, "wt");
    if (!outputfile) printf ("Failed to open output file %s\n", filename);
    else
    { int* NodeMap = malloc(Columns*sizeof(int));
      ArrayKCluster(k, r, method, arraymetric, NodeMap);
      SaveArrayKCluster(outputfile, k, NodeMap);
      fclose(outputfile);
      free(NodeMap);
    }
  }
  if (genemetric && arraymetric)
    sprintf (filename,"%s_K_G%d_A%d.cdt", jobname, k, k);
  else if (genemetric)
    sprintf (filename,"%s_K_G%d.cdt", jobname, k);
  else if (arraymetric)
    sprintf (filename,"%s_K_A%d.cdt", jobname, k);
  /* Now write the data file */
  outputfile = fopen(filename, "wt");
  if (!outputfile) printf ("Failed to open output file %s\n", filename);
  else
  { Save(outputfile, 0, 0);
    fclose(outputfile);
  }
  free(filename);
  return;
}

static void SOM(char genemetric, char arraymetric, int x, int y,
                int Rows, int Columns, char* jobname)
{ char* filename;
  char* extension;
  FILE* GeneFile = NULL;
  FILE* ArrayFile = NULL;
  FILE* DataFile = NULL;
  int GeneIters;
  int ArrayIters;
  const double tau = 0.02;
  int n = 1 + strlen(jobname) + strlen("_SOM") + strlen(".ext");
  /* One for the terminating \0; to be increased below */
  if (genemetric)
  { int dummy = x;
    do n++; while (dummy/=10);
    dummy = y;
    do n++; while (dummy/=10);
    n+=strlen("_G");
    n++; /* For the '-' */
  }
  if (arraymetric)
  { int dummy = x;
    do n++; while (dummy/=10);
    dummy = y;
    do n++; while (dummy/=10);
    n+=strlen("_A");
    n++; /* For the '-' */
  }
  filename = malloc(n*sizeof(char));
  sprintf (filename, "%s_SOM", jobname);
  if (genemetric) sprintf (strchr(filename,'\0'),"_G%d-%d",x,y);
  if (arraymetric) sprintf (strchr(filename,'\0'),"_A%d-%d",x,y);
  extension = strchr(filename, '\0');

  sprintf(extension, ".txt");
  DataFile = fopen(filename, "wt");
  if (!DataFile)
  { printf ("Failed to open output file %s", filename);
    free(filename);
    return;
  }

  if (genemetric)
  { sprintf(extension, ".gnf");
    GeneFile = fopen(filename, "wt");
    if (!GeneFile)
    { printf ("Failed to open output file %s", filename);
      free(filename);
      return;
    }
    GeneIters = 100000;
  }
  else GeneIters = 0;

  if (arraymetric)
  { sprintf(extension, ".anf");
    ArrayFile = fopen(filename, "wt");
    if (!ArrayFile)
    { printf ("Failed to open output file %s", filename);
      free(filename);
      if(GeneFile) free(GeneFile);
      return;
    }
    ArrayIters = 20000;
  }
  else ArrayIters = 0;

  free (filename);

  PerformSOM(GeneFile, x, y, GeneIters, tau, genemetric,
             ArrayFile, x, y, ArrayIters, tau, arraymetric);
  if (GeneFile) fclose(GeneFile);
  if (ArrayFile) fclose(ArrayFile);

  Save(DataFile, 0, 0);
  fclose(DataFile);
  return;
}

/*============================================================================*/
/* Main                                                                       */
/*============================================================================*/

int main(int argc, char* argv[])
{ int i = 1;
  char* filename = 0;
  char* jobname = 0;
  int l = 0;
  int k = 0;
  int r = 1;
  int s = 0;
  int x = 2;
  int y = 1;
  int Rows, Columns;
  char genemetric = 'u';
  char arraymetric = '\0';
  char method = 'm';
  char cg = '\0';
  char ca = '\0';
  int ng = 0;
  int na = 0;
  while (i < argc)
  { const char* const argument = argv[i];
    i++;
    if (strlen(argument)<2)
    { printf("ERROR: missing argument\n");
      return 0;
    }
    if (argument[0]!='-')
    { printf("ERROR: unknown argument\n");
      return 0;
    }
    if(!strcmp(argument,"--version") || !strcmp(argument,"-v"))
    { display_version();
      return 0;
    }
    if(!strcmp(argument,"--help") || !strcmp(argument,"-h"))
    { display_help();
      return 0;
    }
    if(!strcmp(argument,"-cg"))
    { if (i==argc || strlen(argv[i])>1 || !strchr("am",argv[i][0]))
      { printf ("Error reading command line argument cg\n");
        return 0;
      }
      cg = argv[i][0];
      i++;
      continue;
    }
    if(!strcmp(argument,"-ca"))
    { if (i==argc || strlen(argv[i])>1 || !strchr("am",argv[i][0]))
      { printf ("Error reading command line argument ca\n");
        return 0;
      }
      ca = argv[i][0];
      i++;
      continue;
    }
    if(!strcmp(argument,"-ng"))
    { ng = 1;
      continue;
    }
    if(!strcmp(argument,"-na"))
    { na = 1;
      continue;
    }
    switch (argument[1])
    { case 'l': l=1; break;
      case 'u':
      { if (i==argc)
        { printf ("Error reading command line argument u: no job name specified\n");
          return 0;
        }
        jobname = setjobname(argv[i],0);
        i++;
        break;
      }
      case 'f':
      { if (i==argc)
        { printf ("Error reading command line argument f: no file name specified\n");
          return 0;
        }
        filename = argv[i];
        i++;
        break;
      }
      case 'g':
      { int g;
        if (i==argc)
        { printf ("Error reading command line argument g: parameter missing\n");
          return 0;
        }
        g = readnumber(argv[i]);
        if (g < 0 || g > 9)
        { printf ("Error reading command line argument g: should be between 0 and 9 inclusive\n");
          return 0;
        }
        i++;
        genemetric = getmetric(g);
        break;
      }
      case 'e':
      { int e;
        if (i==argc)
        { printf ("Error reading command line argument e: parameter missing\n");
          return 0;
        }
        e = readnumber(argv[i]);
        if (e < 0 || e > 9)
        { printf ("Error reading command line argument e: should be between 0 and 9 inclusive\n");
          return 0;
        }
        i++;
        arraymetric = getmetric(e);
        break;
      }
      case 'm':
      { if (i==argc || strlen(argv[i])>1 || !strchr("msca",argv[i][0]))
        { printf ("Error reading command line argument m: should be 'm', 's', 'c', or 'a'\n");
          return 0;
        }
        method = argv[i][0];
        i++;
        break;
      }
      case 's':
      { s = 1;
        break;
      }
      case 'x':
      { if (i==argc)
        { printf ("Error reading command line argument x: parameter missing\n");
          return 0;
        }
        x = readnumber(argv[i]);
        if (x < 1)
        { printf ("Error reading command line argument x: a positive integer is required\n");
          return 0;
        }
        i++;
        break;
      }
      case 'y':
      { if (i==argc)
        { printf ("Error reading command line argument y: parameter missing\n");
          return 0;
        }
        y = readnumber(argv[i]);
        if (y < 1)
        { printf ("Error reading command line argument y: a positive integer is required\n");
          return 0;
        }
        i++;
        break;
      }
      case 'k':
      { if (i==argc)
        { printf ("Error reading command line argument k: parameter missing\n");
          return 0;
        }
        k = readnumber(argv[i]);
        if (k < 1)
        { printf ("Error reading command line argument k: a positive integer is required\n");
          return 0;
        }
        i++;
        break;
      }
      case 'r':
      { if (i==argc)
        { printf ("Error reading command line argument r: parameter missing\n");
          return 0;
        }
        r = readnumber(argv[i]);
        if (r < 1)
        { printf ("Error reading command line argument r: a positive integer is required\n");
          return 0;
        }
        i++;
        break;
      }
      default: printf ("Unknown option\n");
    }
  }
  if (genemetric=='\0' && arraymetric=='\0') return 0; /* Nothing to do here */
  if (filename) load(filename);
  else
  { display_help();
    return 0;
  }
  Rows = GetRows();
  Columns = GetColumns();
  if (Rows==0 || Columns==0) printf ("No data available\n");
  else
  { if (l) LogTransform();
    switch (cg)
    {  case 'a': AdjustGenes (1, 0, ng);
       case 'm': AdjustGenes (0, 1, ng);
       default : AdjustGenes (0, 0, ng);
    }
    switch (ca)
    {  case 'a': AdjustArrays (1, 0, na);
       case 'm': AdjustArrays (0, 1, na);
       default : AdjustArrays (0, 0, na);
    }
    if(jobname==0) jobname = setjobname(filename,1);
    if(k>0) KMeans(genemetric, arraymetric, k, r, Rows, Columns, jobname);
    else if(s!=0) SOM(genemetric, arraymetric, x, y, Rows, Columns, jobname);
    else Hierarchical(genemetric, arraymetric, method, jobname);
    free(jobname);
  }
  Free();
  return 0;
}


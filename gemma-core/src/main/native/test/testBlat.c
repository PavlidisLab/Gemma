#include "../include/Blat.h"
#include "../include/gfClient.h"

int main (int argc, char **argv) {
  if (argc != 4) {
    return 255;
  }
  gfClient(argv[1], argv[2], "./", argv[3], "/tmp/outfile.tmp", "dna", "dna") ;
  return 1;

}


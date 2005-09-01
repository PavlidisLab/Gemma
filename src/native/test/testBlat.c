#include "../include/Blat.h"
#include "../include/gfClient.h"

int main (int argc, char **argv) {
  if (argc != 4) {
    return 255;
  }
  gfClientMinimal(argv[1], argv[2], "./", argv[3], "/tmp/outfile.tmp") ;
  return 1;

}


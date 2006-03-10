all: O exe test

O:
	libtool --mode=compile gcc -g -O -c -o testBlat.o testBlat.c

exe: O
	libtool --mode=link gcc -g -O -o testBlat testBlat.o  -lBlat


clean:
	rm *.o *.lo
	rm -rf ./lib

test: exe
	./testBlat localhost 177778 test.fa

blat:
	libtool --mode=compile gcc -c -O -o Blat.o Blat.c -I../include/ -I${JAVA_HOME}/include/ -I${JAVA_HOME}/include/linux


lib: blat
	libtool --mode=link gcc -g -O -o libBlat.la Blat.lo  -rpath /usr/local/lib -lGfClient

install:
	libtool --mode=install install -c libBlat.la /usr/local/lib/libBlat.la

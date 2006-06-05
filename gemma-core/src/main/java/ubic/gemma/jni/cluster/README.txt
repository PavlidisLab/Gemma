BORLAND C COMPILER 5.5 SETUP

1. Download the free Borland C/C++ 5.5 compiler from http://www.borland.com/downloads/download_cbuilder.html.
If you don't have an account, sign up for one.  You can also get it from:  
stent.cmmt.ubc.ca:/space4/pavlidis/grp/downloads/borlandCCompiler5.5.exe.
2. Run the installer (freecommandlinetools.exe).  Install in c:\Borland\Bcc55 (the default).
3. In c:\AUTOEXEC.BAT, add the following:  SET PATH=%PATH%;c:\Borland\Bcc55\Bin
4. In c:\Borland\Bcc55\Bin, create the file bcc32.cfg.  Add the lines:

-I"c:\Borland\Bcc55\Include;c:\java\jdk1.5.0_06\include;C:\java\jdk1.5.0_06\include\win32"
-L"c:\Borland\Bcc55\Lib;c:\Borland\BCC55\Lib\PSDK"

NOTE: Make sure you do not have spaces in the path to your java sdk.

5. In c:\Borland\Bcc55\Bin, create the file ilink32.cfg.  Add the line:

-L"c:\Borland\Bcc55\Lib"

6. cd to %GEMMA_HOME%\gemma-core\src\main\java\ubic\gemma\jni\cluster and run:

(if the ubic_gemma_jni_cluster_NCluster.h file has not been created, first run this)
javah -classpath %GEMMA_HOME%\build\Gemma\WEB-INF\classes ubic.gemma.jni.cluster.NCluster

bcc32 -tWM -tWD command.c
bcc32 -tWM -tWD com.c
bcc32 -tWM -tWD data.c
bcc32 -tWM -tWD ranlib.c
bcc32 -tWM -tWD linpack.c
bcc32 -tWM -tWD -l cluster.obj command.obj data.obj ranlib.obj linpack.obj com.obj cluster.c


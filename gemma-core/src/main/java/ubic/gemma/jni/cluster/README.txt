BORLAND C COMPILER 5.5 SETUP

1. Download the free Borland C/C++ 5.5 compiler.
2. Run the installer (freecommandlinetools.exe).  Install in c:\Borland\Bcc55 (the default).
3. In c:\AUTOEXEC.BAT, add the following:  SET PATH=%PATH%;c:\Borland\Bcc55
4. In c:\Borland\Bcc55\Bin, create the file bcc32.cfg.  Add the lines:

-I"c:\Borland\Bcc55\Include;c:\java\jdk1.5.0_06\include;C:\java\jdk1.5.0_06\include\win32"
-L"c:\Borland\Bcc55\Lib;c:\Borland\BCC55\Lib\PSDK"

5. In c:\Borland\Bcc55\Bin, create the file ilink32.cfg.  Add the line:

-L"c:\Borland\Bcc55\Lib"

cd to %GEMMA_HOME%\gemma-core\src\main\java\ubic\gemma\jni\cluster and run:

javah -classpath %GEMMA_HOME%\build\Gemma\WEB-INF\classes ubic.gemma.jni.cluster.NCluster
bcc32 -tWM -tWD command.c
bcc32 -tWM -tWD data.c
bcc32 -tWM -tWD ranlib.c
bcc32 -tWM -tWD linpack.c
bcc32 -tWM -tWD -l cluster.obj command.obj data.obj ranlib.obj linpack.obj com.obj cluster.c


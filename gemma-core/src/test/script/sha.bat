REM Generate a SHA encoded string representing the argument.
set CLASSPATH=%CATALINA_HOME%\server\lib\catalina.jar
set CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\common\lib\jmx.r
set CLASSPATH=%CLASSPATH%;%CATALINA_HOME%\bin\commons-logging-api.jar
java -classpath %CLASSPATH% org.apache.catalina.realm.RealmBase -a SHA %1%

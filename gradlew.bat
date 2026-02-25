@ECHO OFF
SETLOCAL

SET APP_HOME=%~dp0
SET DEFAULT_JVM_OPTS=-Xmx64m -Xms64m

SET CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

IF DEFINED JAVA_HOME (
    SET JAVACMD=%JAVA_HOME%\bin\java.exe
) ELSE (
    SET JAVACMD=java
)

IF NOT EXIST "%JAVACMD%" (
    ECHO ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
    EXIT /B 1
)

"%JAVACMD%" %DEFAULT_JVM_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
ENDLOCAL

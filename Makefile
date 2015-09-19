# Makefile for MultiRegressLines
# Andy Ganse, APL-UW

JAVABASEDIR = /Users/aganse/APLUW/src/java
CODEDIR = ${JAVABASEDIR}/edu/washington/apl/aganse/MultiRegressLines
DTOOLSDIR = ${JAVABASEDIR}/edu/washington/apl/aganse/dataTools

all: multiregresslines

multiregresslines: Makefile ${DTOOLSDIR}/DataSeries.java ${DTOOLSDIR}/DataPlotWindow.java \
		SingleRegressionLine.java DoubleRegressionLine.java TripleRegressionLine.java \
		MultiRegressLines.java
	javac -sourcepath ${JAVABASEDIR} -classpath ${CODEDIR}/classes \
		${CODEDIR}/*.java -d ${CODEDIR}/classes
	cd ${CODEDIR}/classes; jar cmf ../mainclass.mf ../MultiRegressLines.jar *

doc: MultiRegressLines.java 
	javadoc -d doc -author -version *.java

srcjar: 
	cd ${JAVABASEDIR}; \
	jar cf  ${CODEDIR}/MultiRegressLines.src.jar \
		${CODEDIR}/SingleRegressionLine.java \
		${CODEDIR}/DoubleRegressionLine.java 
		${CODEDIR}/TripleRegressionLine.java \
                ${CODEDIR}/MultiRegressLines.java
		${CODEDIR}/mainclass.mf \
		${CODEDIR}/README \
		${CODEDIR}/Makefile \
		${CODEDIR}/classes \
		${CODEDIR}/test.results
		
clean:
	\rm -rf ${CODEDIR}/doc
	\rm -rf ${CODEDIR}/classes/edu
	\rm -rf ${CODEDIR}/classes/ptolemy
	\rm -rf ${CODEDIR}/classes/*.class
	\rm -rf ${CODEDIR}/MultiRegressLines.jar


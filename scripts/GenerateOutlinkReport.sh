#  all deps available in Netarchivesuite dist for release 5.2.2 in lib folder
# so make a symbolic link to this lib folder, or make a copy of the lib-folder to this directory

NAS_VERSION="5.2.2"
DIST=distribution-$NAS_VERSION.zip
URL="https://sbforge.org/nexus/service/local/repositories/releases/content/org/netarchivesuite/distribution/$NAS_VERSION/distribution-$NAS_VERSION.zip"
if [ ! -f $DIST ]; then
 echo "Downloading necessary libs from $URL"
 wget $URL
 unzip $DIST -d $NAS_VERSION
 cp -a $NAS_VERSION/lib .	 
fi

DEPS="common-core-${NAS_VERSION}.jar dom4j-1.6.1.jar jaxen-1.1.jar commons-io-2.0.1.jar log4j-1.2.17.jar slf4j-api-1.7.7.jar slf4j-log4j12-1.7.5.jar commons-logging-1.2.jar webarchive-commons-1.1.5.jar commons-lang-2.3.jar httpclient-4.3.6.jar commons-httpclient-3.1.jar guava-17.0.jar fastutil-6.5.2.jar"
MAINLIB="extractlinks-1.0.0.jar"
CP=$MAINLIB
for jar in DEPS
do
    CP=${CP}:lib/${jar}
done

if [ ! -f $MAINLIB ]; then
 echo "Main library $MAINLIB not found! Aborting"
 exit 1
fi


java -cp $CP dk.netarkivet.extractlinks.GenerateOutlinkReport $1

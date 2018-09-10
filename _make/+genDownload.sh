##This script produces 2 zip files from this component:
##*A zip archive with the current sources
##*A zip archive with the bazaar repositiory without sources.

##the environment variable SRC should be set with the source directory name.

##include the $SRC directory in the zipfile.
cd ../..

##remove existing files.
rm -f $SRC.zip
rm -f $SRC.bzr.zip

##zip the src directory without gathering the symbolic linked directoy.
##especially this is the .bzr-archive. That should not contain in the zipped sources.
zip -r --symlinks -q $SRC.zip $SRC

##zip of the bzr archiv extra:
zip -r -q    $SRC.bzr.zip  $SRC/.bzr

##special solution: copy it to hartmuts windows-folder.
export DST="/home/hartmut/D/vishia/Download"
if test -d $DST; then
  rm -f $DST/$SRC.zip
  rm -f $DST/$SRC.bzr.zip
  cp $SRC.zip $DST/$SRC.zip
  cp $SRC.bzr.zip $DST/$SRC.bzr.zip
fi

cd $SRC/_make
echo ----finsihed------------------------------------------------------------------------------

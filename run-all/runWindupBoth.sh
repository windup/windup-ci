##  Determine this script's location ("Tools" home dir).
scriptPath="$(cd "${0%/*}" 2>/dev/null; echo "$PWD"/"${0##*/}")"
# For the case when called through a symlink
scriptPath=`readlink -f "$scriptPath"`
scriptDir=`dirname $scriptPath`

mkdir -p target/logsW1
mkdir -p target/logsW2
mkdir -p target/reportsW1
mkdir -p target/reportsW2

wget -q -nc http://switch.dl.sourceforge.net/project/jboss/JBoss%20Tattletale/1.2.0.Beta2/tattletale-1.2.0.Beta2.zip
unzip -q -f tattletale-1.2.0.Beta2.zip -d tattletale
TTALE_EXEC='java -Xmx1024m -jar tattletale/tattletale-1.2.0.Beta2/tattletale.jar' # $TTALE [<dir-with-jars> | <jar>#<jar>#<jar>] <report-dir>  (Will contain index.html.)
WINDUP1_EXEC='windup-cli-0.6.8/jboss-windup.sh'
#WINDUP2_EXEC='/home/ondra/work/Migration/windup-distribution/target/2.3.1-SNAPSHOT/bin/windup'
WINDUP2_EXEC='/home/ondra/tmp/windup-distribution-2.3.0.Final/bin/windup'


APPS_DIR="../TestApps/_apps"
APPS_LIST="$APPS_DIR/_TEST_APPS_LIST.txt"



## Copy this script's output.
#exec > >(tee target/log.txt)
exec 5> target/command.txt
BASH_XTRACEFD="5"


echo -e "\n\n<h2>Windup comparison run " `date '+%F %T'` "</h2>\n\n" >> target/Results.html
cat >> target/Results.html <<foo
<style>
  body, table, td, th { font: 10pt Verdana, sans-serif; }
  table { border-collapse: collapse; font-family: Verdana sans-serif; }
  td, th    { border: 1px solid gray; padding: 0 0.5ex; }
  td.wu1, th.wu1 { background-color: #fffdc0; }
  td.wu1, td.wu2 { align: right; }
  td.code.code0 { color: green; }
  td.code { color: red; }
</style>

<script>
  var stopReloading = false;
  setTimeout("if( ! stopReloading )  location.reload(true);", 10000);
</script>
foo

function printMetricsCells {
  echo $1
}


echo -e "<table class='results'>\n" >> target/Results.html
  echo -e "<tr>\n" >> target/Results.html
  echo -e "  <th></td>\n" >> target/Results.html
  echo -e "  <th class='wu1' colspan='4'>Windup 1.x</th>\n" >> target/Results.html
  echo -e "  <th class='wu2' colspan='4'>Windup 2.x</th>\n" >> target/Results.html
  echo -e "</tr>\n" >> target/Results.html
  
  echo -e "<tr>\n" >> target/Results.html
  echo -e "  <th class='app'>App report</th>\n" >> target/Results.html
  
  echo -e "  <th class='wu1'>Time</th>\n" >> target/Results.html
  echo -e "  <th class='wu1'>Ex. code</th>\n" >> target/Results.html
  #echo -e "  <th class='wu1'>IO in</th>\n" >> target/Results.html
  #echo -e "  <th class='wu1'>IO out</th>\n" >> target/Results.html
  echo -e "  <th class='wu1'>Mem MB</th>\n" >> target/Results.html
  echo -e "  <th class='wu1'>Log</th>\n" >> target/Results.html
  
  echo -e "  <th class='wu2'>Time</th>\n" >> target/Results.html
  echo -e "  <th class='wu2'>Ex. code</th>\n" >> target/Results.html
  #echo -e "  <th class='wu2'>IO in</th>\n" >> target/Results.html
  #echo -e "  <th class='wu2'>IO out</th>\n" >> target/Results.html
  echo -e "  <th class='wu2'>Mem MB</th>\n" >> target/Results.html
  echo -e "  <th class='wu2'>Log</th>\n" >> target/Results.html
  echo -e "</tr>\n" >> target/Results.html


while read -r LINE ; do
  #JAR=`echo "$REV" | cut -d/ -f1 | rev`
  if [ -z "$LINE" ] ; then continue; fi
  if [[ "$LINE" == \#* ]] ; then continue; fi
  if [[ "$LINE" == *--END--* ]] ; then break; fi
  
  ## path/to/app.ear | java | source | packages
  APP=`echo $LINE | cut -d'|' -f1`
  LOG=`echo $APP | tr "/ " "__"`
  APP_TYPE=`echo $LINE | cut -d'|' -f2`
  APP_MODE=`echo $LINE | cut -d'|' -f3`
  APP_PKGS=`echo $LINE | cut -d'|' -f4`
  
  if [[ "APP_MODE" == *'source'* ]] ; then SOURCE=1; else SOURCE=0; fi

  echo
  echo "=== $APP   source: $SOURCE ============================================================================="
 
  echo -e "<tr>\n" >> target/Results.html
  
  ## Run Windup 1.x
  echo
  echo "--- Windup 1 -------------------------------------------------------------------------------------------"

  mkdir -p target/reportsW1/$LOG-report
  if [ "$SOURCE" -eq "0" ]; then SRC_MODE=false; else SRC_MODE=true; fi
  truncate --size=0 target/command.txt
  set -x
  /usr/bin/time --quiet -f "time:%E IOin:%Iin IOout:%O exit:%x mem:%M" -o target/timeRes.tmp $WINDUP1_EXEC -input $APPS_DIR/$APP -output target/reportsW1/$LOG-report -source $SRC_MODE > "target/logsW1/$LOG.txt" 2>&1
  CMD=`head -n1 target/command.txt`
  set +x
  MEASURE=`cat target/timeRes.tmp`
  echo -e "$LINE\n$MEASURE\n$CMD\n" >> target/Results-Windup1x.txt
  

  TIME=`  echo "$MEASURE" | cut -d' ' -f1 | cut -d: -f2-`
  IO_IN=` echo "$MEASURE" | cut -d' ' -f2 | cut -d: -f2-`
  IO_OUT=`echo "$MEASURE" | cut -d' ' -f3 | cut -d: -f2-`
  CODE=`  echo "$MEASURE" | cut -d' ' -f4 | cut -d: -f2-`
  MEM=`   echo "$MEASURE" | cut -d' ' -f5 | cut -d: -f2-`
  MEM=$(( MEM / 1000 ))
  echo -e "  <td class='app'><a href='reportsW1/$LOG-report/index.html'>1.x</a> <a href='reportsW2/$LOG-report/index.html'>2.x</a> $LOG</td>\n" >> target/Results.html
  echo -e "  <td class='wu1'>$TIME</td>\n" >> target/Results.html
  echo -e "  <td class='wu1 code code$CODE'>$CODE</td>\n" >> target/Results.html
  #echo -e "  <td class='wu1'>$IO_IN</td>\n" >> target/Results.html
  #echo -e "  <td class='wu1'>$IO_OUT</td>\n" >> target/Results.html
  echo -e "  <td class='wu1'>$MEM</td>\n" >> target/Results.html
  echo -e "  <td class='wu1'><a href='logsW1/$LOG.txt' title=\"$CMD\">log</a></td>\n" >> target/Results.html

  ## Run Windup 2.x
  echo
  echo "--- Windup 2 -------------------------------------------------------------------------------------------"

  mkdir -p target/reportsW2/$LOG-report
  if [ "$SOURCE" -eq "0" ]; then SRC_MODE=''; else SRC_MODE='--sourceMode true'; fi
  truncate --size=0 target/command.txt
  set -x
  #/usr/bin/time --quiet -f "time:%E IOin:%Iin IOout:%O exit:%x mem:%M" -o target/timeRes.tmp forge -e "windup-migrate-app --input $APPS_DIR/$APP --output target/reportsW2/$LOG-report --packages com org net $SRC_MODE" > "target/logsW2/$LOG.txt" 2>&1
  /usr/bin/time --quiet -f "time:%E IOin:%Iin IOout:%O exit:%x mem:%M" -o target/timeRes.tmp $WINDUP2_EXEC -e "windup-migrate-app --input $APPS_DIR/$APP --output target/reportsW2/$LOG-report --packages $APP_PKGS $SRC_MODE" > "target/logsW2/$LOG.txt" 2>&1
  
  CMD=`head -n1 target/command.txt`
  set +x
  MEASURE=`cat target/timeRes.tmp`
  echo -e "$LINE\n$MEASURE\n$CMD\n" >> target/Results-Windup2x.txt
  
  TIME=`  echo "$MEASURE" | cut -d' ' -f1 | cut -d: -f2-`
  IO_IN=` echo "$MEASURE" | cut -d' ' -f2 | cut -d: -f2-`
  IO_OUT=`echo "$MEASURE" | cut -d' ' -f3 | cut -d: -f2-`
  CODE=`  echo "$MEASURE" | cut -d' ' -f4 | cut -d: -f2-`
  MEM=`   echo "$MEASURE" | cut -d' ' -f5 | cut -d: -f2-`
  MEM=$(( MEM / 1000 ))
  #echo -e "  <td class='wu2'><a href='reportsW2/$LOG-report/index.html'>$LOG</a></td>\n" >> target/Results.html
  echo -e "  <td class='wu2'>$TIME</td>\n" >> target/Results.html
  echo -e "  <td class='wu2 code code$CODE'>$CODE</td>\n" >> target/Results.html
  #echo -e "  <td class='wu2'>$IO_IN</td>\n" >> target/Results.html
  #echo -e "  <td class='wu2'>$IO_OUT</td>\n" >> target/Results.html
  echo -e "  <td class='wu2'>$MEM</td>\n" >> target/Results.html
  echo -e "  <td class='wu2'><a href='logsW2/$LOG.txt' title=\"$CMD\">log</a></td>\n" >> target/Results.html
  
  ## Run Windup 2.x
  echo
  echo "--- Tattletale ----------------------------------------------------------------------------------------"

  mkdir -p target/reportsTT/$LOG-report
  CMD="$TTALE_EXEC $APPS_DIR/$APP target/reportsTT/$LOG-report"
  echo -e "  <td class='tt'><a href='reportsTT/$LOG-report/index.html' title=\"$CMD\">ttale</a></td>\n" >> target/Results.html
  
  
  echo -e "</tr>\n" >> target/Results.html
  
done < $APPS_LIST
echo -e "</table>\n" >> target/Results.html

cat >> target/Results.html <<foo
<script>
  var stopReloading = true;
</script>
foo

rm -f target/timeRes.tmp > /dev/null


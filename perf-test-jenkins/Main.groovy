cat Main.groovy
#!/bin/bash
//usr/bin/env groovy  -cp groovy/csvcompare-0.0.1-SNAPSHOT.jar "$0" $@; exit $?

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AbstractPromptReceiver
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.oauth2.Oauth2
import com.google.gdata.client.spreadsheet.SpreadsheetService
import com.google.gdata.data.spreadsheet.*
import groovy.transform.Field

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.text.SimpleDateFormat
//import static com.xlson.groovycsv.CsvParser.parseCsv


@GrabConfig(systemClassLoader = true)
@Grab('org.postgresql:postgresql:9.4-1201-jdbc41')
@Grab('com.google.oauth-client:google-oauth-client:1.20.0')
@Grab('com.google.apis:google-api-services-oauth2:v2-rev88-1.20.0')
@Grab('com.google.gdata:core:1.47.1')
@Grab('com.google.http-client:google-http-client-jackson2:1.15.0-rc')

@Field
final String FILENAME_DATE_FORMAT = "yyyy_MM_dd_HHmm";

@Field
final String currentDateFormattedForFilename = new SimpleDateFormat(FILENAME_DATE_FORMAT).format(new Date());

@Field
final int NUMBER_OF_RUNS = 1;

// The maximum number of standard deviations from the mean
@Field
final double STANDARD_DEVIATION_ERROR_THRESHOLD = 2.0;

Class.forName("org.postgresql.Driver");

@Field
String USER_ID = "windupdocs@gmail.com";

@Field
File SCRIPT_DIR = new File(getClass().protectionDomain.codeSource.location.path).parentFile;

@Field
File WINDUP_OFFLINE = new File(SCRIPT_DIR, "windup-offline.zip");

@Field
File WINDUP_UNZIPPED_FOLDER = new File(SCRIPT_DIR, "windup");

deltree = {f ->
    if (f.directory) f.eachFile {deltree(it)}
    f.delete()
}
deltree(WINDUP_UNZIPPED_FOLDER);

WINDUP_UNZIPPED_FOLDER.mkdirs();

// unzip windup
unzipP = "unzip ${WINDUP_OFFLINE.toString()} -d ${WINDUP_UNZIPPED_FOLDER.toString()}".execute();
unzipP.consumeProcessOutput ();
unzipP.consumeProcessErrorStream(System.out);
unzipP.waitFor();

File WINDUP_BIN;
WINDUP_UNZIPPED_FOLDER.eachFileRecurse {
    if (it.toString().endsWith("/bin/windup")) {
        println(it.toString());
        WINDUP_BIN = it;
    }
};
println "Windup script: " + WINDUP_BIN;

// Run windup on jee-test
@Field
File REPORT_BASE_DIR = new File("/opt/data/testapps_output");
deltree(REPORT_BASE_DIR);
REPORT_BASE_DIR.mkdirs();

// This will contain the exported csv file and a rule provider report
@Field
File SUMMARY_OUTPUT_DIR = new File("/opt/data/test_output_summaries", currentDateFormattedForFilename);
deltree(SUMMARY_OUTPUT_DIR);
SUMMARY_OUTPUT_DIR.mkdirs();

def TEST_FILES = new File("/opt/data/testapps");

String results = "";

def JEE_TEST_NAME = "jee-example";
def JEE_TEST_FILE = new File(TEST_FILES, "other/jee-example-app-1.0.0.ear");
results += runTests(WINDUP_BIN, JEE_TEST_NAME, JEE_TEST_FILE);

// Run windup on hibernate example file
def HIBERNATE_TEST_NAME = "hibernate-tutorial-web";
def HIBERNATE_TEST_FILE = new File(TEST_FILES, "other/hibernate-tutorial-web-3.3.2.GA.war");
results += runTests(WINDUP_BIN, HIBERNATE_TEST_NAME, HIBERNATE_TEST_FILE);

results += runTests(WINDUP_BIN, "badly_named_app", new File(TEST_FILES, "other/badly_named_app"));
results += runTests(WINDUP_BIN, "drgo01.ear", new File(TEST_FILES, "other/drgo01.ear"));
results += runTests(WINDUP_BIN, "travelio.ear", new File(TEST_FILES, "other/travelio.ear"));
results += runTests(WINDUP_BIN, "ClfySmartClient.ear", new File(TEST_FILES, "att/ClfySmartClient.ear"));
results += runTests(WINDUP_BIN, "ClfyAgent.ear", new File(TEST_FILES, "att/ClfyAgent.ear"));
results += runTests(WINDUP_BIN, "PLUM_22197_ACSI_PROD_05_16_2014.ear", new File(TEST_FILES, "att/PLUM_22197_ACSI_PROD_05_16_2014.ear"));
results += runTests(WINDUP_BIN, "PassCodeReset.ear", new File(TEST_FILES, "att/PassCodeReset.ear"));
results += runTests(WINDUP_BIN, "ViewIT.war", new File(TEST_FILES, "att/ViewIT.war"));
results += runTests(WINDUP_BIN, "aps.ear", new File(TEST_FILES, "att/aps.ear"));
results += runTests(WINDUP_BIN, "phoenix-1410.ear", new File(TEST_FILES, "att/phoenix-1410.ear"));
results += runTests(WINDUP_BIN, "FKD2.ear", new File(TEST_FILES, "Lantik/FKD2.ear"));
results += runTests(WINDUP_BIN, "GW03.ear", new File(TEST_FILES, "Lantik/GW03.ear"));
results += runTests(WINDUP_BIN, "drswpc53.ear", new File(TEST_FILES, "Allianz/drswpc53.ear"));

if (results != null && results.trim().length() > 0) {
    println("================================");
    println("");
    println("ERROR:: " + results);
    println("");
    println("================================");
    System.exit(1);
}

private String runTests(File windup, String name, File inputFile) {
    println("Running: " + NUMBER_OF_RUNS + " on input file: " + inputFile);
    String result = "";
    long totalTime = 0;

    for (int i = 0; i < NUMBER_OF_RUNS; i++) {
        File reportDir = new File(REPORT_BASE_DIR, name + "-" + (i+1));
        reportDir.mkdirs();

        String command = "${windup.getAbsolutePath()} --input ${inputFile.getAbsolutePath()} --output ${reportDir.getAbsolutePath()} --overwrite --batchMode --target eap --exportCSV --offline";
        long startTime = System.currentTimeMillis();

        def env = [:];
        env.putAll(System.getenv());
        env.put("MAX_MEMORY", "8192m");

        def windupProc = command.execute(env.collect { k, v -> "$k=$v" }, new File("."));
        windupProc.consumeProcessOutput();
        windupProc.waitFor();

        long endTime = System.currentTimeMillis();
        totalTime += (endTime - startTime);
    }

    long averageTimeMillis = totalTime/NUMBER_OF_RUNS;

    def detailedStats = new LinkedHashMap();
    def ruleStats = new LinkedHashMap();
    def phaseStats = new LinkedHashMap();


    for (int i = 0; i < NUMBER_OF_RUNS; i++) {
        File reportDir = new File(REPORT_BASE_DIR, name + "-" + (i + 1));
        reportDir.mkdirs();

        // get the detailed stats
        File detailedStatsFile = new File(reportDir, "stats/detailed_stats.csv");
        detailedStatsFile.eachLine { line, number ->
            if (number == 1)
                return;
            // Number Of Executions, Total Milliseconds, Milliseconds per execution, "Type"
            // See http://fiddle.re/2mm08a for a regex test.
            // TODO: Switch to Commons-CSV http://www.groovy-tutorial.org/basic-csv/#_reading_a_csv
            def match = line =~ /([^,]*?),\s*([^,]*?),\s*([^,]*?),\s*(?:([^",]+)|(?:"((?:[^\\"]++(?:\\")?)++)"))$/; //"
            
            if (!match.matches())
                return;
                
            def numberOfExecs = Integer.valueOf(match.group(1).trim());
            def totalMillis = Integer.valueOf(match.group(2).trim());
            def detailedStatName = match.group(4);
            if (detailedStatName == null)
                detailedStatName = match.group(5).replaceAll('\"','"');

            //println("Name: " + detailedStatName + " # " + numberOfExecs + " totalMillis: " + totalMillis);
            def detailedStatRow = detailedStats[detailedStatName];
            if (!detailedStatRow) {
                detailedStatRow = [:];
                detailedStatRow["executions"] = numberOfExecs;
                detailedStatRow["total"] = totalMillis;
                detailedStats[detailedStatName] = detailedStatRow;
            } else {
                detailedStatRow["executions"] = detailedStatRow["executions"] + numberOfExecs;
                detailedStatRow["total"] = detailedStatRow["total"] + totalMillis;
            }
        }

        File phaseAndRuleStatsFile = new File(reportDir, "stats/timing.txt");
        String state = "Unknown";
        phaseAndRuleStatsFile.eachLine { line, number ->
            // get the time per rule
            if (line =~ /Rule execution timings:/) {
                state = "ruleExecution";
            } else if (line =~ /Phase execution timings:/) {
                state = "phaseExecution";
            }
            if (line =~ /^$/)
                return;

            if (state == "ruleExecution") {
                def match = line =~ /\s*(.*?), (.*)/;
                if (match.matches()) {
                    def statName = match.group(2);
                    def totalMillis = Double.valueOf(match.group(1)) * 1000;
                    def row = ruleStats[statName];
                    if (!row) {
                        row = [:];
                        row["executions"] = 1;
                        row["total"] = totalMillis;
                        ruleStats[statName] = row;
                    } else {
                        row["executions"] = row["executions"] + 1;
                        row["total"] = row["total"] + totalMillis;
                    }
                }
            }

            if (state == "phaseExecution") {
                def match = line =~ /\s*(.*?), (.*)/;
                if (match.matches()) {
                    def statName = match.group(2);
                    if (statName.indexOf(".") != -1) {
                        statName = statName.substring(statName.lastIndexOf(".")+1);
                    }
                    def totalMillis = Double.valueOf(match.group(1)) * 1000;
                    def row = phaseStats[statName];
                    if (!row) {
                        row = [:];
                        row["executions"] = 1;
                        row["total"] = totalMillis;
                        phaseStats[statName] = row;
                    } else {
                        row["executions"] = row["executions"] + 1;
                        row["total"] = row["total"] + totalMillis;
                    }
                }
            }
        };
        File appSummaryDirectory = new File(SUMMARY_OUTPUT_DIR, name);
        appSummaryDirectory.mkdirs();

        // copy a couple of summary files to the summary folder
        // report dir reportDir
        for (File f : reportDir.listFiles()) {
            if (f.getName().endsWith(".csv")) {
                File outFile = new File(appSummaryDirectory, f.getName());
                outFile << f.text;
            }
        }

        // Find the immediate predecessor to this one
        File previousRuleSummariesDirectory = new File(getPreviousRuleSummariesDirectory(), name);

        String csvComparisonResult = CSVCompare.compare(previousRuleSummariesDirectory, appSummaryDirectory);
        if (csvComparisonResult != null && csvComparisonResult.length() > 0) {
            result += "Application: " + name + " returned differences:\n" + csvComparisonResult;
            result += "\n";
        }

        // copy the rule providers report
        File ruleProviderReport = new File(reportDir, "reports/windup_ruleproviders.html");
        if (!ruleProviderReport.exists()) {
            result += "Application: " + name + " is missing the rule provider report!";
        } else {
            File newRuleProviderReport = new File(appSummaryDirectory, "windup_ruleproviders.html");
            newRuleProviderReport << ruleProviderReport.text

            def failedItems = RuleProviderReportUtil.listFailedRules(newRuleProviderReport.toString());
            failedItems.each {
                result += "On application: " + name + ", Rule: " + it.ruleID + " failed to execute!\n";
            }

            File previousRuleProviderReport = new File(previousRuleSummariesDirectory, "windup_ruleproviders.html");
            if (!previousRuleProviderReport.exists()) {
                println "No previous rule provider report is available for comparisong for application: " + name;
            } else {
                def diffs = RuleProviderReportUtil.findDifferences(previousRuleProviderReport.toString(), newRuleProviderReport.toString());
                if (!diffs.onlyInPrevious.isEmpty() || !diffs.onlyInNew.isEmpty()) {
                    println "=======================";
                    println " Differences in rule executed vs the previous run for " + name + ":";

                    if (!diffs.onlyInPrevious.isEmpty()) {
                        println "Only in Previous File: ";

                        diffs.onlyInPrevious.each {
                            println it.ruleID;
                        }
                        println "";
                    }

                    if (!diffs.onlyInNew.isEmpty()) {
                        println "Only in New File: ";

                        diffs.onlyInNew.each {
                            println it.ruleID;
                        }
                    }
                    println "=======================";
                }
            }
        }
    }

    // log these detailed stats
    result += logResults(name, averageTimeMillis, detailedStats, ruleStats, phaseStats);
    return result;
}

private File getPreviousRuleSummariesDirectory() {
        File parentDir = SUMMARY_OUTPUT_DIR.getAbsoluteFile().getParentFile();
        List<File> allSummaryDirectories = new ArrayList<>();
        allSummaryDirectories.addAll(parentDir.listFiles());
        allSummaryDirectories.sort();
        return allSummaryDirectories[-2];
}

private String logResults(String name, long averageTotalMillis, Map detailedStats, Map ruleStats, Map phaseStats) {
    String result = "";
    Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost/perftest", "perftest", "");
    long perfTestID = 0;
    try {
        String insertMainRow = "insert into performance_test(name, average_time_per_run, date_executed) values (?, ?, now()) returning performance_test_id";
        PreparedStatement pstmt = connection.prepareStatement(insertMainRow);
        try {
            pstmt.setString(1, name);
            pstmt.setLong(2, (averageTotalMillis / 1000).longValue());
            ResultSet rs = pstmt.executeQuery()
            try {
                rs.next();
                perfTestID = rs.getLong(1);
            } finally {
                rs.close();
            }
        } finally {
            pstmt.close();
        }

        // Get previous average and standard deviation
        String getAvgAndStdevSql = "select avg(average_time_per_run),stddev_pop(average_time_per_run) from performance_test where name = ?";
        PreparedStatement getAvgStatement = connection.prepareStatement(getAvgAndStdevSql);
        try {
            getAvgStatement.setString(1, name);
            ResultSet rs = getAvgStatement.executeQuery();
            rs.next();
            double previousAverageSeconds = rs.getDouble(1);
            double previousStandardDeviation = rs.getDouble(2);
            
            double diffFromAverage = Math.abs((averageTotalMillis/1000) - previousAverageSeconds);
            double maxDiff = (double)STANDARD_DEVIATION_ERROR_THRESHOLD * previousStandardDeviation;
            if (diffFromAverage > maxDiff) {
                result = "Performance out of spec for: " + name + ", " + diffFromAverage + 
                         " from average, but the the threshold (" + STANDARD_DEVIATION_ERROR_THRESHOLD + "*" + previousStandardDeviation + ") is: " + 
                         maxDiff + " (Previous Average: " + previousAverageSeconds + ", StdDev: " + previousStandardDeviation + 
                         ", Current Runtime: " + (averageTotalMillis/1000) + ")\n";
            }

        } finally {
            getAvgStatement.close();
        }


        // insert detailed stats
        for (def row : detailedStats) {
            String sql = "insert into detailed_stats (performance_test_id, name, number_of_executions, total_time_taken, average_time_taken) values (?, ?, ?, ?, ?)";
            PreparedStatement insert = connection.prepareStatement(sql);
            insert.setLong(1, perfTestID);
            insert.setString(2, row.key);
            insert.setInt(3, row.value["executions"]);
            insert.setInt(4, row.value["total"]);
            double timePerExec = row.value["total"] == 0 ? 0 : (row.value["total"]/row.value["executions"]).doubleValue();
            insert.setDouble(5, timePerExec);
            insert.executeUpdate();
        }

        // insert rule stats
        for (def row : ruleStats) {
            String sql = "insert into rule_stats (performance_test_id, name, average_time_taken) values (?, ?, ?)";
            PreparedStatement insert = connection.prepareStatement(sql);
            insert.setLong(1, perfTestID);
            insert.setString(2, row.key);
            double timePerExec = row.value["total"] == 0 ? 0 : (row.value["total"]/row.value["executions"]).doubleValue();
            insert.setDouble(3, timePerExec);
            insert.executeUpdate();
        }

        // insert phase stats
        for (def row : phaseStats) {
            String sql = "insert into phase_stats (performance_test_id, name, average_time_taken) values (?, ?, ?)";
            PreparedStatement insert = connection.prepareStatement(sql);
            insert.setLong(1, perfTestID);
            insert.setString(2, row.key);
            double timePerExec = row.value["total"] == 0 ? 0 : (row.value["total"]/row.value["executions"]).doubleValue();
            insert.setDouble(3, timePerExec);
            insert.executeUpdate();
        }
    } finally {
        connection.close();
    }

    uploadToGoogle(name, averageTotalMillis, detailedStats, ruleStats, phaseStats);
    return result;
}

@Field
def CREDENTIALS_STORE_DIR = new File(SCRIPT_DIR, ".credentials");
@Field
def CLIENT_SECRETS_FILE = new File(CREDENTIALS_STORE_DIR, "client_secrets.json");
@Field
URL SPREADSHEET_FEED_URL = new URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full");

private void uploadToGoogle(String name, long averageTotalMillis, Map detailedStats, Map ruleStats, Map phaseStats) {
    // 5. Authenticate to Google
    /** Global instance of the HTTP transport. */
    final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /** Global instance of the JSON factory. */
    final JsonFactory JSON_FACTORY = new JacksonFactory();

    Credential credential = authorize(HTTP_TRANSPORT, JSON_FACTORY);
    Oauth2 oauth2 = new Oauth2.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
            .setApplicationName("perftest")
            .build();

    SpreadsheetService service = new SpreadsheetService("MySpreadsheetIntegration-v1", );
    service.setOAuth2Credentials(credential);

    // Make a request to the API and get all spreadsheets.
    SpreadsheetEntry spreadsheet = getSpreadSheet(service, name);
    if (spreadsheet == null) {
        throw new RuntimeException("Failed to find spreadsheet for " + name + " (name should be \"perftest - " + name + "\")");
    }

    WorksheetFeed worksheetFeed = service.getFeed(spreadsheet.getWorksheetFeedUrl(), WorksheetFeed.class);
    WorksheetEntry totalWorksheet;
    WorksheetEntry byPhaseWorksheet;
    WorksheetEntry byRuleProviderWorksheet;
    for (WorksheetEntry entry : worksheetFeed.entries) {
        switch(entry.title.plainText) {
            case "Total":
                totalWorksheet = entry;
                break;
            case "By Phase":
                byPhaseWorksheet = entry;
                break;
            case "By RuleProvider":
                byRuleProviderWorksheet = entry;
                break;
        }
    }

    if (totalWorksheet == null)
        throw new RuntimeException("Could not find Total worksheet");
    if (byPhaseWorksheet == null)
        throw new RuntimeException("Could not find 'By Phase' worksheet");
    if (byRuleProviderWorksheet == null)
        throw new RuntimeException("Could not find 'By RuleProvider' worksheet");

    String dateFormatted = new Date().format("MM-dd-yyyy HH:mm:ss");

    // Fetch the cell feed of the worksheet.
    def mapRow = [:];
    mapRow['Date'] = dateFormatted;
    mapRow['Name'] = name;
    mapRow['Duration (in Seconds)'] = String.valueOf((averageTotalMillis/1000).intValue());
    updateWorksheet(service, totalWorksheet, mapRow);

    def byRuleRow = new LinkedHashMap();
    byRuleRow['Date'] = dateFormatted;
    for (def e : ruleStats) {
        double timePerExec = e.value["total"] == 0 ? 0 : (e.value["total"]/e.value["executions"]).doubleValue();
        byRuleRow[e.key] = String.valueOf(timePerExec);
    }
    updateWorksheet(service, byRuleProviderWorksheet, byRuleRow);

    def byPhaseRow = new LinkedHashMap();
    byPhaseRow['Date'] = dateFormatted;
    for (def e : phaseStats) {
        double timePerExec = e.value["total"] == 0 ? 0 : (e.value["total"]/e.value["executions"]).doubleValue();
        byPhaseRow[e.key] = String.valueOf(timePerExec);
    }
    updateWorksheet(service, byPhaseWorksheet, byPhaseRow);
}

private void updateWorksheet(SpreadsheetService service, WorksheetEntry worksheet, Map values) {
    URL cellFeedUrl = worksheet.getCellFeedUrl();
    CellFeed cellFeed = service.getFeed(cellFeedUrl, CellFeed.class);

    def columnNameMap = [:];

    int maxColumn = 0;
    int maxRow = 0;

    // Iterate through each cell, printing its value.
    for (CellEntry cell : cellFeed.getEntries()) {
        String id = cell.getId().substring(cell.getId().lastIndexOf('/') + 1);
        def colRowMatcher = id =~ /R([0-9]+)C([0-9]+)/;
        if (colRowMatcher.matches()) {
            def row = Integer.parseInt(colRowMatcher.group(1));
            def col = Integer.parseInt(colRowMatcher.group(2));
            if (row > maxRow)
                maxRow = row;
            if (col > maxColumn)
                maxColumn = col;

            if (row == 1) {
                def value = cell.cell.value;
                columnNameMap[value] = col;
            }
        }
    }

    maxRow++;
    if ((worksheet.rowCount - maxRow) < 100) {
        worksheet.setRowCount(maxRow + 1000);
        worksheet = worksheet.update();
    }

    for (def col : values) {
        if (!columnNameMap.containsKey(col.key)) {
            // make sure we have enough columns
            if ((worksheet.colCount - maxColumn) < 10) {
                worksheet.setColCount(maxColumn + 20);
                worksheet = worksheet.update();
            }

            // insert header
            maxColumn++;
            retry (5, {
                CellEntry newHeaderCell = new CellEntry(1, maxColumn, col.key);
                service.insert(cellFeedUrl, newHeaderCell);
            });
            columnNameMap[col.key] = maxColumn;
        }

        retry (5, {
            CellEntry newDataCell = new CellEntry(maxRow, columnNameMap[col.key], col.value);
            service.insert(cellFeedUrl, newDataCell);
        });
    }
}

private void retry(int count, def c) {
    for (int i = 0; i < count; i++) {
        try {
            c();
            return;
        } catch (Throwable t) {
            println("Failed due to: " + t.message + ", try " + (i+1) + " of " + count);
            Thread.sleep(10000L * (i+1));
        }
    }
}

private SpreadsheetEntry getSpreadSheet(SpreadsheetService service, String name) {
    String spreadsheetName = "perftest - " + name;

    // Make a request to the API and get all spreadsheets.
    SpreadsheetFeed feed = service.getFeed(SPREADSHEET_FEED_URL, SpreadsheetFeed.class);
    List<SpreadsheetEntry> spreadsheets = feed.getEntries();
    for (SpreadsheetEntry entry : spreadsheets) {
        if (spreadsheetName == entry.getTitle().getPlainText()) {
            return entry;
        }
    }
    return null;
}

private Credential authorize(HttpTransport transport, JsonFactory jsonFactory) {
    FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(CREDENTIALS_STORE_DIR);

    /** OAuth 2.0 scopes. */
    final List<String> SCOPES = Arrays.asList(
            "https://spreadsheets.google.com/feeds",
            "https://docs.google.com/feeds");
    Reader clientSecretsIS = new FileReader(CLIENT_SECRETS_FILE);
    try {
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory, clientSecretsIS);

        // redirect to an authorization page
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(transport, jsonFactory, clientSecrets, SCOPES)
                                                .setDataStoreFactory(dataStoreFactory)
                                                .build();
        VerificationCodeReceiver codeReceiver = new AbstractPromptReceiver() {
            @Override
            String getRedirectUri() throws Exception {
                return "http://localhost";
            }

            @Override
            String waitForCode() {
                String url = flow.newAuthorizationUrl().setRedirectUri(getRedirectUri()).build();
                System.out.println("Browse here and get the token:")
                System.out.println(url);
                return super.waitForCode()
            }
        };


        AuthorizationCodeInstalledApp installedApp = new AuthorizationCodeInstalledApp(flow, codeReceiver);
        return installedApp.authorize(USER_ID);
    } finally {
        clientSecretsIS.close();
    }

}
[root@jsightle-windup-jenkins perftest]# 

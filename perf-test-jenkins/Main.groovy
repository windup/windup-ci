#!/bin/bash
//usr/bin/env groovy  -cp groovy/csvcompare-0.0.1-SNAPSHOT.jar "$0" $@; exit $?

import groovy.transform.Field

/*
 * Runs Windup for given test application, compares new outputs with outputs from previous run, logs performance stats
 * into DB and uploads them to Google spreadsheet.
 *
 * Expected inputs and environment settings:
 * - args[0] -- path to Windup binary
 * - args[1] -- path to dir with shared data, "testapps_output" and "test_output_summaries" dirs are expected to be
 *              present there, summary outputs are stored in the newest dir (according to name in +%Y_%m_%d_%H%M format)
 *              in the "test_output_summaries" dir
 * - args[2] -- test app name, determines which Google spreadsheet to use
 * - args[3] -- path to test app file
 * - .credentials -- direcory with credatial to access Google services is present in the dir with this script
 * - CSVCompare.groovy, RuleProviderReportUtil.groovy, SummaryResult.groovy, SummaryResultDb.groovy,
 *   SummaryResultGoogleSpreadsheet.groovy in the dir with this script
 * - csvcompare-0.0.1-SNAPSHOT.jar in the "groovy" subdir of the dir with this script
 *
 * Outputs:
 * - Windup outputs for all Windup runs on given test app in "${args[1]}/testapps_output" subdirs
 * - summary outputs (CSV and HTML files)for Windup runs on given test app in the last subdir
 *   of "${args[1]}/test_output_summaries" dir
 * - summary performance stats are logged into DB and uploaded to Google spreadsheet according to test app name
 *
 * TODO:
 * - improve CSV export from Windup CSV comparator
 * - add check against fixed set of results (e.g. from the last Windup release)
 * - add script argument processing
 * - switch to Commons-CSV http://www.groovy-tutorial.org/basic-csv/#_reading_a_csv (see TODO below)
 */


// How many times to test with given application
@Field final int NUMBER_OF_RUNS = 1

// The maximum number of standard deviations from the mean before reporting error
@Field final double STANDARD_DEVIATION_ERROR_THRESHOLD = 2.0


// Directory containing this script
final File SCRIPT_DIR = new File(getClass().protectionDomain.codeSource.location.path).parentFile
System.setProperty("SCRIPT_DIR", SCRIPT_DIR.absolutePath)
println("SCRIPT_DIR: ${SCRIPT_DIR.absolutePath}")

// Windup binary to execute
final File WINDUP_BIN = new File(args[0])
println("WINDUP_BIN: ${WINDUP_BIN}")

// Test application to be processed by Windup
@Field final String TEST_APP_NAME = args[2]
println("TEST_APP_NAME: ${TEST_APP_NAME}")
final File TEST_APP_FILE = new File(args[3])
println("TEST_APP_FILE: ${TEST_APP_FILE}")
if (!TEST_APP_FILE.exists()) {
    println("Error: test application file ${TEST_APP_FILE} should exist")
    System.exit(1)
}

// Directory where to store Windup output reports for test applications
@Field final File REPORT_BASE_DIR = new File("${args[1]}/testapps_output")
println("REPORT_BASE_DIR: ${REPORT_BASE_DIR}")

@Field final File SUMMARIES_OUTPUT_DIR = new File("${args[1]}/test_output_summaries")

// Directory where to strore exported csv files and rule provider reports
@Field final File SUMMARY_OUTPUT_DIR = new File(getCurrentRuleSummaryDirectory(), TEST_APP_NAME)
SUMMARY_OUTPUT_DIR.mkdirs()
println("SUMMARY_OUTPUT_DIR: ${SUMMARY_OUTPUT_DIR}")
if (SUMMARY_OUTPUT_DIR.list().length > 0) {
    println("Error: summary output dir should be empty")
    System.exit(1)
}


println("Running Windup for ${TEST_APP_NAME}: ${NUMBER_OF_RUNS} run(s) on test file ${TEST_APP_FILE}")
SummaryResult summaryResult = runWindupOnTestApp(WINDUP_BIN, TEST_APP_NAME, TEST_APP_FILE)

println("Checking Windup outputs for ${TEST_APP_NAME}")
summaryResult = checkOutputs(TEST_APP_NAME, summaryResult)

println("Storing results for ${TEST_APP_NAME} into database")
summaryResult = SummaryResultDb.logResultAndCheckAverageTime(summaryResult, STANDARD_DEVIATION_ERROR_THRESHOLD);

println("Uploading results for ${TEST_APP_NAME} to Google spreadsheet")
SummaryResultGoogleSpreadsheet.logResult(summaryResult);


if (summaryResult.hasErrors()) {
    println("================================")
    println("")
    println("ERROR:: " + summaryResult.errors)
    println("")
    println("================================")

    File errorsReport = new File(SUMMARY_OUTPUT_DIR, "errors-found.txt");
    errorsReport << summaryResult.errors;
    println("Previous error report is stored in ${errorsReport}")

    System.exit(1)
}


private SummaryResult runWindupOnTestApp(File windup, String testAppName, File testAppFile) {
    SummaryResult summaryResult = new SummaryResult(testAppName);
    long totalTime = 0;

    for (int i = 1; i <= NUMBER_OF_RUNS; i++) {
        File reportDir = new File(REPORT_BASE_DIR, testAppName + "-" + i);
        reportDir.mkdirs();

        String command = "${windup.getAbsolutePath()} --input ${testAppFile.getAbsolutePath()} \
                --output ${reportDir.getAbsolutePath()} --overwrite --batchMode --target eap --exportCSV --offline";
        long startTime = System.currentTimeMillis();

        def env = [:];
        env.putAll(System.getenv());
        env.put("MAX_MEMORY", "8192m");

        def windupProc = command.execute(env.collect { k, v -> "$k=$v" }, new File("."));
        windupProc.consumeProcessOutput();
        windupProc.waitFor();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        totalTime += duration;

        println("Reports from run ${i} for app ${testAppName} created in ${duration} ms")
    }

    long averageTimeMillis = totalTime / NUMBER_OF_RUNS;
    println("Reports from all runs for app ${testAppName} created in ${totalTime} ms, avg ${averageTimeMillis} ms / run")

    summaryResult.averageTimeMillis = averageTimeMillis
    return summaryResult;
}

private SummaryResult checkOutputs(String testAppName, SummaryResult summaryResult) {
    String errors = "";
    def detailedStats = new LinkedHashMap();
    def ruleStats = new LinkedHashMap();
    def phaseStats = new LinkedHashMap();

    for (int i = 1; i <= NUMBER_OF_RUNS; i++) {
        File reportDir = new File(REPORT_BASE_DIR, testAppName + "-" + i);

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

        // copy a couple of summary files to the summary folder
        // report dir reportDir
        for (File f : reportDir.listFiles()) {
            if (f.getName().endsWith(".csv")) {
                File outFile = new File(SUMMARY_OUTPUT_DIR, f.getName());
                outFile << f.text;
            }
        }

        // Find the immediate predecessor to this one
        File previousRuleSummariesDirectory = new File(getPreviousRuleSummariesDirectory(), testAppName);

        println("Comparing ${previousRuleSummariesDirectory} with ${SUMMARY_OUTPUT_DIR}")
        String csvComparisonResult = CSVCompare.compare(previousRuleSummariesDirectory, SUMMARY_OUTPUT_DIR);
        if (csvComparisonResult != null && csvComparisonResult.length() > 0) {
            errors += "Application: " + testAppName + " returned differences:\n" + csvComparisonResult + "\n";
        }

        // copy the rule providers report
        File ruleProviderReport = new File(reportDir, "reports/windup_ruleproviders.html");
        if (!ruleProviderReport.exists()) {
            errors += "Application: " + testAppName + " is missing the rule provider report!";
        } else {
            File newRuleProviderReport = new File(SUMMARY_OUTPUT_DIR, "windup_ruleproviders.html");
            newRuleProviderReport << ruleProviderReport.text

            def failedItems = RuleProviderReportUtil.listFailedRules(newRuleProviderReport.toString());
            failedItems.each {
                errors += "On application: " + testAppName + ", Rule: " + it.ruleID + " failed to execute!\n";
            }

            File previousRuleProviderReport = new File(previousRuleSummariesDirectory, "windup_ruleproviders.html");
            if (!previousRuleProviderReport.exists()) {
                println "No previous rule provider report is available for comparisong for application: " + testAppName;
            } else {
                def diffs = RuleProviderReportUtil.findDifferences(previousRuleProviderReport.toString(), newRuleProviderReport.toString());
                if (!diffs.onlyInPrevious.isEmpty() || !diffs.onlyInNew.isEmpty()) {
                    println "=======================";
                    println " Differences in rule executed vs the previous run for " + testAppName + ":";

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

    summaryResult.detailedStats = detailedStats;
    summaryResult.ruleStats = ruleStats;
    summaryResult.phaseStats = phaseStats;
    summaryResult.errors += errors;

    return summaryResult;
}

private File getPreviousRuleSummariesDirectory() {
    List<File> allSummaryDirectories = new ArrayList<>();
    allSummaryDirectories.addAll(SUMMARIES_OUTPUT_DIR.listFiles());
    allSummaryDirectories.sort();

    if (allSummaryDirectories.size > 1) {
        return allSummaryDirectories[-2];
    } else {
        return allSummaryDirectories[0];
    }
}

private File getCurrentRuleSummaryDirectory() {
    List<File> allSummaryDirectories = new ArrayList<>();
    allSummaryDirectories.addAll(SUMMARIES_OUTPUT_DIR.listFiles());
    allSummaryDirectories.sort();

    return allSummaryDirectories[-1];
}

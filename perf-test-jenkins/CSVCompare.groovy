import java.io.File;

import org.jboss.windup.utils.*;
import org.jboss.windup.utils.model.ExportReportModelToCSV;
import org.jboss.windup.utils.model.ReportModel;

public class CSVCompare {

    public static String compare(File oldOutputs, File currentOutputs) {
        String result = "";

        for (File currentCsv : currentOutputs.listFiles()) {
            if (currentCsv.getName().toLowerCase().endsWith(".csv")) {
                File oldCsv = new File(oldOutputs, currentCsv.getName());
                if (!oldCsv.exists()) {
                    result += "The CSV output " + currentCsv + " is not present in old outputs " + oldOutputs + "\n";
                } else {
                    result += compareCsvFiles(oldCsv, currentCsv);
                }
            }
        }

        for (File oldCsv : oldOutputs.listFiles()) {
            if (oldCsv.getName().toLowerCase().endsWith(".csv")) {
                File currentCsv = new File(currentOutputs, oldCsv.getName());
                if (!currentCsv.exists()) {
                    result += "The CSV output " + oldCsv + " is not present in current outputs " + currentOutputs + "\n";
                }
            }
        }

        return result;
    }

    private static String compareCsvFiles(File oldCsv, File currentCsv) {
        String result = "";

        println "  Comparing old file " + oldCsv;
        println "  with current file  " + currentCsv;

        try {
            List<ReportModel> oldCsvLines = new CsvWindupExportLoader(oldCsv.toURL(), (char) ',').parseCSV();
            List<ReportModel> currentCsvLines = new CsvWindupExportLoader(currentCsv.toURL(), (char) ',').parseCSV();

            WindupReportComparison extraInCurrentComparision = new WindupReportComparison(oldCsvLines, currentCsvLines);
            List<ReportModel> extraLinesInCurrent = extraInCurrentComparision.compareNewAndOldReports();
            if (!extraLinesInCurrent.isEmpty()) {
                result += "* Extra lines in current CSV report " + currentCsv + "\n"
                result += "  see following lines or " + currentCsv + "-extra_in_current-diff.csv\n"
                result += extraLinesInCurrent.join(",\n") + "\n";
                (new ExportReportModelToCSV(extraLinesInCurrent))
                        .export(new File(currentCsv.parent, currentCsv.name + "-extra_in_current-diff.csv"));
            }

            WindupReportComparison extraInOldComparision = new WindupReportComparison(currentCsvLines, oldCsvLines);
            List<ReportModel> extraLinesInOld = extraInOldComparision.compareNewAndOldReports();
            if (!extraLinesInOld.isEmpty()) {
                result += "* Extra lines in old CSV report " + oldCsv + "\n"
                result += "  see following lines or " + currentCsv + "-extra_in_old-diff.csv\n"
                result += extraLinesInOld.join(",\n") + "\n";
                (new ExportReportModelToCSV(extraLinesInOld))
                        .export(new File(currentCsv.parent, currentCsv.name + "-extra_in_old-diff.csv"));
            }
        } catch (MalformedURLException e) {
           result += "MalformedURLException: " + e.getMessage() + "\n";
        } catch (IOException ioe) {
           result += "Error while exporting resulted difference to file - " + ioe.getLocalizedMessage() + "\n";
        }

        return result;
    }

}

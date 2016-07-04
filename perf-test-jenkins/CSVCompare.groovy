import java.io.File;

import org.jboss.windup.utils.*;
import org.jboss.windup.utils.model.ExportReportModelToCSV;
import org.jboss.windup.utils.model.ReportModel;

public class CSVCompare {
    public static String compare(File directory1, File directory2) {
        String result = "";
        File[] csvFiles = directory2.listFiles();
        for (File file : csvFiles) {
            if (file.getName().toLowerCase().endsWith(".csv")) {
                File file1 = new File(directory1, file.getName());
                if (!file1.exists())
                    result += "Previous rule points information is missing for: " + file + " (previous directory: " + directory1 + ")\n";
                else
                    result += compareCSVFiles(file1, file);
            }
        }
        return result;
    }

    private static String compareCSVFiles(File file1, File file2) {
        String result = "";
        try
        {
            CsvWindupExportLoader loader1 = new CsvWindupExportLoader(file1.toURL(), (char)',');
            println "Original file " + file1;
            CsvWindupExportLoader loader2 = new CsvWindupExportLoader(file2.toURL(), (char)',');
            println "New file " + file2;
            WindupReportComparison reportCmp = new WindupReportComparison(loader1.parseCSV(), loader2.parseCSV());
            List<ReportModel> listDiff = reportCmp.compareNewAndOldReports();
            if (listDiff.size()> 0) {
                result += listDiff.toString();
                (new ExportReportModelToCSV(listDiff)).export(new File("diff.csv"));
            }
        }
        catch (MalformedURLException e) {
           result += "MalformedURLException: " + e.getMessage() + "\n";
        } catch (IOException ioe) {
           result += "Error while exporting resulted difference to file - " + ioe.getLocalizedMessage() + "\n";
        }
        return result;
    }
}

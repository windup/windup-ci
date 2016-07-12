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

// Downloads necessary dependencies
@GrabConfig(systemClassLoader = true)
@Grab('com.google.oauth-client:google-oauth-client:1.20.0')
@Grab('com.google.apis:google-api-services-oauth2:v2-rev88-1.20.0')
@Grab('com.google.gdata:core:1.47.1')
@Grab('com.google.http-client:google-http-client-jackson2:1.15.0-rc')

/**
 * Enables to log result into Google spreadsheet.
 */
public class SummaryResultGoogleSpreadsheet {

    // User id used to access Google services
    private static String USER_ID = "windupdocs@gmail.com"

    // Directory containing this script
    private static File SCRIPT_DIR = new File(System.getProperty("SCRIPT_DIR"))

    // Directory with credetials used to access Google services
    private static File CREDENTIALS_STORE_DIR = new File(SCRIPT_DIR, ".credentials")
    private static File CLIENT_SECRETS_FILE = new File(CREDENTIALS_STORE_DIR, "client_secrets.json")
    private static URL SPREADSHEET_FEED_URL = new URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full")


    public static void logResult(SummaryResult summaryResult) {
        String testAppName = summaryResult.testAppName;

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
        SpreadsheetEntry spreadsheet = getSpreadSheet(service, testAppName);
        if (spreadsheet == null) {
            throw new RuntimeException("Failed to find spreadsheet for " + testAppName + " (name should be \"perftest - " + testAppName + "\")");
        }
        println("Uploading to spradsheet ${spreadsheet.spreadsheetLink.href}")

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
        mapRow['Name'] = testAppName;
        mapRow['Duration (in Seconds)'] = String.valueOf((summaryResult.averageTimeMillis / 1000).intValue());
        updateWorksheet(service, totalWorksheet, mapRow);

        def byRuleRow = new LinkedHashMap();
        byRuleRow['Date'] = dateFormatted;
        for (def e : summaryResult.ruleStats) {
            double timePerExec = e.value["total"] == 0 ? 0 : (e.value["total"] / e.value["executions"]).doubleValue();
            byRuleRow[e.key] = String.valueOf(timePerExec);
        }
        updateWorksheet(service, byRuleProviderWorksheet, byRuleRow);

        def byPhaseRow = new LinkedHashMap();
        byPhaseRow['Date'] = dateFormatted;
        for (def e : summaryResult.phaseStats) {
            double timePerExec = e.value["total"] == 0 ? 0 : (e.value["total"] / e.value["executions"]).doubleValue();
            byPhaseRow[e.key] = String.valueOf(timePerExec);
        }
        updateWorksheet(service, byPhaseWorksheet, byPhaseRow);
    }

    private static Credential authorize(HttpTransport transport, JsonFactory jsonFactory) {
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

    private static SpreadsheetEntry getSpreadSheet(SpreadsheetService service, String name) {
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

    private static void updateWorksheet(SpreadsheetService service, WorksheetEntry worksheet, Map values) {
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

    private static void retry(int count, def c) {
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

}

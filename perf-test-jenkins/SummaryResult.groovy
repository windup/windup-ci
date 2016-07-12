/**
 * Summary result for given test app.
 */
public class SummaryResult {

    String testAppName;
    long averageTimeMillis;
    Map detailedStats;
    Map ruleStats;
    Map phaseStats;
    String errors = "";

    public SummaryResult(String testAppName) {
        this.testAppName = testAppName;
    }

    public boolean hasErrors() {
        return this.errors != null && !this.errors.trim().isEmpty();
    }
}

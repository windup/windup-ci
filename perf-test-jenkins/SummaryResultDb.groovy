import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet

// Downloads necessary dependencies
@GrabConfig(systemClassLoader = true)
@Grab('org.postgresql:postgresql:9.4-1201-jdbc41')


/**
 * Enables to log result to DB and to compare current and previous result.
 */
public class SummaryResultDb {

    private static final String DB_URL = "jdbc:postgresql://localhost/perftest";
    private static final String DB_USER = "perftest";
    private static final String DB_PASSWORD = "";

    static {
        // Loads PostgreSQL driver
        Class.forName("org.postgresql.Driver");
    }

    public static SummaryResult logResultAndCheckAverageTime(SummaryResult summaryResult,
            double standardDeviationErrorThreshold) {

        String testAppName = summaryResult.testAppName;
        long averageTime = (summaryResult.averageTimeMillis / 1000).longValue();
        String errors = "";

        Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        long perfTestID = 0;
        try {
            String insertMainRow = "insert into performance_test(name, average_time_per_run, date_executed) values (?, ?, now()) returning performance_test_id";
            PreparedStatement pstmt = connection.prepareStatement(insertMainRow);
            try {
                pstmt.setString(1, testAppName);
                pstmt.setLong(2, averageTime);
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
                getAvgStatement.setString(1, testAppName);
                ResultSet rs = getAvgStatement.executeQuery();
                rs.next();
                double previousAverageTime = rs.getDouble(1);
                double previousStandardDeviation = rs.getDouble(2);

                double diffFromAverage = Math.abs(averageTime - previousAverageTime);
                double maxDiff = (double) standardDeviationErrorThreshold * previousStandardDeviation;
                if (diffFromAverage > maxDiff) {
                    errors = "Performance out of spec for " + testAppName
                            + ": diff " + diffFromAverage + " from average, but the the threshold ("
                            + standardDeviationErrorThreshold + "*" + previousStandardDeviation + ") is: " + maxDiff
                            + " (Previous Average: " + previousAverageTime + ", StdDev: " + previousStandardDeviation
                            + ", Current Runtime: " + (averageTime) + ")\n";
                }

            } finally {
                getAvgStatement.close();
            }


            // insert detailed stats
            for (def row : summaryResult.detailedStats) {
                String sql = "insert into detailed_stats (performance_test_id, name, number_of_executions, total_time_taken, average_time_taken) values (?, ?, ?, ?, ?)";
                PreparedStatement insert = connection.prepareStatement(sql);
                insert.setLong(1, perfTestID);
                insert.setString(2, row.key);
                insert.setInt(3, row.value["executions"]);
                insert.setInt(4, row.value["total"]);
                double timePerExec = row.value["total"] == 0 ? 0 : (row.value["total"] / row.value["executions"]).doubleValue();
                insert.setDouble(5, timePerExec);
                insert.executeUpdate();
            }

            // insert rule stats
            for (def row : summaryResult.ruleStats) {
                String sql = "insert into rule_stats (performance_test_id, name, average_time_taken) values (?, ?, ?)";
                PreparedStatement insert = connection.prepareStatement(sql);
                insert.setLong(1, perfTestID);
                insert.setString(2, row.key);
                double timePerExec = row.value["total"] == 0 ? 0 : (row.value["total"] / row.value["executions"]).doubleValue();
                insert.setDouble(3, timePerExec);
                insert.executeUpdate();
            }

            // insert phase stats
            for (def row : summaryResult.phaseStats) {
                String sql = "insert into phase_stats (performance_test_id, name, average_time_taken) values (?, ?, ?)";
                PreparedStatement insert = connection.prepareStatement(sql);
                insert.setLong(1, perfTestID);
                insert.setString(2, row.key);
                double timePerExec = row.value["total"] == 0 ? 0 : (row.value["total"] / row.value["executions"]).doubleValue();
                insert.setDouble(3, timePerExec);
                insert.executeUpdate();
            }
        } finally {
            connection.close();
        }

        summaryResult.errors += errors;
        return summaryResult;
    }
}

// Imports the Google Cloud client library
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import java.io.FileInputStream;
import com.google.common.collect.Lists;
import com.google.auth.oauth2.GoogleCredentials;

/**
 * A quick start code for Cloud Spanner. It demonstrates how to setup the Cloud Spanner client and
 * execute a simple query using it against an existing database.
 */
public class CloudSpannerRead {
    public static void main(String... args) throws Exception {

        /*if (args.length != 2) {
            System.err.println("Usage: QuickStartSample <instance_id> <database_id>");
            return;
        }*/
        // Instantiates a client
        String jsonPath = "/Users/hgo2/google/keys/tst2-integration-bf32-8ca0b161e68a.json";
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(jsonPath))
                .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));

        SpannerOptions options = SpannerOptions.newBuilder()
                .setProjectId("tst2-integration-bf32")
                .setCredentials(credentials)
                .build();
        Spanner spanner = options.getService();

        // Name of your instance & database.
        //String instanceId = args[0];
        //String databaseId = args[1];
        String instanceId = "test-instance";
        String databaseId = "my_database";
        try {
            // Creates a database client
            DatabaseClient dbClient =
                    spanner.getDatabaseClient(DatabaseId.of(options.getProjectId(), instanceId, databaseId));
            // Queries the database
            ResultSet resultSet = dbClient.singleUse().executeQuery(Statement.of("select * from my_table"));

            System.out.println("\n\nResults:");
            // Prints the results
            while (resultSet.next()) {
                System.out.printf("%d\n\n", resultSet.getLong(0));
                System.out.println(resultSet.getString(1));
            }
        } finally {
            // Closes the client which will free up the resources used
            spanner.close();
        }
    }
}

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
public class CloudSpannerInsert {
    public static void main(String... args) throws Exception {

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
        String instanceId = "test-instance";
        String databaseId = "my_database";
        try {
            // Creates a database client
            DatabaseClient dbClient =
                    spanner.getDatabaseClient(DatabaseId.of(options.getProjectId(), instanceId, databaseId));

            dbClient
                    .readWriteTransaction()
                    .run(transaction -> {
                        String sql =
                                "INSERT INTO my_table (id_column, name_column) VALUES "
                                        + "(14, 'Garcia'), "
                                        + "(15, 'Morales')";
                        long rowCount = transaction.executeUpdate(Statement.of(sql));
                        System.out.printf("%d records inserted.\n", rowCount);
                        return null;
                    });

        } finally {
            // Closes the client which will free up the resources used
            spanner.close();
        }
    }
}

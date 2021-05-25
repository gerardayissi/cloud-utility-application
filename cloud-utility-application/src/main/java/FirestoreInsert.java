//import com.example.firestore.snippets.model.City;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.Transaction;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.common.collect.Lists;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class FirestoreInsert {
    //private static Firestore db;
    /*
    FirestoreInsert(Firestore db) {
        this.db = db;
    }
    */

    public static void main(String... args) throws Exception {
        System.out.println("Insert Data in FireStore");
        // Create a Map to store the data we want to set

        // Instantiates a client
        String jsonPath = "/Users/hgo2/google/keys/tst2-integration-bf32-8ca0b161e68a.json";
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(jsonPath))
                .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));

        FirestoreOptions options = FirestoreOptions.newBuilder()
                .setProjectId("tst2-integration-bf32")
                .setCredentials(credentials)
                .build();
        Firestore  db  = options.getService();


        Map<String, Object> docData = new HashMap<>();
        docData.put("name", "Los Angeles");
        docData.put("state", "CA");
        docData.put("country", "USA");
        docData.put("regions", Arrays.asList("west_coast", "socal"));
// Add a new document (asynchronously) in collection "users" with id "LA"
        ApiFuture<WriteResult> future = db.collection("users").document("LA").set(docData);
// ...
// future.get() blocks on response
        System.out.println("Update time : " + future.get().getUpdateTime());
    }
}


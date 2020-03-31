package com.tailoredbrands.business_interface.item_full_feed;

import com.google.api.core.ApiFuture;
import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.batching.FlowController;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.threeten.bp.Duration;

public class ItemFullFeedPublishMAOTest {

    public static void main(String[] args) throws Exception {
        Credentials MAOCredentials = ServiceAccountCredentials.fromStream(
                ClassLoader.getSystemClassLoader().getResourceAsStream("keys/tssls4-pubsubrw.json"));


        Publisher publisher = Publisher.newBuilder("projects/tasl-omni-stg-04-ops/topics/INB_XINT_ItemQueueMSGType_GCPQ")
                .setCredentialsProvider(FixedCredentialsProvider.create(MAOCredentials))
                .setBatchingSettings(
                        BatchingSettings.newBuilder()
                                .setElementCountThreshold(1000L)
                                .setRequestByteThreshold(1024 * 1024L)
                                .setDelayThreshold(Duration.ofSeconds(1))
                                .setFlowControlSettings(
                                        FlowControlSettings.newBuilder()
                                                .setLimitExceededBehavior(FlowController.LimitExceededBehavior.Ignore)
                                                .build())
                                .build())
                .build();

        PubsubMessage message = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8("{\"BaseUOM\":\"U\",\"Brand\":\"TMW\",\"ItemId\":\"TMW101H90000\",\"LargeImageURI\":\"TEST18\",\"SmallImageURI\":\"http://images.menswearhouse.com/is/image/TMW/MW40_100D_JOSEPH_FEISS_GOLD_SPORT_COATS_VESTS_TAN_WINDOWPANE_MAIN?$pickTicket$\",\"Media\":{\"MediaId\":\"TMW100C35196\",\"MediaSizeId\": \"Large\",\"MimeTypeId\": \"image/jpeg\",\"Sequence\": \"1\",\"URI\":\"http://images.menswearhouse.com/is/image/TMW/MW40_100D_JOSEPH_FEISS_GOLD_SPORT_COATS_VESTS_TAN_WINDOWPANE_MAIN?$pickTicket$\"},\"Extended\": {\"EcomProductName\":\"Joseph &amp; Feiss Gold Classic Fit Sport Coat, Tan Windowpane\",\"EcomColor\":\"TAN CHECK\",\"EcomSize\":\"35 Short\"}}"))
                .putAttributes("User", "admin@tbi.com")
                .putAttributes("Organization", "TMW")
                .build();

        ApiFuture<String> messageId = publisher.publish(message);
        System.out.println("Published message: " + message);
        publisher.publishAllOutstanding();
        System.out.println("Message Id: " + messageId.get());
    }
}

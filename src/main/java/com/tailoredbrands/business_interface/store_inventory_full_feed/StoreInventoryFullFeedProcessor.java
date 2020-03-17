package com.tailoredbrands.business_interface.store_inventory_full_feed;

import com.fasterxml.jackson.databind.JsonNode;
import com.tailoredbrands.generated.json.store_inventory_full_feed.Sync;
import com.tailoredbrands.generated.json.store_inventory_full_feed.SyncSupplyEvent;
import com.tailoredbrands.pipeline.error.ProcessingException;
import com.tailoredbrands.pipeline.options.GcsToPubSubOptions;
import com.tailoredbrands.util.json.JsonUtils;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.val;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.tailoredbrands.pipeline.error.ErrorType.CSV_ROW_TO_OBJECT_CONVERSION_ERROR;
import static com.tailoredbrands.pipeline.error.ErrorType.OBJECT_TO_JSON_CONVERSION_ERROR;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static java.util.Collections.singletonList;

public class StoreInventoryFullFeedProcessor extends PTransform<PCollection<Map<String, String>>,
    PCollection<Tuple2<Map<String, String>, Try<JsonNode>>>> {

    private String user;
    private String organization;
    private Integer THRESHOLD;

    public StoreInventoryFullFeedProcessor(PipelineOptions options) {
        if (options instanceof GcsToPubSubOptions) {
            val gcsToPubSubOptions = (GcsToPubSubOptions) options;
            user = gcsToPubSubOptions.getUser();
            organization = gcsToPubSubOptions.getOrganization();
            THRESHOLD = gcsToPubSubOptions.getErrorThreshold();
        } else {
            throw new IllegalArgumentException("Invalid Store Inventory Full Feed options: " + options.getClass().getSimpleName());
        }
    }

    @Override
    public PCollection<Tuple2<Map<String, String>, Try<JsonNode>>> expand(PCollection<Map<String, String>> rows) {
        return rows
            .apply("CSV row to Start Sync DTO", csvRowToStartSyncDto())
            .apply("DTO to JSON", dtoToJson());
    }

    MapElements<Map<String, String>, Tuple2<Map<String, String>, Try<Sync>>> csvRowToStartSyncDto() {
        return MapElements
            .into(new TypeDescriptor<Tuple2<Map<String, String>, Try<Sync>>>() {
            })
            .via(csvRow -> new Tuple2<>(csvRow, Try.of(() -> toStartSync(csvRow))
                .mapFailure(Case($(e -> !(e instanceof ProcessingException)),
                    exc -> new ProcessingException(CSV_ROW_TO_OBJECT_CONVERSION_ERROR, exc))))
            );
    }

    private Sync toStartSync(Map<String, String> csvRow) {
        val startSync = new Sync();
        val supplyEvent = new SyncSupplyEvent();
        supplyEvent.setTransactionNumber(csvRow.get("StoreNumber") + getDatetime("yyyymmddhhmiss"));
        supplyEvent.setTransactionTypeId("StartSync");
        supplyEvent.setLocationId(csvRow.get("StoreNumber"));
        supplyEvent.setSyncTransactionDate(getDatetime("yyyy-MM-ddHH:mm:ss"));
        supplyEvent.setFullSync(true);
        startSync.setSyncSupplyEvent(supplyEvent);
        return startSync;
    }

    static String getDatetime(String format) {
        val localDateTime = LocalDateTime.now(ZoneOffset.UTC);
        val formatter = DateTimeFormatter.ofPattern(format);
        return localDateTime.format(formatter);
    }

    @SuppressWarnings("unchecked")
    MapElements<Tuple2<Map<String, String>, Try<Sync>>, Tuple2<Map<String, String>, Try<JsonNode>>> dtoToJson() {
        return MapElements
            .into(new TypeDescriptor<Tuple2<Map<String, String>, Try<JsonNode>>>() {
            })
            .via(tuple -> tuple.map2(
                maybeItem -> maybeItem
                    .map(JsonUtils::toJsonNode)
                    .map(this::toJsonWithAttributes)
                    .mapFailure(Case($(e -> !(e instanceof ProcessingException)),
                        exc -> new ProcessingException(OBJECT_TO_JSON_CONVERSION_ERROR, exc))))
            );
    }

    private JsonNode toJsonWithAttributes(JsonNode json) {
        val attributes = new LinkedHashMap<String, String>(2);
        attributes.put("User", user);
        attributes.put("Organization", organization);

        val message = new LinkedHashMap<String, Object>(2);
        message.put("attributes", attributes);
        message.put("data", json);

        val payload = new HashMap<String, List<Map<String, Object>>>(1);
        payload.put("messages", singletonList(message));

        return JsonUtils.toJsonNode(payload);
    }
}

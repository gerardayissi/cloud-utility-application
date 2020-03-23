package com.tailoredbrands.business_interface.store_inventory_full_feed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tailoredbrands.generated.json.store_inventory_full_feed.*;
import com.tailoredbrands.pipeline.error.ProcessingException;
import com.tailoredbrands.pipeline.options.GcsToPubSubOptions;
import com.tailoredbrands.util.json.JsonUtils;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.val;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.Combine;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.joda.time.Duration;

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
    PCollectionList<Tuple2<List<Map<String, String>>, Try<JsonNode>>>> {

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
    public PCollectionList<Tuple2<List<Map<String, String>>, Try<JsonNode>>> expand(PCollection<Map<String, String>> rows) {

        Duration windowDuration = Duration.standardSeconds(3);
        Window<Tuple2<Map<String, String>, Try<SupplyDetail>>> window = Window.into(FixedWindows.of(windowDuration));

        val mainPC = rows
            .apply("CSV row to DTO", csvRowToSupplyDetailsDto())
            .apply("Declare window", window)
            .apply("Combine DTOs into list", Combine.globally(new CombineRowsFn()).withoutDefaults());

        val startSyncPC = mainPC
            .apply("Transform Sync Start DTO to JSON", dtoToJson("StartSync")).setName("StartSync");

        val syncDetail = mainPC
            .apply("Transform Sync Detail DTO to JSON", dtoToJson("SyncDetail")).setName("syncDetail");

        val endSync = mainPC
            .apply("Transform Sync End DTO to JSON", dtoToJson("EndSync")).setName("endSync");

        val pc = PCollectionList.of(startSyncPC).and(syncDetail).and(endSync);

        return pc;
    }

    MapElements<Map<String, String>, Tuple2<Map<String, String>, Try<SupplyDetail>>> csvRowToSupplyDetailsDto() {
        return MapElements
            .into(new TypeDescriptor<Tuple2<Map<String, String>, Try<SupplyDetail>>>() {
            })
            .via(csvRow -> new Tuple2<>(csvRow, Try.of(() -> toPayloadSync(csvRow))
                .mapFailure(Case($(e -> !(e instanceof ProcessingException)),
                    exc -> new ProcessingException(CSV_ROW_TO_OBJECT_CONVERSION_ERROR, exc))))
            );
    }

    MapElements<Tuple2<List<Map<String, String>>, Try<List<SupplyDetail>>>, Tuple2<List<Map<String, String>>, Try<JsonNode>>> dtoToJson(String syncType) {
        return MapElements
            .into(new TypeDescriptor<Tuple2<List<Map<String, String>>, Try<JsonNode>>>() {
            })
            .via(tuple -> tuple
                .map2(maybeDto -> maybeDto
                    .map(JsonUtils::serializeObject)
                    .map(JsonUtils::deserialize)
                    .map(jsonNode -> toJsonWithAttributes(jsonNode, syncType))
                    .mapFailure(
                        Case($(e -> !(e instanceof ProcessingException)),
                            exc -> new ProcessingException(OBJECT_TO_JSON_CONVERSION_ERROR, exc))
                    )
                )
            );
    }

    private SupplyDetail toPayloadSync(Map<String, String> csvRow) {
        val supplyDetails = new SupplyDetail();
        val supplyDefinition = new SupplyDefinition();
        supplyDefinition.setItemId(csvRow.get("Itemcode"));

        val supplyType = new SupplyType();
        supplyType.setSupplyTypeId("On Hand Available");

        val supplyData = new SupplyData();
        supplyData.setQuantity(csvRow.get("Quantity"));
        supplyData.setUom("U");

        supplyDefinition.setSupplyType(supplyType);
        supplyDefinition.setSupplyData(supplyData);

        supplyDetails.setSupplyDefinition(supplyDefinition);
        return supplyDetails;
    }

    private JsonNode toJsonWithAttributes(JsonNode json, String type) {
        val attributes = new LinkedHashMap<String, String>(2);
        attributes.put("User", user);
        attributes.put("Organization", organization);

        val message = new LinkedHashMap<String, Object>(2);
        message.put("attributes", attributes);

        val transactionNumber = "LocationId" + getDatetime("yyyymmddhhmss");
        val jsonNode = getJsonToPush(type, transactionNumber, json);

        val event = new LinkedHashMap<String, Object>(2);
        event.put("SyncSupplyEvent", jsonNode);

        message.put("data", event);

        val payload = new HashMap<String, List<Map<String, Object>>>(1);
        payload.put("messages", singletonList(message));

        return JsonUtils.toJsonNode(payload);
    }

    private JsonNode getJsonToPush(String type, String transactionNumber, JsonNode json) {
        val syncDTO = toDtoSync(type, transactionNumber, json);
        val jsonMain = JsonUtils.toJsonNode(syncDTO);
        ObjectNode objectNode = (ObjectNode) jsonMain;
        if (type.equals("SyncDetail")) {
            objectNode.set("SupplyDetails", json);
        } else {
            objectNode.remove("SupplyDetails");
        }
        return objectNode;
    }

    private SyncSupplyEvent toDtoSync(String type, String transactionNumber, JsonNode json) {
        val supplyEvent = new SyncSupplyEvent();
        supplyEvent.setTransactionNumber(transactionNumber);
        supplyEvent.setTransactionTypeId(type);
        supplyEvent.setLocationId("Location_id");
        supplyEvent.setSyncTransactionDate(getDatetime("yyyy-MM-dd"));
        supplyEvent.setFullSync(true);
        supplyEvent.setCountRecords(Integer.toString(json.size()));
        return supplyEvent;
    }

    private String getDatetime(String format) {
        val localDateTime = LocalDateTime.now(ZoneOffset.UTC);
        val formatter = DateTimeFormatter.ofPattern(format);
        return localDateTime.format(formatter);
    }
}

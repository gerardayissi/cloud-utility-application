package com.tailoredbrands.business_interface.store_inventory_full_feed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.tailoredbrands.generated.json.store_inventory_full_feed.*;
import com.tailoredbrands.pipeline.error.ProcessingException;
import com.tailoredbrands.pipeline.options.GcsToPubSubOptions;
import com.tailoredbrands.pipeline.pattern.gcs_to_pub_sub.GcsToPubSubCounter;
import com.tailoredbrands.util.FileWithMeta;
import com.tailoredbrands.util.json.JsonUtils;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.val;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.apache.commons.csv.CSVRecord;
import org.joda.time.Duration;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.tailoredbrands.pipeline.error.ErrorType.CSV_ROW_TO_OBJECT_CONVERSION_ERROR;
import static com.tailoredbrands.pipeline.error.ErrorType.OBJECT_TO_JSON_CONVERSION_ERROR;
import static com.tailoredbrands.util.Peek.increment;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static java.util.Collections.singletonList;

public class StoreInventoryFullFeedProcessor extends PTransform<PCollection<FileWithMeta>,
    PCollection<Tuple2<FileWithMeta, List<Try<JsonNode>>>>> {

    private String user;
    private GcsToPubSubCounter counter;
    private Integer batchSize;
    private Long windowDurationSec;

    public StoreInventoryFullFeedProcessor(PipelineOptions options) {
        if (options instanceof GcsToPubSubOptions) {
            val gcsToPubSubOptions = (GcsToPubSubOptions) options;
            user = gcsToPubSubOptions.getUser();
            batchSize = gcsToPubSubOptions.getBatchPayloadSize();
            counter = new GcsToPubSubCounter(gcsToPubSubOptions.getBusinessInterface());
            windowDurationSec = gcsToPubSubOptions.getDurationSeconds();
        } else {
            throw new IllegalArgumentException("Invalid Store Inventory Full Feed options: " + options.getClass().getSimpleName());
        }
    }

    @Override
    public PCollection<Tuple2<FileWithMeta, List<Try<JsonNode>>>> expand(PCollection<FileWithMeta> rows) {

        Duration windowDuration = Duration.standardSeconds(windowDurationSec);
        Window<Tuple2<FileWithMeta, Try<SupplyDetail>>> window = Window.into(FixedWindows.of(windowDuration));

        val mainPC2 = rows
            .apply("Read all file records", readFileRecords())
            .apply("Count original CSV records", increment(counter.csvRowsRead))
            .apply("CSV row to DTO", csvRowToSupplyDetailsDto())
            .apply("Declare window", window)
            .apply("Transform to KV", toKV())
            .apply(GroupByKey.create())
            .apply("Combine payload per file", ParDo.of(new CombineRowsPerKey()));

        val startSyncPC = mainPC2
            .apply("Transform StartSync DTO to JSON", dtoToJson())
            .apply("StartSync Json to Push", toFinalJson("StartSync"))
            .setName("StartSync");

        val syncDetail = mainPC2
            .apply("Transform SyncDetail DTO to JSON", dtoToJson())
            .apply("SyncDetail Json to Push", toFinalJson("SyncDetail"))
            .setName("SyncDetail");

        val endSync = mainPC2
            .apply("Transform EndSync DTO to JSON", dtoToJson())
            .apply("EndSync Json to Push", toFinalJson("EndSync"))
            .setName("EndSync");

        val pcs = PCollectionList.of(startSyncPC).and(syncDetail).and(endSync);
        val finalPc = pcs.apply(Flatten.pCollections());

        return finalPc;
    }

    MapElements<Tuple2<FileWithMeta, KV<Integer, CSVRecord>>, Tuple2<FileWithMeta, Try<SupplyDetail>>> csvRowToSupplyDetailsDto() {
        return MapElements
            .into(new TypeDescriptor<Tuple2<FileWithMeta, Try<SupplyDetail>>>() {
            })
            .via(tuple2 -> tuple2
                .map2(kv -> Try.of(() -> toSupplyDetailDto(kv.getValue().toMap()))
                    .mapFailure(Case($(e -> !(e instanceof ProcessingException)),
                        exc -> new ProcessingException(CSV_ROW_TO_OBJECT_CONVERSION_ERROR, exc, kv.getKey()))))
            );
    }

    MapElements<Tuple2<FileWithMeta, List<Try<SupplyDetail>>>, Tuple2<FileWithMeta, List<Try<JsonNode>>>> dtoToJson() {
        return MapElements
            .into(new TypeDescriptor<Tuple2<FileWithMeta, List<Try<JsonNode>>>>() {
            })
            .via(tuple2 -> tuple2
                .map2(maybeDto -> {
                    val res = new ArrayList<Try<JsonNode>>();
                    maybeDto.forEach(dto -> {
                        val item = dto.map(JsonUtils::toJsonNode)
                            .mapFailure(
                                Case($(e -> !(e instanceof ProcessingException)),
                                    exc -> new ProcessingException(OBJECT_TO_JSON_CONVERSION_ERROR, exc))
                            );
                        res.add(item);
                    });
                    return res;
                }));
    }

    MapElements<Tuple2<FileWithMeta, List<Try<JsonNode>>>, Tuple2<FileWithMeta, List<Try<JsonNode>>>> toFinalJson(String syncType) {
        return MapElements
            .into(new TypeDescriptor<Tuple2<FileWithMeta, List<Try<JsonNode>>>>() {
            })
            .via(tuple2 -> tuple2
                .map2(jsonNodes -> {
                    val res = new ArrayList<Try<JsonNode>>();
                    val supplyDetails = new ArrayList<JsonNode>();
                    jsonNodes.forEach(jsonNodeTry -> {
                        if (jsonNodeTry.isFailure()) {
                            res.add(jsonNodeTry);
                        }

                        jsonNodeTry.map(jsonNode -> supplyDetails.add(jsonNode))
                            .mapFailure(
                                Case($(e -> !(e instanceof ProcessingException)),
                                    exc -> new ProcessingException(OBJECT_TO_JSON_CONVERSION_ERROR, exc))
                            );
                    });

                    if (!syncType.equals("SyncDetail")) {
                        val payload = JsonUtils.serializeObject(supplyDetails);
                        val supplyDetailsPayload = JsonUtils.deserialize(payload);
                        val finalJson = Try.of(() -> toJsonWithAttributes(supplyDetailsPayload, syncType, tuple2._1.getSourceName()));
                        res.add(finalJson);

                    } else {
                        val batches = Lists.partition(supplyDetails, batchSize);

                        for (List<JsonNode> json : batches) {
                            val payload = JsonUtils.serializeObject(json);
                            val supplyDetailsPayload = JsonUtils.deserialize(payload);
                            val finalJson = Try.of(() -> toJsonWithAttributes(supplyDetailsPayload, syncType, tuple2._1.getSourceName()));
                            res.add(finalJson);
                        }
                    }
                    return res;
                }));
    }

    static ParDo.SingleOutput<FileWithMeta, Tuple2<FileWithMeta, KV<Integer, CSVRecord>>> readFileRecords() {
        return ParDo.of(
            new DoFn<FileWithMeta, Tuple2<FileWithMeta, KV<Integer, CSVRecord>>>() {
                @ProcessElement
                public void processElement(@Element FileWithMeta fileWithMeta, DoFn.OutputReceiver<Tuple2<FileWithMeta, KV<Integer, CSVRecord>>> receiver) {
                    val csvRows = fileWithMeta.getRecords();
                    for (KV<Integer, CSVRecord> row : csvRows) {
                        receiver.output(new Tuple2<>(fileWithMeta, row));
                    }
                }
            }
        );
    }

    static ParDo.SingleOutput<Tuple2<FileWithMeta, Try<SupplyDetail>>, KV<String, Tuple2<FileWithMeta, Try<SupplyDetail>>>> toKV() {
        return ParDo.of(
            new DoFn<Tuple2<FileWithMeta, Try<SupplyDetail>>, KV<String, Tuple2<FileWithMeta, Try<SupplyDetail>>>>() {
                @ProcessElement
                public void processElement(ProcessContext c) {
                    val input = c.element();
                    c.output(KV.of(input._1.getSourceName(), new Tuple2<>(input._1, input._2)));
                }
            }
        );
    }

    private SupplyDetail toSupplyDetailDto(Map<String, String> csvRow) {
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

    private JsonNode toJsonWithAttributes(JsonNode payload, String type, String filename) {
        val fileWithoutExt = filename.split("\\.(?=[^\\.]+$)")[0];
        val fileSplitted = fileWithoutExt.split("_");
        val locationId = fileSplitted[fileSplitted.length - 1];
        val organization = fileSplitted[0];

        val attributes = new LinkedHashMap<String, String>(2);
        attributes.put("User", user);
        attributes.put("Organization", organization);

        val message = new LinkedHashMap<String, Object>(2);
        message.put("attributes", attributes);

        val transactionNumber = locationId + getDatetime("yyyymmddhhmss");

        val syncDTO = toSupplyEventDto(type, payload, transactionNumber, locationId);
        val syncSupplyEvent = getSyncSupplyEventJson(type, syncDTO, payload);

        val event = new LinkedHashMap<String, Object>(2);
        event.put("SyncSupplyEvent", syncSupplyEvent);

        message.put("data", event);

        val resPayload = new HashMap<String, List<Map<String, Object>>>(1);
        resPayload.put("messages", singletonList(message));

        return JsonUtils.toJsonNode(resPayload);
    }

    private JsonNode getSyncSupplyEventJson(String type, SyncSupplyEvent dto, JsonNode payload) {
        val json = JsonUtils.toJsonNode(dto);
        ObjectNode objectNode = (ObjectNode) json;
        if (type.equals("SyncDetail")) {
            objectNode.set("SupplyDetails", payload);
        } else {
            objectNode.remove("SupplyDetails");
        }
        return objectNode;
    }

    private SyncSupplyEvent toSupplyEventDto(String type, JsonNode payload, String transactionNumber, String locationId) {
        val supplyEvent = new SyncSupplyEvent();
        supplyEvent.setTransactionNumber(transactionNumber);
        supplyEvent.setTransactionTypeId(type);
        supplyEvent.setLocationId(locationId);
        supplyEvent.setSyncTransactionDate(getDatetime("yyyy-MM-dd"));
        supplyEvent.setFullSync(true);
        supplyEvent.setCountRecords(Integer.toString(payload.size()));
        return supplyEvent;
    }

    private String getDatetime(String format) {
        val localDateTime = LocalDateTime.now(ZoneOffset.UTC);
        val formatter = DateTimeFormatter.ofPattern(format);
        return localDateTime.format(formatter);
    }
}

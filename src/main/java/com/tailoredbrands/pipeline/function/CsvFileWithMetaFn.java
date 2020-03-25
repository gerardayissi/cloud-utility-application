package com.tailoredbrands.pipeline.function;

import com.tailoredbrands.util.FileWithMeta;
import io.vavr.collection.List;
import org.apache.beam.sdk.io.FileIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.Channels;
import java.util.Map;

public class CsvFileWithMetaFn extends DoFn<FileIO.ReadableFile, FileWithMeta> {

    @ProcessElement
    public void process(@Element FileIO.ReadableFile element, DoFn.OutputReceiver<FileWithMeta> receiver) throws IOException {
        List<Map<String, String>> csvRecords = List.empty();
        String filename = element.getMetadata().resourceId().getFilename();
        String fileContent = element.readFullyAsUTF8String();
        InputStream is = Channels.newInputStream(element.open());
        InputStreamReader reader = new InputStreamReader(is);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT
            .withFirstRecordAsHeader()
            .withDelimiter(',')
            .parse(reader);
        for (CSVRecord record : records) {
            csvRecords.append(record.toMap());
        }
        receiver.output(FileWithMeta.of(filename, fileContent, csvRecords));
        reader.close();
    }
}

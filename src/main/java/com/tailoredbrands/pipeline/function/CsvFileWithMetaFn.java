package com.tailoredbrands.pipeline.function;

import com.tailoredbrands.util.FileWithMeta;
import org.apache.beam.sdk.io.FileIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.List;

public class CsvFileWithMetaFn extends DoFn<FileIO.ReadableFile, FileWithMeta> {

    @ProcessElement
    public void process(@Element FileIO.ReadableFile element, DoFn.OutputReceiver<FileWithMeta> receiver) throws IOException {
        List<KV<Integer, CSVRecord>> csvRecords = new ArrayList<>();
        String filename = element.getMetadata().resourceId().getFilename();
        String fileContent = element.readFullyAsUTF8String();
        InputStream is = Channels.newInputStream(element.open());
        InputStreamReader reader = new InputStreamReader(is);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT
            .withFirstRecordAsHeader()
            .withDelimiter(',')
            .parse(reader);
        int recordIdx = 0;
        for (CSVRecord record : records) {
            recordIdx++;
            csvRecords.add(KV.of(recordIdx, record));
        }
        receiver.output(FileWithMeta.of(filename, fileContent, csvRecords));
        reader.close();
    }
}

package com.tailoredbrands.pipeline.function;

import com.tailoredbrands.util.FileRowMetadata;
import org.apache.beam.sdk.io.FileIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.Channels;

public class CsvFileWithMetadataFn extends DoFn<FileIO.ReadableFile, FileRowMetadata> {

    @ProcessElement
    public void process(@Element FileIO.ReadableFile element, DoFn.OutputReceiver<FileRowMetadata> receiver) throws IOException {
        String filename = element.getMetadata().resourceId().getFilename();
        InputStream is = Channels.newInputStream(element.open());
        InputStreamReader reader = new InputStreamReader(is);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT
            .withFirstRecordAsHeader()
            .withDelimiter(',')
            .parse(reader);
        for (CSVRecord record : records) {
            receiver.output(FileRowMetadata.of(filename, record.toMap()));
        }
        reader.close();
    }
}

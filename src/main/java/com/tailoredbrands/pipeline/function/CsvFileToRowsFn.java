package com.tailoredbrands.pipeline.function;

import org.apache.beam.sdk.io.FileIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.Channels;
import java.util.Map;

public class CsvFileToRowsFn extends DoFn<FileIO.ReadableFile, Map<String, String>> {

    private final char delimiter;

    public CsvFileToRowsFn(String delimiter) {
        if (delimiter.length() > 1) {
            throw new IllegalArgumentException("Delimiter length exceedes 1 char: " + delimiter);
        }
        this.delimiter = delimiter.charAt(0);
    }

    @ProcessElement
    public void process(@Element FileIO.ReadableFile element, DoFn.OutputReceiver<Map<String, String>> receiver) throws IOException {
        InputStream is = Channels.newInputStream(element.open());
        InputStreamReader reader = new InputStreamReader(is);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withDelimiter(delimiter)
                .parse(reader);
        for (CSVRecord record : records) {
            receiver.output(record.toMap());
        }
        reader.close();
    }
}

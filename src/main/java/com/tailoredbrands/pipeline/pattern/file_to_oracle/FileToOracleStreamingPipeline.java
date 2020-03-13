package com.tailoredbrands.pipeline.pattern.file_to_oracle;

import com.tailoredbrands.pipeline.options.FileToOracleOptions;
import com.tailoredbrands.pipeline.pattern.jms_to_pub_sub.JmsToPubSubCounter;
import com.tailoredbrands.pipeline.pattern.pub_sub_to_oracle.PubSubToOracleStreamingPipeline;
import org.apache.beam.runners.dataflow.DataflowRunner;
import org.apache.beam.runners.direct.DirectRunner;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

import static com.tailoredbrands.util.Peek.increment;

public class FileToOracleStreamingPipeline {
    private static final Logger LOG = LoggerFactory.getLogger(PubSubToOracleStreamingPipeline.class);

    public static void main(String[] args) {
        FileToOracleOptions options = PipelineOptionsFactory
                .fromArgs(args)
                .as(FileToOracleOptions.class);

        options.setRunner(DirectRunner.class);
        options.setRunner(DataflowRunner.class);


        options.setTempLocation("gs://tst1-integration-3ca6/temp/");
        options.setJobName("GAL-file-oracle-performance");
        options.setRegion("us-east1");
        options.setProject("tst1-integration-3ca6");
        options.setServiceAccount("project-service-account@tst1-integration-3ca6.iam.gserviceaccount.com");
        options.setNetwork("shared-vpc");
        options.setSubnetwork("https://www.googleapis.com/compute/v1/projects/network-b2b9/regions/us-east1/subnetworks/np-integration4");
        options.setNumWorkers(1);
        options.setMaxNumWorkers(1);

        options.setInputFilePattern("gs://tst1-integration-3ca6/temp/sudoku.csv");
        options.setDriver("oracle.jdbc.OracleDriver");
        options.setUrl("jdbc:oracle:thin:@//ws7tstdb601.tmw.com:2414/gentest");
        options.setTable("XAO_SUDOKU");
        options.setUser("wcadmin");
        options.setPassword("wcadmin7gentest");

        run(options);
    }

    public static PipelineResult run(FileToOracleOptions options) {

        final JmsToPubSubCounter counter = new JmsToPubSubCounter("counter");
        final Pipeline pipeline = Pipeline.create(options);
        pipeline
                .apply("Read from File", TextIO.read().from(options.getInputFilePattern()))
                .apply("Records Read", increment(counter.jmsRecordsRead))
                .apply("Mapping", mapToObject())
                .apply("Write to Oracle", writeToOracle(options));

        return pipeline.run();
    }

    private static ParDo.SingleOutput<String, Sudoku> mapToObject() {
        return ParDo.of(new DoFn<String, Sudoku>() {
            private static final long serialVersionUID = 1L;
            private static final String header = "puzzle,solution";

            @ProcessElement
            public void processElement(ProcessContext c) {
                final String row = c.element();

                if (row.equals(header)) return;

                final String[] line = row.split(",");

                final Sudoku object = new Sudoku();
                object.puzzle = line[0];
                object.solution = line[1];

                c.output(object);
            }
        });
    }

    private static JdbcIO.Write<Sudoku> writeToOracle(FileToOracleOptions options) {
        return JdbcIO.<Sudoku>write()
                .withDataSourceConfiguration(JdbcIO
                        .DataSourceConfiguration.create(
                                options.getDriver(),
                                options.getUrl())
                        .withUsername(options.getUser())
                        .withPassword(options.getPassword())
                )
                .withStatement(String.format("insert into %s values(?, ?)", options.getTable()))
                .withPreparedStatementSetter((JdbcIO.PreparedStatementSetter<Sudoku>)
                        (object, query) -> {
                            query.setString(1, object.puzzle);
                            query.setString(2, object.solution);
                        });
    }

    public static class Sudoku implements Serializable {
        String puzzle;
        String solution;
    }
}

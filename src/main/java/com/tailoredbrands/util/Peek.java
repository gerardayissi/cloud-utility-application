package com.tailoredbrands.util;

import com.tailoredbrands.util.func.SerializableConsumer;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;

/**
 * An intermediate transformer which allows applying a side-effectful operation for every element
 *
 * @param <T> element type
 */
public class Peek<T> extends PTransform<PCollection<T>, PCollection<T>> {

    private final SerializableConsumer<T> action;

    private Peek(SerializableConsumer<T> f) {
        this.action = f;
    }

    public static <T> Peek<T> each(SerializableConsumer<T> action) {
        return new Peek<>(action);
    }

    /**
     * Build transform to increment given Counter (if it is not null)
     *
     * @param counter - nullable counter
     * @param <T>     - type of collection elements
     * @return counting peek
     */
    public static <T> Peek<T> increment(Counter counter) {
        return Peek.each(ignored -> {
            if (counter != null) {
                counter.inc();
            }
        });
    }

    @Override
    public PCollection<T> expand(PCollection<T> input) {
        return input
                .apply(
                        ParDo.of(
                                new DoFn<T, T>() {
                                    @ProcessElement
                                    public void processElement(@Element T element, OutputReceiver<T> r) {
                                        action.accept(element);
                                        r.output(element);
                                    }
                                }))
                .setCoder(input.getCoder());
    }
}

package com.tailoredbrands.util.predef;


import com.tailoredbrands.util.func.SerializableRunnableE;

import java.util.concurrent.Callable;

public class Exceptions {

    public static <A> A exceptionally(Callable<A> f) {
        try {
            return f.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void exceptionally(SerializableRunnableE f) {
        try {
            f.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

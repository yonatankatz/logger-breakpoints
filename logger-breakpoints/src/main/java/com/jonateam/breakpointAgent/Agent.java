package com.jonateam.breakpointAgent;

import java.lang.instrument.Instrumentation;

/**
 * Created by yonatan.katz on 12/19/2018.
 */
public class Agent {

    public static void premain(String args, Instrumentation instrumentation){
        LoggerTransformer transformer = new LoggerTransformer();
        instrumentation.addTransformer(transformer);
    }
}

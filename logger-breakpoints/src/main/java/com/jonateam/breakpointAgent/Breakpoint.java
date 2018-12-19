package com.jonateam.breakpointAgent;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import javax.management.JMX;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by yonatan.katz on 12/19/2018.
 */
public class Breakpoint {

    private static ThreadLocal<Object[]> stackArguments = new ThreadLocal<>();

    public static void storeStack(Object[] args) {
        if (!Config.isEnabled()) {
            return;
        }
        stackArguments.set(args);
    }

    public static void freeStack() {
        stackArguments.set(null);
    }

    public static void breakIt(Object[] args) {
        if (!Config.isEnabled()) {
            return;
        }
        Map breakpoint = (Map) Config.getPropertiesAsTree().get("breakpoint");
        if (breakpoint == null) {
            return;
        }
        for (Object breakpointsInstance : breakpoint.values()) {
            String search = (String)((Map) breakpointsInstance).get("search");
            String invoke = (String)((Map) breakpointsInstance).get("invoke");
            if (search == null || search.isEmpty() || invoke == null || invoke.isEmpty()) {
                return;
            }
            handleBreakpoint(args, search, invoke);
        }
    }

    private static void handleBreakpoint(Object[] args, String search, String invoke) {
        String[] invokeOptions = invoke.split(",");
        for (Object arg : args) {
            if (arg.toString().contains(search)) {
                for (String invokeOption : invokeOptions) {
                    invokeOperation(invokeOption.trim());
                }
            }
        }

    }

    private static void invokeOperation(String invokeOption) {
        if (invokeOption.toLowerCase().equals("stack")) {
            try {
                throw new RuntimeException("Current Stack:");
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        else if (invokeOption.toLowerCase().equals("threaddump")) {
            try {
                System.out.println(threadDump());
            } catch(Throwable e) {
                throw new AssertionError(e);
            }
        }
        else if (invokeOption.toLowerCase().equals("heap")) {
            try {
                System.out.println(heapStatus());
            } catch(Throwable e) {
                throw new AssertionError(e);
            }
        }
        else if (invokeOption.toLowerCase().equals("args")) {
            try {
                System.out.println(Arrays.toString(stackArguments.get()));
            } catch(Throwable e) {
                throw new AssertionError(e);
            }
        }
    }

    private static String heapStatus() {
        int mb = 1024*1024;
        String heap = "";
        //Getting the runtime reference from system
        Runtime runtime = Runtime.getRuntime();

        //Print used memory
        heap += "Used Memory:"
                + ((runtime.totalMemory() - runtime.freeMemory()) / mb) + " MB\n";

        //Print free memory
        heap += "Free Memory:" + (runtime.freeMemory() / mb)+ " MB\n";

        //Print total available memory
        heap += "Total Memory:" + (runtime.totalMemory() / mb) + " MB\n";

        //Print Maximum available memory
        heap += "Max Memory:" + (runtime.maxMemory() / mb) + " MB\n";
        return heap;
    }

    private static String threadDump() {
        final StringBuilder dump = new StringBuilder();
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);
        for (ThreadInfo threadInfo : threadInfos) {
            dump.append('"');
            dump.append(threadInfo.getThreadName());
            dump.append("\" ");
            final Thread.State state = threadInfo.getThreadState();
            dump.append("\n   java.lang.Thread.State: ");
            dump.append(state);
            final StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
            for (final StackTraceElement stackTraceElement : stackTraceElements) {
                dump.append("\n        at ");
                dump.append(stackTraceElement);
            }
            dump.append("\n\n");
        }
        return dump.toString();
    }

}

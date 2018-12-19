package com.jonateam.breakpointAgent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Created by yonatan.katz on 12/19/2018.
 */
public class Config {

    private static Properties properties = new Properties();
    private static boolean enabled = false;
    private static File configFile = null;
    private static Map configTree = null;

    static class DaemonThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        }
    }

    static {
        load();
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    load();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }, 10, 10, TimeUnit.SECONDS);

    }

    public static Map getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public static Map getPropertiesAsTree() {
        return Collections.unmodifiableMap(configTree);
    }


    private static void load() {
        configFile = new File("./.breakpoint.config");
        if (!configFile.exists()) {
            if (isWindows()) {
                configFile = new File("c:/.breakpoint.config");
            }
            else {
                configFile = new File("/opt/.breakpoint.config");
                if (!configFile.exists()) {
                    configFile = new File("~/.breakpoint.config");
                }

            }
        }
        if (configFile.exists()) {
            try {
                properties.load(new FileInputStream(configFile));
                configTree = getConfigAsTree(properties);
                enabled = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            enabled = false;
        }
    }

    public static boolean isEnabled() {
        return enabled && properties != null && "true".equals(properties.getProperty("enabled"));
    }

    private static String OS = null;
    private static String getOsName()
    {
        if(OS == null) { OS = System.getProperty("os.name"); }
        return OS;
    }
    private static boolean isWindows()
    {
        return getOsName().toLowerCase().startsWith("Windows");
    }

    private static Map<String, Map> getConfigAsTree(Properties p) {
        Map<String, Map> tree = new LinkedHashMap();

        for (String name : p.stringPropertyNames()) {
            String[] parts = name.split("\\.");
            Map nextTree = tree;
            for (int i = 0, partsLength = parts.length; i < partsLength; i++) {
                String part = parts[i];
                Object v = nextTree.get(part);
                if (v == null) {
                    if (i < partsLength - 1) {
                        Map newNextTree = new LinkedHashMap();
                        nextTree.put(part, newNextTree);
                        nextTree = newNextTree;
                    } else {
                        nextTree.put(part, p.getProperty(name));
                    }
                } else {
                    if (i < partsLength - 1) {
                        nextTree = (Map) v;
                    }
                }
            }
        }
        return tree;
    }

}

package com.jonateam.testApp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by yonatan.katz on 12/19/2018.
 */
public class AnotherClass {

    Logger logger = LoggerFactory.getLogger(AnotherClass.class);

    public void doSomething(int param) {
//        logger.info("info: value {} " + param, 12);
//
//        logger.debug("debug: value " + (param + 3));
//
//        logger.error("error: value " + param);
//
//        logger.warn("Here is a log message for Hakathon Dec 2018 ");
//
//        logger.info("SharonBL is the king");

        makeMe("ABC", 12, new File("myFile"));

    }

    private void makeMe(String abc, int i, File myFile) {
        logger.info("all-together");
    }
}

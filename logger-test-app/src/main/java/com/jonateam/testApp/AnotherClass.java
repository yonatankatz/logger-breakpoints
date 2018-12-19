package com.jonateam.testApp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by yonatan.katz on 12/19/2018.
 */
public class AnotherClass {

    Logger logger = LoggerFactory.getLogger(AnotherClass.class);

    public void doSomething(int param) {
        logger.info("info: value " + param);

        logger.debug("debug: value " + param);

        logger.error("error: value " + param);

    }
}

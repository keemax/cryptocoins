package com.keemax.consts;

import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 1/8/14
 * Time: 8:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExchangeProperties {
    private Properties properties;
    private static ExchangeProperties instance = null;

    public ExchangeProperties() {
        properties = new Properties();
        try {
            properties.load(ExchangeProperties.class.getClassLoader().getResourceAsStream("config.properties"));
        } catch(Exception e) {
            System.err.println("Error loading properties file");
            e.printStackTrace();
        }
    }

    public static ExchangeProperties getInstance() {
        if (instance == null) {
            return new ExchangeProperties();
        }
        return instance;
    }

    public String get(String prop) {
        return properties.getProperty(prop);
    }
}

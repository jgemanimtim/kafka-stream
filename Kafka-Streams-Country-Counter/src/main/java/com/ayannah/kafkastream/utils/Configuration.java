package com.ayannah.kafkastream.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;


/**
 * 
 * @author josephgile.manimtim
 *
 */
public class Configuration {

	private Environment environment;
    private Map<String, String> configuration;

    public Configuration(String env) {
        if("production".equals(env)) {
            environment = Environment.PRODUCTION;
        } else if("test".equals(env)) {
            environment = Environment.TEST;
        } else {
            throw new RuntimeException("Invalid environment!");
        }

        loadConfiguration(environment);
        setupLogger(environment);
    }

    public Map<String, String> getConfigurationMap() {
        return this.configuration;
    }

    public Environment getEnvironment() {
        return this.environment;
    }

    public String getConfiguration(String key) {
        return this.configuration.get(key);
    }

    protected void loadConfiguration(Environment environment) {
        try {
            Properties configFile = new Properties();
            configFile.load(Configuration.class.getResourceAsStream("/settings."+environment.getLabel()+".properties"));
            this.configuration = new HashMap<String, String>((Map) configFile);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    protected void setupLogger(Environment environment) {
        String log4jConfiguration = "/log4j.properties";
        if (environment == Environment.PRODUCTION) {
            log4jConfiguration = "/log4j.prod.properties";
        }

        
        Properties props = new Properties();
        try {
			props.load(Configuration.class.getResourceAsStream(log4jConfiguration));
		} catch (IOException e) {
			System.out.println("Failed to load log4jconfiguration");
		}
        
        
        PropertyConfigurator.configure(props);
    }

    public enum Environment {
        TEST("test"),
        PRODUCTION("prod");

        public String label;
        Environment(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
	
}

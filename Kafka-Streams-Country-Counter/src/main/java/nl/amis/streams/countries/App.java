package nl.amis.streams.countries;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.ForeachAction;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.processor.WallclockTimestampExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ayannah.bigquery.BigQueryDao;
import com.ayannah.kafkastream.utils.Configuration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import nl.amis.streams.JsonPOJODeserializer;
import nl.amis.streams.JsonPOJOSerializer;




public class App 
{
    
	static public class CountryMessage {
        /* the JSON messages produced to the countries Topic have this structure:
         { "name" : "The Netherlands"
         , "code" : "NL
         , "continent" : "Europe"
         , "population" : 17281811
         , "size" : 42001
         };
   
        this class needs to have at least the corresponding fields to deserialize the JSON messages into
        */
 
        public String code;
        public String name;
        public int population;
        public int size;
        public String continent;
    }
	
	private static final String APP_ID = "countries-streaming-analysis-app";
	
	private static Configuration configuration = new Configuration(System.getProperty("environment", "test"));
	private static Logger logger = LoggerFactory.getLogger(App.class);
	
	public static void main( String[] args )
    {
		logger.info("Kafka Streams Demonstration");
		logger.info("Environment is "+configuration.getEnvironment().getLabel());
		
		logger.debug("Kafka Host: " + configuration.getConfiguration("kafka.host"));
		logger.debug("Kafka Port: " + configuration.getConfiguration("kafka.port"));
		
        // Create an instance of StreamsConfig from the Properties instance
        StreamsConfig config = new StreamsConfig(getProperties());
        final Serde < String > stringSerde = Serdes.String();
        final Serde < Long > longSerde = Serdes.Long();
 
        // define countryMessageSerde
        Map < String, Object > serdeProps = new HashMap < > ();
        final Serializer < CountryMessage > countryMessageSerializer = new JsonPOJOSerializer < > ();
        serdeProps.put("JsonPOJOClass", CountryMessage.class);
        countryMessageSerializer.configure(serdeProps, false);
 
        final Deserializer < CountryMessage > countryMessageDeserializer = new JsonPOJODeserializer < > ();
        serdeProps.put("JsonPOJOClass", CountryMessage.class);
        countryMessageDeserializer.configure(serdeProps, false);
        final Serde < CountryMessage > countryMessageSerde = Serdes.serdeFrom(countryMessageSerializer, countryMessageDeserializer);
 
        // building Kafka Streams Model
        KStreamBuilder kStreamBuilder = new KStreamBuilder();
        // the source of the streaming analysis is the topic with country messages
        KStream<String, CountryMessage> countriesStream = 
                                       kStreamBuilder.stream(stringSerde, countryMessageSerde, "countries1");

        
        countriesStream.foreach(new ForeachAction<String, CountryMessage>() {
            
			public void apply(String arg0, CountryMessage countryMessage) {
				System.out.println(countryMessage.name);
				System.out.println(countryMessage.code);
				System.out.println(countryMessage.population);
				System.out.println(countryMessage.size);
				
				ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
				try {
					String json = ow.writeValueAsString(countryMessage);
					System.out.println(json);
					BigQueryDao bigQueryDao = new BigQueryDao(configuration);
					bigQueryDao.insert(json);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
         });
        // THIS IS THE CORE OF THE STREAMING ANALYTICS:
        // running count of countries per continent, published in topic RunningCountryCountPerContinent
        KTable<String,Long> runningCountriesCountPerContinent = countriesStream
        														    .selectKey((k, country) -> country.continent)
                                                                 .countByKey("Counts");
        
        runningCountriesCountPerContinent.to(stringSerde, longSerde,  "RunningCountryCountPerContinent");
        runningCountriesCountPerContinent.print(stringSerde, longSerde);
 
        
        
 
 
        logger.info("Starting Kafka Streams Countries Example");
        KafkaStreams kafkaStreams = new KafkaStreams(kStreamBuilder, config);
        kafkaStreams.start();
        logger.info("Now started CountriesStreams Example");
    }
	
	
	
	private static Properties getProperties() {
        Properties settings = new Properties();
        // Set a few key parameters
        settings.put(StreamsConfig.APPLICATION_ID_CONFIG, APP_ID);
        // Kafka bootstrap server (broker to talk to); ubuntu is the host name for my VM running Kafka, port 9092 is where the (single) broker listens 
        settings.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.getConfiguration("kafka.host") + ":" +
        		configuration.getConfiguration("kafka.port"));
        // Apache ZooKeeper instance keeping watch over the Kafka cluster; ubuntu is the host name for my VM running Kafka, port 2181 is where the ZooKeeper listens 
        settings.put(StreamsConfig.ZOOKEEPER_CONNECT_CONFIG, configuration.getConfiguration("zookeeper.host") + ":" +
        		configuration.getConfiguration("zookeeper.port"));
        // default serdes for serialzing and deserializing key and value from and to streams in case no specific Serde is specified
        settings.put(StreamsConfig.KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        settings.put(StreamsConfig.VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        settings.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/");
        // to work around exception Exception in thread "StreamThread-1" java.lang.IllegalArgumentException: Invalid timestamp -1
        // at org.apache.kafka.clients.producer.ProducerRecord.<init>(ProducerRecord.java:60)
        // see: https://groups.google.com/forum/#!topic/confluent-platform/5oT0GRztPBo
        settings.put(StreamsConfig.TIMESTAMP_EXTRACTOR_CLASS_CONFIG, WallclockTimestampExtractor.class);
        return settings;
    }
}

package com.ayannah.bigquery;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;

import com.ayannah.kafkastream.utils.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryError;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.TableId;

public class BigQueryDao {

	private Configuration configuration;
	
 	public BigQueryDao(Configuration configuration) {
		super();
		this.configuration = configuration;
	}



	
	
	
	
	public void insert(String json) {
		BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();
		
		
		TableId tableId = TableId.of(configuration.getConfiguration("bigquery.dataset"), 
				configuration.getConfiguration("bigquery.table"));
		
		try {
		  HashMap<String,Object> mapResult = new ObjectMapper().readValue(json, HashMap.class);
		  InsertAllResponse response = bigquery.insertAll(InsertAllRequest.newBuilder(tableId)
		    .addRow(UUID.randomUUID().toString(), mapResult)
		    .build());
		  if (response.hasErrors()) {
		    // If any of the insertions failed, this lets you inspect the errors
		    for (Entry<Long, List<BigQueryError>> entry : response.getInsertErrors().entrySet()) {
		      System.out.println(entry);
		    }
		  }
		} catch (IOException e) {
			  // Failed to Map JSON String
			  System.out.println(e);
		}
	}



	public Configuration getConfiguration() {
		return configuration;
	}



	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}
	
}

package edu.gatech.chai.omoponfhir.omopv5.dstu2.config;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.gatech.chai.omopv5.jpa.dao.DatabaseConfiguration;

public class LocalConfig {
    static final Logger logger = LoggerFactory.getLogger(LocalConfig.class);

	@Autowired
	DatabaseConfiguration databaseConfiguration;

	public int databaseConfiguration(String targetDatabase, String...parameters) {
        if (targetDatabase == null || targetDatabase.isEmpty()) {
            targetDatabase = "postgresql";
        }

		// What driver do we want to use?
		databaseConfiguration.setSqlRenderTargetDialect(targetDatabase);

		if ("bigquery".equalsIgnoreCase(targetDatabase)) {
            if (parameters.length == 2) {
                databaseConfiguration.setBigQueryDataset(parameters[0]);
                databaseConfiguration.setBigQueryProject(parameters[1]);
            } else {
                logger.error("Incorrect number of parameters for BigQuery database configuration");
                return -1;
            }
		} else {
            if (parameters.length == 3) {
                BasicDataSource ds = new BasicDataSource();
                ds.setUrl(parameters[0]);
                ds.setUsername(parameters[1]);
                ds.setPassword(parameters[2]);
                ds.setMinIdle(5);
                ds.setMaxIdle(10);
                ds.setMaxOpenPreparedStatements(100);
                databaseConfiguration.setDataSource(ds);
                databaseConfiguration.setSqlRenderTargetDialect(targetDatabase);
                databaseConfiguration.setDataSource(ds);
            } else {
                logger.error("Incorrect number of parameters for SQL database configuration");
                return -1;
            }
		}
    
        return 0;
	}
}

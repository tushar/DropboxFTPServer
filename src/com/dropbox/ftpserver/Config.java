package com.dropbox.ftpserver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {
  
  protected static final String CONFIG_FILE_NAME = "config.properties";
  protected static final String MAX_LOGINS_CONFIG_KEY = "maxLogins";
  protected static final String MAX_FTP_WORKER_THREADS = "maxFTPWorkerThreads";
  protected static final String NUM_PUT_THREADS = "numPutThreads";
  protected static final String NUM_GET_THREADS = "numGetThreads";
  protected static final String FILE_SIZE = "fileSizeInBytes";
  protected static final String PORT = "port";
  
  protected static final Integer DEFAULT_MAX_LOGINS = 10000;
  protected static final Integer DEFAULT_MAX_FTP_WORKER_THREADS = 16;
  protected static final Integer DEFAULT_NUM_PUT_THREADS = 32;
  protected static final Integer DEFAULT_NUM_GET_THREADS = 32;
  protected static final Integer DEFAULT_FILE_SIZE_IN_BYTES = 1024*1024; // 1MB
  protected static final Integer DEFAULT_PORT = 2221;
  
  protected static Config instance;
  
  protected final Properties properties;
  private final Logger LOG = LoggerFactory.getLogger(Config.class);
  
  public static synchronized Config getInstance() throws IOException {
    if(instance != null) {
      return instance;
    }
    instance = new Config();
    return instance;
  }
  
  public int getMaxLogins() {
    return Integer.valueOf((String)properties.get(MAX_LOGINS_CONFIG_KEY));
  }
  
  public int getMaxFtpWorkerThreads() {
    return Integer.valueOf((String)properties.get(MAX_FTP_WORKER_THREADS));
  }
  
  public int getNumPutThreads() {
    return Integer.valueOf((String)properties.get(NUM_PUT_THREADS));
  }
  
  public int getNumGetThreads() {
    return Integer.valueOf((String)properties.get(NUM_GET_THREADS));
  }
  
  public int getFileSize() {
    return Integer.valueOf((String)properties.getProperty(FILE_SIZE));
  }
  
  public int getPort() {
    return Integer.valueOf((String)properties.getProperty(PORT));
  }
  
  protected Config() throws IOException {
    properties = new Properties();
    createDefaultConfig();
    loadConfig();
    storeConfig();
  }
  
  protected boolean loadConfig() {
    try {
      FileInputStream is = new FileInputStream(CONFIG_FILE_NAME);
      properties.load(is);
      return true;
    }  catch (IOException e) {
      LOG.info("Failed to file config file: " + CONFIG_FILE_NAME,e);
    }
    return false;
  }
  
  protected void createDefaultConfig() {
    properties.setProperty(MAX_LOGINS_CONFIG_KEY, DEFAULT_MAX_LOGINS.toString());
    properties.setProperty(MAX_FTP_WORKER_THREADS, DEFAULT_MAX_FTP_WORKER_THREADS.toString());
    properties.setProperty(NUM_GET_THREADS, DEFAULT_NUM_GET_THREADS.toString());
    properties.setProperty(NUM_PUT_THREADS, DEFAULT_NUM_PUT_THREADS.toString());
    properties.setProperty(FILE_SIZE, DEFAULT_FILE_SIZE_IN_BYTES.toString());    
    properties.setProperty(PORT, DEFAULT_PORT.toString());
  }
  
  protected void storeConfig() throws IOException {
    properties.store(new FileOutputStream(CONFIG_FILE_NAME), null);
  }
}

package com.dropbox.ftpserver;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.apache.ftpserver.ftplet.User;

import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.ftpserver.auth.DropboxUser;

/**
 * A singleton object to hold various entities needed by different parts of the code. 
 * 
 * We don't use the traditional pattern of lazy singleton initialization here. 
 * The assumption is that at program startup point, createInstance is called to setup the 
 * instance correctly. Only after that is getInstance called by different components. 
 * 
 */
public class DropboxContext {

  protected final AppKeyPair appKeyPair;
  protected final Map<String, User> usernameMap = new ConcurrentHashMap<String, User>();
  protected final Executor dropboxPutExecutor;  
  protected final Executor ftpSTORExecutor;
  protected final Executor ftpRETRExecutor;
  
  protected static DropboxContext instance;
  
  public static DropboxContext getInstance() {
    return instance;
  }
  
  public static DropboxContext createInstance(String key, String secret, 
      Executor dropboxPutExecutor, Executor ftpSTORExecutor, Executor ftpRETRExecutor) {    
    instance = new DropboxContext(key, secret, dropboxPutExecutor, 
        ftpSTORExecutor, ftpRETRExecutor);
    return instance;
  }
  
  public AppKeyPair getAppKeyPair() {
    return appKeyPair;
  }
  
  public void addUser(String username, String dropboxUserId, AccessTokenPair accessTokenPair) {
    User user = new DropboxUser(username, dropboxUserId, accessTokenPair);
    usernameMap.put(username, user);
  }
  
  public User getUser(String username) {
    return usernameMap.get(username);
  }
  
  public Set<String> getAllUsernames() {
    return usernameMap.keySet();
  }
     
  public Executor getDropboxPutExecutor() {
    return dropboxPutExecutor;
  }
    
  public Executor getFtpSTORExecutor() {
    return ftpSTORExecutor;
  }
  
  public Executor getFtpRETRExecutor() {
    return ftpRETRExecutor;
  }
  
  protected DropboxContext(String key, String secret, 
      Executor dropboxPutExecutor, Executor ftpSTORExecutor, Executor ftpRETRExecutor) {
    appKeyPair = new AppKeyPair(key, secret);
    this.dropboxPutExecutor = dropboxPutExecutor;
    this.ftpSTORExecutor = ftpSTORExecutor;
    this.ftpRETRExecutor = ftpRETRExecutor;
  }
  
}

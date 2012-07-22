package com.dropbox.ftpserver;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.Session;
import com.dropbox.client2.session.WebAuthSession;


public class Utils {

  public static WebAuthSession getNewWAS() throws DropboxException {
    DropboxContext dropboxContext = DropboxContext.getInstance();
    WebAuthSession was = 
      new WebAuthSession(dropboxContext.getAppKeyPair(), Session.AccessType.APP_FOLDER);
    return was;
  }
  
  public static WebAuthSession getNewWAS(AccessTokenPair accessToken) {
    DropboxContext dropboxContext = DropboxContext.getInstance();
    WebAuthSession was = 
      new WebAuthSession(dropboxContext.getAppKeyPair(), Session.AccessType.APP_FOLDER, accessToken);
    return was;    
  }
  
}

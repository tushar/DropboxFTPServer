package com.dropbox.ftpserver.auth;
import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;

import com.dropbox.ftpserver.DropboxContext;

/**
 * Implementation of the FTP User Manager interface that authenticates Dropbox users. 
 * Only the essential methods needed for this program have been implemented. 
 * 
 * An FTP user is considered authenticated, if in he was setup in the authentication flow at startup. 
 */
public class DropboxUserManager implements UserManager {
     
  @Override
  public User authenticate(Authentication authentication) throws AuthenticationFailedException {
    DropboxContext dropboxContext = DropboxContext.getInstance();
    if(authentication instanceof UsernamePasswordAuthentication) {
      UsernamePasswordAuthentication upauth = (UsernamePasswordAuthentication) authentication;
      String username = upauth.getUsername();
      User user = dropboxContext.getUser(username);
      if(user == null) {
        throw new AuthenticationFailedException("Authentication failed. Given username does not exist");
      }
      return user;
    }
    return null;
  }

  @Override
  public void delete(String username) throws FtpException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean doesExist(String username) throws FtpException {
    
    return DropboxContext.getInstance().getUser(username) != null;
  }

  @Override
  public String getAdminName() throws FtpException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String[] getAllUserNames() throws FtpException {
    DropboxContext dropboxContext = DropboxContext.getInstance();    
    return dropboxContext.getAllUsernames().toArray(new String[0]);
  }

  @Override
  public User getUserByName(String username) throws FtpException {
    DropboxContext dropboxContext = DropboxContext.getInstance();
    return dropboxContext.getUser(username);
  }

  @Override
  public boolean isAdmin(String username) throws FtpException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void save(User username) throws FtpException {
    // TODO Auto-generated method stub
    
  }

  
  
}

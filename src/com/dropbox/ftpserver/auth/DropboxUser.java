package com.dropbox.ftpserver.auth;
import java.util.List;

import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.AuthorizationRequest;
import org.apache.ftpserver.ftplet.User;

import com.dropbox.client2.session.AccessTokenPair;

/**
 * 
 */

/**
 *
 */
public class DropboxUser implements User {

  private final String username;
  private final String dropboxUserId;
  private final AccessTokenPair accessTokenPair;
  private final int hashCode; 
  
  public DropboxUser(String username, String dropboxUserId, AccessTokenPair accessTokenPair) {
    this.username = username;
    this.dropboxUserId = dropboxUserId;
    this.accessTokenPair = accessTokenPair;
    hashCode = computeHashCode();
  }
  
  public AccessTokenPair getAccessTokenPair() {
    return accessTokenPair;
  }
  
  /* (non-Javadoc)
   * @see org.apache.ftpserver.ftplet.User#authorize(org.apache.ftpserver.ftplet.AuthorizationRequest)
   */
  @Override
  public AuthorizationRequest authorize(AuthorizationRequest request) {
    return request;
  }

  /* (non-Javadoc)
   * @see org.apache.ftpserver.ftplet.User#getAuthorities()
   */
  @Override
  public List<Authority> getAuthorities() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.apache.ftpserver.ftplet.User#getAuthorities(java.lang.Class)
   */
  @Override
  public List<Authority> getAuthorities(Class<? extends Authority> clazz) {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.apache.ftpserver.ftplet.User#getEnabled()
   */
  @Override
  public boolean getEnabled() {
    // TODO Auto-generated method stub
    return true;
  }

  /* (non-Javadoc)
   * @see org.apache.ftpserver.ftplet.User#getHomeDirectory()
   */
  @Override
  public String getHomeDirectory() {
    return "Apps/TusharFTPServer";
  }

  /* (non-Javadoc)
   * @see org.apache.ftpserver.ftplet.User#getMaxIdleTime()
   */
  @Override
  public int getMaxIdleTime() {
    // TODO Auto-generated method stub
    return 0;
  }

  /* (non-Javadoc)
   * @see org.apache.ftpserver.ftplet.User#getName()
   */
  @Override
  public String getName() {
    return username;
  }

  /* (non-Javadoc)
   * @see org.apache.ftpserver.ftplet.User#getPassword()
   */
  @Override
  public String getPassword() {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean equals(Object other) {
    if(!(other instanceof DropboxUser)) {
      return false;
    }
    DropboxUser otherUser = (DropboxUser) other;
    return username.equals(otherUser.username) && 
           dropboxUserId.equals(otherUser.dropboxUserId) &&
           accessTokenPair.equals(otherUser.accessTokenPair);    
  }
  
  public int hashCode() {
    return hashCode;
  }
  
  private int computeHashCode() {
    int result = 17;
    result = 31 * result + username.hashCode();
    result = 31 * result + dropboxUserId.hashCode();
    result = 31 * result + accessTokenPair.hashCode();
    return result;
  }
  
}

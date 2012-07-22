package com.dropbox.ftpserver.auth;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.WebAuthSession;
import com.dropbox.ftpserver.DropboxContext;
import com.dropbox.ftpserver.Utils;


public class AuthTerminal {

  public void run() {    
    DropboxContext dropboxContext = DropboxContext.getInstance();
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));    
    String username;
    while(true) {
      try {
        System.out.println("Enter username or \\quit: ");        
        username = br.readLine(); 
        
        if(username.equals("\\quit")) {
          System.out.println("Exitting user auth flow. Will be starting FTP server now");
          return;
        }
        
        System.out.println("Got username: " + username);
        
        WebAuthSession was = Utils.getNewWAS();
        WebAuthSession.WebAuthInfo webAuthInfo = was.getAuthInfo();
        System.out.println("1. Go to: " + webAuthInfo.url);
        System.out.println("2. Allow access to this app.");
        System.out.println("3. Press ENTER.");
        br.readLine();
        
        String userId = was.retrieveWebAccessToken(webAuthInfo.requestTokenPair);
        AccessTokenPair accessToken = was.getAccessTokenPair();
        
        dropboxContext.addUser(username, userId, accessToken);
        
        System.out.println("Successfully added user: " + username + " with dropbox user id: " + userId);
        
      } catch (Exception e) {
        System.err.println("Caught exception: " + e);
      }
    }
    
  }
  
}

package com.dropbox.ftpserver.file;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;

import com.dropbox.ftpserver.Utils;
import com.dropbox.ftpserver.auth.DropboxUser;

/**
 * A very basic implementation of the FTP FileSystemView interface. 
 * 
 * The only method really implemented is getFile since that was all that was needed
 * by this program. 
 */
public class DropboxFileSystemView implements FileSystemView {

  final DropboxUser user;
  
  public DropboxFileSystemView(DropboxUser user) {
    this.user = user;
  }
  
  @Override
  public boolean changeWorkingDirectory(String arg0) throws FtpException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void dispose() {
    // TODO Auto-generated method stub

  }

  @Override
  public FtpFile getFile(String file) throws FtpException {    
    return new DropboxFile(file, Utils.getNewWAS(user.getAccessTokenPair()));
  }

  @Override
  public FtpFile getHomeDirectory() throws FtpException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public FtpFile getWorkingDirectory() throws FtpException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isRandomAccessible() throws FtpException {
    // TODO Auto-generated method stub
    return false;
  }

}

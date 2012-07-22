package com.dropbox.ftpserver.file;
import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;

import com.dropbox.ftpserver.auth.DropboxUser;


public class DropboxFileSystemFactory implements FileSystemFactory {

  @Override
  public FileSystemView createFileSystemView(User user) throws FtpException {

    return new DropboxFileSystemView((DropboxUser)user);
  }

}

package com.dropbox.ftpserver.file;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;

import org.apache.ftpserver.ftplet.FtpFile;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.WebAuthSession;
import com.dropbox.ftpserver.Config;
import com.dropbox.ftpserver.DropboxContext;

/**
 * Implements the FtpFile interface over a dropbox file. 
 * 
 * Supports reading and writing dropbox files. 
 * 
 * The createOutputStream method is the only one doing something interesting. Please read it's javadocs. 
 */
public class DropboxFile implements FtpFile {

  protected final String path;
  protected final DropboxAPI<WebAuthSession> dropboxAPI;
  
  public DropboxFile(String path, WebAuthSession was) {
    this.path = path;
    this.dropboxAPI = new DropboxAPI<WebAuthSession>(was);
  }
  
  @Override
  public InputStream createInputStream(long offset) throws IOException {
    if(offset != 0) {
      throw new IOException("offset must be 0 since we don't support random access");
    }
    try {
      return dropboxAPI.getFileStream(path, null);
    } catch (DropboxException e) {
      throw new IOException(e);
    }
  }

  /**
   * This method is called by the STOR command. 
   * What makes this less than straightforward is that the dropbox api requires an input stream
   * and is blocking. 
   * 
   * We do two things to get around this. 
   * One, we use a pair of PipedInput and Output streams to link the OutputStream we return here
   * to the InputStream we give to the dropbox put api. 
   * 
   * Second, we invoke the dropbox put api in another thread. Specifically, we use an executor to 
   * maintain a thread pool. We construct a new runnable to invoke the dropox put api and submit 
   * this runnable to the executor. 
   */
  @Override
  public OutputStream createOutputStream(long offset) throws IOException {
    if(offset != 0) {
      throw new IOException("offset must be 0 since we don't support random access");
    }
    PipedOutputStream pos = new PipedOutputStream();
    PipedInputStream pis = new PipedInputStream(pos, 8*1024); // Use an 8K buffer instead of the 1K default
    DropboxFileWriter writer = new DropboxFileWriter(path, dropboxAPI, pis);
    DropboxContext.getInstance().getDropboxPutExecutor().execute(writer);
    return pos;
  }

  @Override
  public boolean delete() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean doesExist() {
    return true;
  }

  @Override
  public String getAbsolutePath() {
    return path;
  }

  @Override
  public String getGroupName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public long getLastModified() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getLinkCount() {
    // TODO Auto-generated method stub
    return 1;
  }

  @Override
  public String getName() {
    return path;
  }

  @Override
  public String getOwnerName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public long getSize() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public boolean isDirectory() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isFile() {
    return true;
  }

  @Override
  public boolean isHidden() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isReadable() {
    return true;
  }

  @Override
  public boolean isRemovable() {
    return false;
  }

  @Override
  public boolean isWritable() {
    return true;
  }

  @Override
  public List<FtpFile> listFiles() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean mkdir() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean move(FtpFile arg0) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean setLastModified(long arg0) {
    // TODO Auto-generated method stub
    return false;
  }

  protected static class DropboxFileWriter implements Runnable {
    private final String path;
    private final DropboxAPI<WebAuthSession> dropboxAPI;
    private final InputStream is;
    private final Config config;
    
    public DropboxFileWriter(String path, DropboxAPI<WebAuthSession> dropboxAPI, InputStream is) throws IOException {
      this.path = path;
      this.dropboxAPI = dropboxAPI;
      this.is = is;
      this.config = Config.getInstance();
    }
    
    public void run() {
      try {
        dropboxAPI.putFileOverwrite(path, is, config.getFileSize(), null);
      } catch (DropboxException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        if(is != null) {
          try {
            is.close();
          } catch (IOException e) {
            // swallow exception 
          }
        }
      }
    }
  }
  
}

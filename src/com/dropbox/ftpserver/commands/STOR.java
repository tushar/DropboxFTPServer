package com.dropbox.ftpserver.commands;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;

import org.apache.ftpserver.command.AbstractCommand;
import org.apache.ftpserver.ftplet.DataConnection;
import org.apache.ftpserver.ftplet.DataConnectionFactory;
import org.apache.ftpserver.ftplet.DefaultFtpReply;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.ftpserver.impl.IODataConnectionFactory;
import org.apache.ftpserver.impl.LocalizedFtpReply;
import org.apache.ftpserver.impl.ServerFtpStatistics;
import org.apache.ftpserver.util.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dropbox.ftpserver.DropboxContext;

/**
 * Implementation of the FTP STOR command. 
 * 
 * This command is used to put a file into Dropbox. Similar to RETR, our aim here is to do the 
 * actual transfer of data in another thread, so the the the thread calling execute can go back
 * to processing FTP commands. 
 * 
 * Similar to RETR, we create a new runnable that will do the actual file transfer, and submit 
 * this runnable to an executor. 
 * 
 * We do not support seeking into a file. Thus, the whole is transferred each time.  
 * 
 * Note: Most of this code was copied from the STOR method in Apache Ftp Server. 
 * 
 */
public class STOR extends AbstractCommand {

  private final Logger LOG = LoggerFactory.getLogger(STOR.class);
  
  @Override
  public void execute(FtpIoSession session, FtpServerContext context,
      FtpRequest request) throws IOException, FtpException {
    boolean cleanup = true;
    try {

      // argument check
      String fileName = request.getArgument();
      if (fileName == null) {
        session.write(LocalizedFtpReply.translate(session, request, context,
            FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS, "STOR",
            null));
        return;
      }

      // 24-10-2007 - added check if PORT or PASV is issued, see
      // https://issues.apache.org/jira/browse/FTPSERVER-110
      DataConnectionFactory connFactory = session.getDataConnection();
      if (connFactory instanceof IODataConnectionFactory) {
        InetAddress address = ((IODataConnectionFactory) connFactory)
            .getInetAddress();
        if (address == null) {
          session.write(new DefaultFtpReply(
              FtpReply.REPLY_503_BAD_SEQUENCE_OF_COMMANDS,
              "PORT or PASV must be issued first"));
          return;
        }
      }

      // get filename
      FtpFile file = null;
      try {
        file = session.getFileSystemView().getFile(fileName);
      } catch (Exception ex) {
        LOG.debug("Exception getting file object", ex);
      }
      if (file == null) {
        session.write(LocalizedFtpReply.translate(session, request, context,
            FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "STOR.invalid",
            fileName));
        return;
      }
      fileName = file.getAbsolutePath();

      // get permission
      if (!file.isWritable()) {
        session.write(LocalizedFtpReply.translate(session, request, context,
            FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "STOR.permission",
            fileName));
        return;
      }

      // get data connection
      session.write(
          LocalizedFtpReply.translate(session, request, context,
              FtpReply.REPLY_150_FILE_STATUS_OKAY, "STOR", fileName))
          .awaitUninterruptibly(10000);

      DataConnection dataConnection;
      try {
        dataConnection = session.getDataConnection().openConnection();
      } catch (Exception e) {
        LOG.debug("Exception getting the input data stream", e);
        session.write(LocalizedFtpReply.translate(session, request, context,
            FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION, "STOR", fileName));
        return;
      }

      STORRunnable storRunnable = 
        new STORRunnable(session, context, file, dataConnection, request);
      DropboxContext.getInstance().getFtpSTORExecutor().execute(storRunnable);      
      cleanup = false;
      
    } finally {
      if(cleanup) {
        session.resetState();
        session.getDataConnection().closeDataConnection();
      }
    }
  }
  
  protected static class STORRunnable implements Runnable {
    
    private final Logger LOG = LoggerFactory.getLogger(STORRunnable.class);
    
    protected final FtpIoSession session;
    protected final FtpServerContext context;
    protected final FtpFile file;
    protected final DataConnection dataConnection;
    protected final FtpRequest request;
    
    public STORRunnable(FtpIoSession session, FtpServerContext context,
        FtpFile file, DataConnection dataConnection, FtpRequest request) {
      this.session = session;
      this.context = context;
      this.file = file;
      this.dataConnection = dataConnection;
      this.request = request;
    }
    
    public void run() {

      try {
        String fileName = request.getArgument();

        // transfer data
        boolean failure = false;
        OutputStream outStream = null;
        try {
          outStream = file.createOutputStream(0);
          long transSz = dataConnection.transferFromClient(
              session.getFtpletSession(), outStream);

          // attempt to close the output stream so that errors in
          // closing it will return an error to the client (FTPSERVER-119)
          if (outStream != null) {
            outStream.close();
          }

          LOG.info("File uploaded {}", fileName);

          // notify the statistics component
          ServerFtpStatistics ftpStat = (ServerFtpStatistics) context
              .getFtpStatistics();
          ftpStat.setUpload(session, file, transSz);

        } catch (SocketException ex) {
          LOG.debug("Socket exception during data transfer", ex);
          failure = true;
          session.write(LocalizedFtpReply.translate(session, request, context,
              FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED, "STOR",
              fileName));
        } catch (IOException ex) {
          LOG.debug("IOException during data transfer", ex);
          failure = true;
          session.write(LocalizedFtpReply.translate(session, request, context,
              FtpReply.REPLY_551_REQUESTED_ACTION_ABORTED_PAGE_TYPE_UNKNOWN,
              "STOR", fileName));
        } finally {
          // make sure we really close the output stream
          IoUtils.close(outStream);
        }

        // if data transfer ok - send transfer complete message
        if (!failure) {
          session.write(LocalizedFtpReply.translate(session, request, context,
              FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, "STOR", fileName));

        }

      } finally {
        session.resetState();
        session.getDataConnection().closeDataConnection();
      }
    }
  }
}

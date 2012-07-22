package com.dropbox.ftpserver.commands;

import java.io.IOException;
import java.io.InputStream;
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

public class RETR extends AbstractCommand {

  private final Logger LOG = LoggerFactory.getLogger(RETR.class);
  
  @Override
  public void execute(FtpIoSession session, FtpServerContext context,
      FtpRequest request) throws IOException, FtpException {

    boolean cleanup = true;
    try {

      // argument check
      String fileName = request.getArgument();
      if (fileName == null) {
        session.write(LocalizedFtpReply.translate(session, request, context,
            FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS, "RETR",
            null));
        return;
      }

      // get file object
      FtpFile file = null;
      try {
        file = session.getFileSystemView().getFile(fileName);
      } catch (Exception ex) {
        LOG.debug("Exception getting file object", ex);
      }
      if (file == null) {
        session.write(LocalizedFtpReply.translate(session, request, context,
            FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "RETR.missing",
            fileName));
        return;
      }
      fileName = file.getAbsolutePath();

      // check file existence
      if (!file.doesExist()) {
        session.write(LocalizedFtpReply.translate(session, request, context,
            FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "RETR.missing",
            fileName));
        return;
      }

      // check valid file
      if (!file.isFile()) {
        session.write(LocalizedFtpReply.translate(session, request, context,
            FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "RETR.invalid",
            fileName));
        return;
      }

      // check permission
      if (!file.isReadable()) {
        session.write(LocalizedFtpReply.translate(session, request, context,
            FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "RETR.permission",
            fileName));
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

      // get data connection
      session.write(LocalizedFtpReply.translate(session, request, context,
          FtpReply.REPLY_150_FILE_STATUS_OKAY, "RETR", null));

      DataConnection dataConnection;
      try {
        dataConnection = session.getDataConnection().openConnection();
      } catch (Exception e) {
        LOG.debug("Exception getting the output data stream", e);
        session.write(LocalizedFtpReply.translate(session, request, context,
            FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION, "RETR", null));
        return;
      }

      // send file data to client
      RETRRunnable retrRunnable =
        new RETRRunnable(session, context, file, dataConnection, request);
      DropboxContext.getInstance().getFtpRETRExecutor().execute(retrRunnable);
      cleanup = false;

    } finally {
      if(cleanup) {
        session.resetState();
        session.getDataConnection().closeDataConnection();
      }
    }
  }
  
  protected static class RETRRunnable implements Runnable {
    
    private final Logger LOG = LoggerFactory.getLogger(RETR.class);
    
    protected final FtpIoSession session;
    protected final FtpServerContext context;
    protected final FtpFile file;
    protected final DataConnection dataConnection;
    protected final FtpRequest request;
    
    public RETRRunnable(FtpIoSession session, FtpServerContext context,
        FtpFile file, DataConnection dataConnection, FtpRequest request) {
      this.session = session;
      this.context = context;
      this.file = file;
      this.dataConnection = dataConnection;
      this.request = request;
    }
    
    public void run() {
     
      LOG.info("In RETRRunnable!");
      
      try {
        String fileName = request.getArgument();
        boolean failure = false;
        InputStream is = null;
        try {
          // open streams
          is = file.createInputStream(0);

          // transfer data
          long transSz = dataConnection.transferToClient(
              session.getFtpletSession(), is);
          // attempt to close the input stream so that errors in
          // closing it will return an error to the client (FTPSERVER-119)
          if (is != null) {
            is.close();
          }

          LOG.info("File downloaded {}", fileName);

          // notify the statistics component
          ServerFtpStatistics ftpStat = (ServerFtpStatistics) context
              .getFtpStatistics();
          if (ftpStat != null) {
            ftpStat.setDownload(session, file, transSz);
          }

        } catch (SocketException ex) {
          LOG.error("Socket exception during data transfer", ex);
          failure = true;
          session.write(LocalizedFtpReply.translate(session, request, context,
              FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED, "RETR",
              fileName));
        } catch (IOException ex) {
          LOG.error("IOException during data transfer", ex);
          failure = true;
          session.write(LocalizedFtpReply.translate(session, request, context,
              FtpReply.REPLY_551_REQUESTED_ACTION_ABORTED_PAGE_TYPE_UNKNOWN,
              "RETR", fileName));
        } finally {          
          // make sure we really close the input stream
          IoUtils.close(is);
        }

        // if data transfer ok - send transfer complete message
        if (!failure) {
          session.write(LocalizedFtpReply.translate(session, request, context,
              FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, "RETR", fileName));

        }
      } finally {
        session.resetState();
        session.getDataConnection().closeDataConnection();
      }                   
    }
    
  }
  
}

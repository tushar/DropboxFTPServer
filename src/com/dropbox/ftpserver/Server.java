package com.dropbox.ftpserver;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.command.CommandFactoryFactory;
import org.apache.ftpserver.listener.ListenerFactory;

import com.dropbox.ftpserver.auth.AuthTerminal;
import com.dropbox.ftpserver.auth.DropboxUserManager;
import com.dropbox.ftpserver.commands.RETR;
import com.dropbox.ftpserver.commands.STOR;
import com.dropbox.ftpserver.file.DropboxFileSystemFactory;


public class Server {
  protected FtpServer server;
  protected final DropboxContext dropboxContext;
  protected final Config config;
  
  public Server(String key, String secret) throws IOException {
    config = Config.getInstance();
    
    Executor dropboxPutExecutor = 
      Executors.newFixedThreadPool(config.getNumPutThreads());
    Executor ftpSTORExecutor = 
      Executors.newFixedThreadPool(config.getNumPutThreads());
    Executor ftpRETRExecutor = 
      Executors.newFixedThreadPool(config.getNumGetThreads());

    dropboxContext = DropboxContext.createInstance(key, secret, 
        dropboxPutExecutor, ftpSTORExecutor, ftpRETRExecutor);
  }
  
  public void startFTPServer() throws Exception {
    if(server != null) {
      return;
    }        
        
    FtpServerFactory serverFactory = new FtpServerFactory();    
    
    // replace the default listener
    ListenerFactory factory = new ListenerFactory();           
    // set the port of the listener
    factory.setPort(config.getPort());
    serverFactory.addListener("default", factory.createListener());
    
    // set up connection config
    ConnectionConfigFactory connConfigFactory = new ConnectionConfigFactory();
    connConfigFactory.setAnonymousLoginEnabled(false);
    connConfigFactory.setMaxThreads(config.getMaxFtpWorkerThreads());
    connConfigFactory.setMaxLogins(config.getMaxLogins());
    serverFactory.setConnectionConfig(connConfigFactory.createConnectionConfig());
        
    // set up command factory
    CommandFactoryFactory commandFactoryFactory = new CommandFactoryFactory();
    commandFactoryFactory.addCommand("STOR", new STOR());       
    commandFactoryFactory.addCommand("RETR",new RETR());
    serverFactory.setCommandFactory(commandFactoryFactory.createCommandFactory());    
    
    DropboxUserManager dropboxUserManager = new DropboxUserManager();
    serverFactory.setUserManager(dropboxUserManager);
    
    DropboxFileSystemFactory fileSystemFactory = new DropboxFileSystemFactory();
    serverFactory.setFileSystem(fileSystemFactory);
    
    // start the server
    this.server = serverFactory.createServer(); 
            
    server.start();  
  }
  
  public void startAuthTerminal() {
    AuthTerminal authTerminal = new AuthTerminal();
    authTerminal.run();
  }
  
  public static void main(String[] args) throws Exception {
    if(args.length != 2) {
      System.err.println("Must pass the dropbox app key and secret as cmd line params");
      return;
    }
    
    Server server = new Server(args[0], args[1]);
    server.startAuthTerminal();
    server.startFTPServer();
  }
  
}

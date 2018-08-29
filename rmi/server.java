 import java.io.*;
 import java.net.ServerSocket;
 import java.net.Socket;
 import java.nio.file.Path;
 import java.nio.file.Paths;
 
 public class server
 {
     private ServerSocket getSRSocket;
     private OutputStream outputStream, outputStream1;
     private InputStream inputStream;
     private static int totalThreadsRunningCount = 0;
     private static boolean isShutDownCalled = false;

     server(Socket getClientSocket, ServerSocket getSRSkt){
         try {
             this.getSRSocket = getSRSkt;
             this.outputStream = getClientSocket.getOutputStream();
             this.outputStream1 = getClientSocket.getOutputStream();
             this.inputStream = getClientSocket.getInputStream();

         } catch (Exception e){
             e.printStackTrace();
             System.out.println(e.getMessage());
         }
     }

     /**
      * update total number of threads active
      * @param int num
      * @return boolean var
      */
     public static synchronized void updateThreadCount(int num) {
         server.totalThreadsRunningCount = totalThreadsRunningCount + num;
     }
     
     
     /**
      * return total threads active
      * @param void
      * @return int totalthreads
      */
     public static synchronized int getRunningThreadsCount() {
         return server.totalThreadsRunningCount;
     }

     
     /**
      * check if server is shutdown
      * @param void
      * @return boolean var
      */
     public static synchronized boolean isServerShutDown() {
         return server.isShutDownCalled;
     }

     
     /**
      * update server shutdown status
      * @param boolean var
      * @return void
      */
     public static synchronized void updateServerShutDown(boolean isShutDown) {
         server.isShutDownCalled = isShutDown;
     }


     /**
      * read and write file to specified location
      * @param String path
      * @return void
      */
     private void writeUploadFileFromClient(String getfilePath) {
         try {
        	 	
        	 
        	 	int totalBytesRead = 0;
        	 	
        	 	// Create file output stream to write the data into file
        	 	FileOutputStream fileOutputStr;
        	 	File checkFile = new File(getfilePath);

        	 	//check for existence of file
        	 	if (checkFile.exists()) {
        	 		fileOutputStr = new FileOutputStream(getfilePath,true);
        	 		totalBytesRead = (int)checkFile.length();
        	 	}
        	 	else{
        	 			fileOutputStr = new FileOutputStream(getfilePath);
        	 	}
        	 	
        	 	// Send total file length count to client currently present on its directory
        	 	ObjectOutputStream TotalFileLength = new ObjectOutputStream(outputStream);
        	 	TotalFileLength.writeObject(totalBytesRead);
             
        	 	// Create file output stream to write the data into file
        	 	BufferedOutputStream bufferedOutputStr = new BufferedOutputStream(fileOutputStr);

        	 	//sendErrorToClient(0, null);

        	 	// get original file total size from client
        	 	ObjectInputStream readByteStream = new ObjectInputStream(inputStream);
        	 	int totalBytesFileCount = (int)readByteStream.readObject();

        	 	// byte array initialization
        	 	byte[] bytesArray = new byte[1024];
            
        	 	//condition when to continue reading file
        	 	if(totalBytesRead < totalBytesFileCount) {
        	 		totalBytesRead += inputStream.read(bytesArray, 0, bytesArray.length);
        	 		bufferedOutputStr.write(bytesArray);
        	 		bufferedOutputStr.flush();
        	 	}
        	 	while (totalBytesRead < totalBytesFileCount) {
        	 		totalBytesRead += inputStream.read(bytesArray);
        	 		bufferedOutputStr.write(bytesArray);
        	 		bufferedOutputStr.flush();
        	 	}

        	 	readByteStream.close();
        	 	inputStream.close();
        	 	outputStream.close();

         	} catch (FileNotFoundException e) {
             System.out.println(e.getMessage());
             e.printStackTrace();
             //sendErrorToClient(404, "File not found");
         	}catch (Exception e) {
         		e.printStackTrace();
         		System.out.println(e.getMessage());
         	}
     }
     
     
     /**
      * read file and send to client for downloading
      * @param String filepath String fileoffset
      * @return void
      */
     private void readDownloadFileToClient(String getfilePath, String fileOffset) {
      	 File file1 = new File(getfilePath);
      	 if(!file1.exists())
      	 {
      		sendErrorToClient(404, "file not found");
      		return;
      	 }
         System.out.println("server "+fileOffset);
         int offset=Integer.parseInt(fileOffset);
         int value=offset;
         System.out.println("server file path "+getfilePath);
    	 try {
             	 
             	File file = new File(getfilePath);
             	int numBytes = (int) file.length();
             	int numBytes1 = (int) file.length();
             	System.out.println("numBytes1 "+numBytes1);
             	FileInputStream fileInput = new FileInputStream(file);
             	BufferedInputStream bufferedInput = new BufferedInputStream(fileInput);      
            
             	//condition when file is partially send previously
             	if(offset>0 && offset<numBytes) {
             		bufferedInput.skip(offset);
             		ObjectOutputStream TotalFileLength = new ObjectOutputStream(outputStream);
             		TotalFileLength.writeObject(numBytes);
             		outputStream.flush();
             	}
             	else {
             		ObjectOutputStream TotalFileLength = new ObjectOutputStream(outputStream);
             		TotalFileLength.writeObject(numBytes);
             		outputStream.flush();
             	}
             
             	sendErrorToClient(0, null);
             	int bytesSent = 0;
             	int startByteCount=0;
             
             	try {
             		// initialize byte array
             		byte[] bytesArray = new byte[1024];

             		// read and write file to client
             		bytesSent = bufferedInput.read(bytesArray, startByteCount, bytesArray.length);
             		outputStream.write(bytesArray, 0, bytesArray.length);
             		outputStream.flush();
                 
             		if(offset>=numBytes)
             			offset=0;

             		// conditon if to continue to read the file
             		while (startByteCount < numBytes){
                	 		startByteCount++;
                	 		if(startByteCount>offset)
                	 		{
                	 			bufferedInput.read(bytesArray);
                	 			outputStream.write(bytesArray);
                	 			outputStream.flush();
                	 		}
                	 		else
                	 			continue;
             		}
             	} catch (IOException e) {
             		e.printStackTrace();
             		System.out.println(e.getMessage());
             	}
             	bufferedInput.close();
             	inputStream.close();
         	}
         	catch (FileNotFoundException e) {
         		System.out.println(e.getMessage());
         		e.printStackTrace();
         		sendErrorToClient(404, "File not found");
         	}
         	catch (Exception e) {
         		e.printStackTrace();
         		System.out.println(e.getMessage());
         	}
     	}
     
          
     /**
      * displays files present in specified directory
      * @param String dirPath
      * @return void
      */
     void listFilesFromDirectory(String dirPath){
         try {
        	 	//get file name
        	 	if(0 == dirPath.indexOf("/") || 0 == dirPath.indexOf("\\"))
        	 		dirPath = dirPath.substring(1);
        	 	File checkFile = new File(dirPath);
        	 	String fileNames = "";
        	 	if (checkFile.exists()) {
        	 		sendErrorToClient(0, null);
        	 		String[] files = new File(dirPath).list();
        	 		fileNames = "Root Directory@ /";
        	 	   for (String file : files){
        	 		   fileNames = fileNames + "\n" + file;
                   }
        	 	}
        	 	else {
        	 		sendErrorToClient(404, "Directory not found");
        	 		return;
        	 	}
        	 	ObjectOutputStream outputToClient = new ObjectOutputStream(outputStream);
        	 	outputToClient.writeObject(fileNames);
        	 	outputToClient.flush();
        	 	outputToClient.close();
        	 	inputStream.close();
         	} catch(IOException e){
             e.printStackTrace();
             System.out.println(e.getMessage());
         }
     }


     /**
      * create directory at server side
      * @param String dirPath
      * @return void
      */
     private void createDirectory(String dirPath) {
         try {
                File getDirectory = new File(dirPath);
                
                //check if file exists
                if (!getDirectory.exists()) {
                	getDirectory.mkdir();
                	
                	// send success code to client
                	sendErrorToClient(0, null);
                	ObjectOutputStream outputToClient = new ObjectOutputStream(outputStream);
                	outputToClient.writeObject("successfully created directory@ " + dirPath);
                	outputToClient.flush();
                	outputToClient.close();
                } 
                else
                	sendErrorToClient(210, "Directory already exists");
         	}catch (Exception e) {
             e.printStackTrace();
             System.out.println(e.getMessage());
         }
     }

     
     /**
      * delete directory at specified path if not empty
      * @param String dirPath
      * @return void
      */
     private void removeDirectory(String dirPath) {
         try{
        	 File getDirectory = new File(dirPath);
             
             //check existence of file
             if (getDirectory.exists()) {
                 if (getDirectory.listFiles().length > 0)
                     sendErrorToClient(400, "Directory not empty it may contain files");
                 else {
                     getDirectory.delete();
                     sendErrorToClient(0, null);
                     ObjectOutputStream outputToClient = new ObjectOutputStream(outputStream);
                     outputToClient.writeObject("successfully deleted directory@ " + dirPath);
                     outputToClient.flush();
                     outputToClient.close();
                 }
              }else
                 sendErrorToClient(404, "Directory not found");
          }catch (Exception e){
        	  e.printStackTrace();
        	  System.out.println(e.getMessage());
         }
     }

     
     /**
      * delete file at specified path if it exists
      * @param String filepath
      * @return void
      */
     private void deleteFile(String getfilePath) {
         try{
             File checkFile = new File(getfilePath);
             if (checkFile.exists()) {
                 if (checkFile.listFiles() != null) {
                     if (checkFile.listFiles().length > 0) {
                         sendErrorToClient(404, "File not found");
                         return;
                     }
                 }
                 checkFile.delete();
                 sendErrorToClient(0, null);
                 ObjectOutputStream outputToClient = new ObjectOutputStream(outputStream);
                 outputToClient.writeObject("File deleted successfully@ /" + getfilePath);
                 outputToClient.flush();
                 outputToClient.close();
             } else
                 sendErrorToClient(404, "File not found");
          }catch (Exception e){
        	  e.printStackTrace();
        	  System.out.println(e.getMessage());
          }
     }


     /**
      * Shutdown server if its in idle state
      * @param void
      * @return void
      */
     private void shutdownServer() {
         try {
             	server.updateServerShutDown(true);
             	ObjectOutputStream messageToClient = new ObjectOutputStream(outputStream);
             	
             	//check total running threads are more than one
             	if (server.getRunningThreadsCount() > 1) {
             		messageToClient.writeObject("Server is busy. Server will shutdown once idle");
             		while(true) {
             			
             			//if client less than or equal to one shutdown server
             			if (server.getRunningThreadsCount() <= 1) {                       
             				this.getSRSocket.close();     
             				server.updateServerShutDown(false);
             				break;
             			}
             		}
             	 } else {
                 messageToClient.writeObject("Server is shutdown");
                 messageToClient.close();
                 this.getSRSocket.close();
             }
         	} catch (IOException e){
         		System.out.println(e.getMessage());
         		e.printStackTrace();
         		}
      }


     /**
      * read client request and call specific method
      * @param void
      * @return void
      */
     private void processClientRequest() {
         try {
        	 
             	// Read the commands sent by client and call specific function
             	ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
             	String command = (String) objectInputStream.readObject();
             	//System.out.println("entered split server");
             	String commands[] = command.split("@");
             	if (commands.length <= 0){
             		objectInputStream.close();
             		return;
             	}

             	// read command and check if valid
             	switch (commands[0]){
                 	 case "upload":{
                 		 this.writeUploadFileFromClient(commands[1]);
                 		 break;
                 	 }
                 	 case "download":{
                 		 this.readDownloadFileToClient(commands[1],commands[2]);
                 		 break;
                 	 }
                  	case "dir": {
                 		if (commands.length <= 1)
                 			this.listFilesFromDirectory("./");
                 		else
                 			this.listFilesFromDirectory(commands[1]);
                 		break;
                 	 }
                 	 case "mkdir":{
                 		 this.createDirectory(commands[1]);
                 		 break;
                 	 }
                 	 case "rmdir":{
                 		 this.removeDirectory(commands[1]);
                 		 break;
                 	 }
                 	 case "rm":{
                 		 this.deleteFile(commands[1]);
                 		 break;
                 	 }
                 	 case "shutdown":{
                 		 this.shutdownServer();
                 		 break;
                 	 }
                 	 default:{
                 		 System.out.println("Error: Invalid Command");
                 		 break;
                 	 }
             	}
             	objectInputStream.close();
             	if (inputStream != null) inputStream.close();
             	if (outputStream != null) outputStream.close();
         }
         catch (Exception e) {
             e.printStackTrace();
             System.out.println(e.getMessage());
         }
     }

     
     /**
      * Print message
      * @param String message
      * @return void
      */
     private void println(String message) {
         System.out.println(message);
     }

     
     /**
      * checks for error from server side and send to client
      * @param int errCode and String errMessage
      * @return void
      */
     private void sendErrorToClient(int errCode, String errMessage) {
         try {
             	ObjectOutputStream sendErrorToClient = new ObjectOutputStream(outputStream);
             	sendErrorToClient.writeObject(errCode);
             	if(errCode != 0){
             		ObjectOutputStream errorMessageToClient = new ObjectOutputStream(outputStream);
             		errorMessageToClient.writeObject(errMessage);
             		sendErrorToClient.close();
             		errorMessageToClient.close();
             	}
         }
         catch (Exception e){
             System.out.println(e.getMessage());
             e.printStackTrace();
         }
     }


     /**
      * server runs from here
      * @param String args[]
      * @return void
      */
     public static void main(String[] args) {
         try {
             	if (args.length < 2){
             		System.out.println("Error! Start server with 'start <port #>'");
             		return;
             	}
             	if (args[0].compareTo("start")==0)                  //entered argument args[0] is start
             	{
             		int port = Integer.parseInt(args[1]);           //args[1] get port number
             		ServerSocket socket = new ServerSocket(port);
             		System.out.println("Server is up and running on port#: " + port);
             		start(socket);
             	} else
             		System.out.println("Error: Start server as 'start <port #>'");
         }catch (Exception e){
             e.printStackTrace();
             System.out.println(e.getMessage());
         }
     }
     
     
     /**
      * connect client to server
      * @param Socket clientSocket, ServerSocket getSRSocket
      * @return void
      */
     public static void startConnectionThread(Socket clientSocket, ServerSocket getSRSocket){
         	 Runnable serverRunnable = new Runnable(){
             @Override
             public void run(){
                 try {
                     	updateThreadCount(1);
                     	server socketServer = new server(clientSocket, getSRSocket);
                     	socketServer.processClientRequest();
                     	updateThreadCount(-1);
                     	clientSocket.close();
                 }catch (IOException e){
                     e.printStackTrace();
                     System.out.println(e.getMessage());
                 }
             }
         };
         Thread srvThread = new Thread(serverRunnable);
         srvThread.start();
     }
     

     /**
      * accepts client incoming request if server is active
      * @param ServerSocket getSRSocket
      * @return void
      */
     public static void start(ServerSocket getSRSocket) {
         
    	 //if server not shutdwon keep accepting request
         while (!getSRSocket.isClosed() && !server.isServerShutDown()) {
             try {
                 	//accept incoming request
                 	Socket clientSocket = getSRSocket.accept();
                 	startConnectionThread(clientSocket, getSRSocket);
             }catch (IOException e){
                 if (getSRSocket.isClosed())
                	 System.out.println("Server has been shutdown");
                 else {
                     	e.printStackTrace();
                     	System.out.println(e.getMessage());
                 }	
             }
         }
     }
 }
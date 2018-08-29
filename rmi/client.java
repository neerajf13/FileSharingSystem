import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

 public class client {
	 
     private Socket clientSocket = null;
     private OutputStream outputStream = null;
     private InputStream inputStream = null;
     
     client(String host, int port) {
         try{
             	this.clientSocket = new Socket(host, port);
             	outputStream = this.clientSocket.getOutputStream();
             	inputStream = this.clientSocket.getInputStream();
         }catch (Exception e) {
        	 System.out.println("Error: cannot connect to server");
       }
     }

     /**
      * checks for error from server side
      * @param void
      * @return boolean
      */
     private boolean checkServerError () {
         try {
             // Read the response from server
             	ObjectInputStream getsSrResponse = new ObjectInputStream(inputStream);
             	Object err = getsSrResponse.readObject();
             	int errorCode = 1;
             	if (err != null)
             		errorCode = (int) err;
             	if (errorCode > 0) {
             		ObjectInputStream getsSrResponse1 = new ObjectInputStream(inputStream);
             		String errorMessage = (String)getsSrResponse1.readObject();
             		System.out.println("Error " + errorCode + ": " + errorMessage);
             		getsSrResponse.close();
             		getsSrResponse1.close();
             		return true;
             	}
         }
         catch (Exception e){
             e.printStackTrace();
             System.out.println(e.getMessage());
         }
         return false;
     }
     

     /**
      * sends file from client to server
      * @param clientFilePathName and serverFilePathName
      * @return void
      */
     private void uploadFileToServer(String clientFilePathName, String serverFilePathName) {

            try {
            	
             		File file1=new File(clientFilePathName);
             		if(!file1.exists()) {
             			System.out.println("404: File not found at client");
             			return;
             		}
            		// Send upload file name to server
                	ObjectOutputStream writeDataToServer = new ObjectOutputStream(outputStream);
                	writeDataToServer.writeObject("upload@" + serverFilePathName);

                	File file = new File(clientFilePathName);
                	int bytesTransferred = 0;                
                	System.out.println("File uploading to server...");

                	// initialize the byte array
                	byte[] bytesArray = new byte[1024];
                	FileInputStream fileInput = new FileInputStream(file);
                	BufferedInputStream bufferedInput = new BufferedInputStream(fileInput);
                
                	// get updated file size from server already uploaded
                	ObjectInputStream readByteStream = new ObjectInputStream(inputStream);
                	bytesTransferred = (int)readByteStream.readObject();
                
                	//Skip sent bytes to Resume the download from previous state
                	bufferedInput.skip((long)bytesTransferred);
                
                	System.out.println("CLIENT bytesTransferred: "+ bytesTransferred);
                
                	//Sending the total Size of the File
                	ObjectOutputStream writeDataToServer1 = new ObjectOutputStream(outputStream);
                	int bytesToBeUploaded = (int)file.length();
                	writeDataToServer1.writeObject(bytesToBeUploaded);
                
                	// condition to check if to read file
                 	if(bytesTransferred < bytesToBeUploaded) {
                		bytesTransferred += bufferedInput.read(bytesArray, 0, bytesArray.length);
                		outputStream.write(bytesArray, 0, bytesArray.length);
                		outputStream.flush();
                	}
                
                	// condition to continue reading file
                	while (bytesTransferred < bytesToBeUploaded) {
                		bytesTransferred += bufferedInput.read(bytesArray);
                		outputStream.write(bytesArray);
                		outputStream.flush();

                    float perc = (float)bytesTransferred / (float) bytesToBeUploaded * (float)100.0;
                    System.out.print("\r");
                    System.out.print("Uploading ... " + ((int)perc) + "%");
                }

                System.out.println("\nFile Uploaded");
                bufferedInput.close();
                outputStream.close();

            }
            catch (ClassNotFoundException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                
            }
            catch (FileNotFoundException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
     }

     /**
      * reads and writes file from server to client
      * @param clientFilePathName and serverFilePathName
      * @return void
      */
     private void downloadFileFromServer(String serverFilePathName, String clientFilePathName) {
    	 int fileOffset=0;
         try {
        	 	
        	 
             	//clientFilePathName = s + "/" + clientFilePathName;
             	File file=new File(clientFilePathName);
             	File file1=new File(serverFilePathName);
             	if(!file1.exists()) {
             		System.out.println("404: File not found at server");
             		return;
             	}
             	System.out.println("client path name "+clientFilePathName);
             	byte[] b = new byte[(int)file.length()];
             	
             	//check existence of file
             	if(file.exists())
             		fileOffset=b.length;
             
             	ObjectOutputStream writeDataToServer = new ObjectOutputStream(outputStream);
             	writeDataToServer.writeObject("download@" + serverFilePathName+"@" +fileOffset);

             	// Get the total file size info from server
             	ObjectInputStream TotalFileLength = new ObjectInputStream(inputStream);
             	int totalSize = (int)TotalFileLength.readObject();
             	System.out.println("total size "+totalSize);
             	int bytesRead = 0;
             	int totalBytes=0;
             	if(fileOffset<totalSize)
             		totalBytes=totalSize-fileOffset;
             	else
             		totalBytes=totalSize;
             	System.out.println("total bytes updated "+totalBytes);
             	System.out.println("client bytes to download "+totalSize);

             	// Read the file bytes from Server and write to the file at client
             	try {
             			byte[] bytesArray = new byte[1024];

                 // Create file output stream to write the data into file
             		FileOutputStream fos = null;
             		if(fileOffset>0 && fileOffset<totalSize) {
             			System.out.println("Resuming download...");
             			fos = new FileOutputStream(clientFilePathName,true);	 
             		}
             		else {
             			fos = new FileOutputStream(clientFilePathName);
             			fileOffset=0;
             		}
             		BufferedOutputStream bos = new BufferedOutputStream(fos);
             		bytesRead += inputStream.read(bytesArray, 0, bytesArray.length);
             		bos.write(bytesArray);
             		bos.flush();
             		             		
             		//conition to continue reading and writing the file
             		while (bytesRead < totalBytes) {
             			bytesRead += inputStream.read(bytesArray);
             			bos.write(bytesArray);
             			bos.flush();
             			float perc = ((float) bytesRead +(float) fileOffset)/ ((float) totalSize)* (float) 100.0;
             			System.out.print("\r");
             			System.out.print("Downloading File... " + ((int) perc) + "% complete");
             		}
             		System.out.println("\n File has been successfully downloaded");
             	 } catch (IOException e) {
             		 System.out.println(e.getMessage());
             		 e.printStackTrace();
                }
             	writeDataToServer.close();
             	TotalFileLength.close();
         }
         catch(Exception e) {
             e.printStackTrace();
             System.out.println(e.getMessage());
         }
     }

     
     /**
      * send request to server to check existence of file or directory and manipulate the file
      * @param String command
      * @return void
      */
     private void dirManipulation(String command) {
         try {
             	ObjectOutputStream writeDataToServer = new ObjectOutputStream(outputStream);
             	writeDataToServer.writeObject(command);
             	writeDataToServer.flush();
             	
             	// check for server error
             	if(checkServerError())
             		return;

             	ObjectInputStream getInputFromServer = new ObjectInputStream(inputStream);
             	System.out.println((String)getInputFromServer.readObject());
             	writeDataToServer.close();
             	getInputFromServer.close();
         }catch (IOException e) {
             System.out.println(e.getMessage());
             e.printStackTrace();
         }
         catch (ClassNotFoundException e) {
             System.out.println(e.getMessage());
             e.printStackTrace();
         }
     }

     /**
      * send request to server for server to be shutdown
      * @param String command
      * @return void
      */
     private void shutDownRequestServer(String command) {
         try {
             	ObjectOutputStream writeDataToServer = new ObjectOutputStream(outputStream);
             	writeDataToServer.writeObject(command);
             	writeDataToServer.flush();
             	ObjectInputStream getDataFromServer = new ObjectInputStream(inputStream);
             	String msg = (String) getDataFromServer.readObject();
             	System.out.println(msg);
             	getDataFromServer.close();
             	this.inputStream.close();
             	this.outputStream.close();
             }catch (Exception e){
             System.out.println(e.getMessage());
             e.printStackTrace();
         }
     }

     /**
      * read client command to process the required functionality 
      * @param String[] commands
      * @return void
      */
     public void readClientCommand(String []commands){
         if (commands.length > 0) {
             switch (commands[0]) {
                 case "upload": {
                     if (commands.length == 3) {
                         this.uploadFileToServer(commands[1], commands[2]);
                     } else {
                         System.out.println("Error: Command not found! Exiting...");
                         return;
                     }
                     break;
                 }
                 case "download": {
                     if (commands.length == 3) {
                         this.downloadFileFromServer(commands[1], commands[2]);
                     } else {
                         System.out.println("Error: Command not found! Exiting...");
                         return;
                     }
                     break;
                 }
                 case "dir": {
                     if (commands.length == 2) {
                         this.dirManipulation("dir@" + commands[1]);
                         System.out.println("in client "+commands[1]);
                     } else {
                         this.dirManipulation("dir@./");
                     }
                     break;
                 }
                 case "mkdir": {
                     if (commands.length == 2) {
                         this.dirManipulation(commands[0] + "@" + commands[1]);
                     } else {
                         System.out.println("Error: Command not found! Exiting...");
                         return;
                     }
                     break;
                 }
                 case "rmdir": {
                     if (commands.length == 2) {
                         this.dirManipulation(commands[0] + "@" + commands[1]);
                     } else {
                         System.out.println("Error: Command not found! Exiting...");
                         return;
                     }
                     break;
                 }
                 case "rm": {
                     if (commands.length == 2) {
                         this.dirManipulation(commands[0] + "@" + commands[1]);
                     } else {
                         System.out.println("Error: Command not found! Exiting...");
                         return;
                     }
                     break;
                 }
                 case "shutdown": {
                     this.shutDownRequestServer(commands[0]);
                     break;
                 }
                 default: {
                     System.out.println("Error: Command not found! Exiting...");
                     try {
                         this.inputStream.close();
                         this.outputStream.close();
                     } catch (IOException e) {
                         System.out.println(e.getMessage());
                         e.printStackTrace();
                     }
                     break;
                 }
             }
         } else{
             try{
                 this.inputStream.close();
                 this.outputStream.close();
             } catch (IOException e) {
                 System.out.println(e.getMessage());
                 e.printStackTrace();
             }
         }
     }

     /**
      * Main method will initiate first - program runs from here
      * @param String[] args
      * @return void
      */
     public static void main(String[] args){
         try {
             	String hostWithPort = System.getenv("PA1_SERVER");
             	String splitHostAndPort[] = hostWithPort.split("\\s*:\\s*");
             	if(splitHostAndPort.length < 2) {
             		System.out.println("Environment variable <PA1_SERVER> not set correctly");
                 return;
             	}
             	client clientObj = new client(splitHostAndPort[0], Integer.parseInt(splitHostAndPort[1]));
             	if (clientObj.outputStream != null && clientObj.inputStream != null)
             		clientObj.readClientCommand(args);
         } catch (Exception e) {
             System.out.println(e.getMessage());
             e.printStackTrace();
         }
     }
 }
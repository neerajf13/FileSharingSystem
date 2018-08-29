import java.io.*;
import java.rmi.*;

public class client {

    private serverInterface srvObject = null;

    client(serverInterface sObject) {
        this.srvObject = sObject;
    }
    
    
    /**
     * Main method will initiate first - program runs from here
     * @param String[] args
     * @return void
     */
    public static void main(String[] args) {
        try 
        {
            String server = System.getenv("PA2_SERVER");
            if (server != null && !server.isEmpty())
            {
                String[] inpArray = server.split(":");
                if (inpArray.length > 0 && inpArray[0] != null && !inpArray[0].isEmpty())
                {
                    String urlName = "rmi://" + inpArray[0] + "/FileServer";
                    System.out.println(urlName);
                    serverInterface servInterface = (serverInterface)Naming.lookup(urlName);
                    if (servInterface != null)
                    {
                        client cl = new client(servInterface);
                        cl.processClientCommandToServer(args);
                    }
                }
            } 
            else
            	System.out.println("Environment variable not set");
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    /**
     * read client command to process the required functionality to server 
     * @param String[] inputCmd
     * @return void
     */
    public void processClientCommandToServer(String[] inputCmd) {
        try
        {
        	
          //check for the operation to perform
          switch(inputCmd[0]) {
          	case "upload": {
              if (inputCmd.length > 2)
              	this.uploadClientFile(inputCmd[2], inputCmd[1]);
              else 
              	System.out.println("Error 400: BAD_REQUEST Invalid command");
              break;
          	}
          	case "download": {
              if (inputCmd.length > 2)
              	this.downloadServerFile(inputCmd[1], inputCmd[2]);
              else
              	System.out.println("Error 400: BAD_REQUEST Invalid command");
              break;
          	}
            case "dir":
            case "mkdir":
            case "rmdir":
            case "rm":
            case "shutdown": {
                if (inputCmd.length > 1)
                {
                    String displayMessage = this.srvObject.processClientRequest(inputCmd[0] + "@" + inputCmd[1]);
                    System.out.println(displayMessage);
                }
                else if (0 == inputCmd[0].compareTo("dir") || 0 == inputCmd[0].compareTo("shutdown"))
                {
                    if (inputCmd.length == 1)
                    {
                        String displayMessage = this.srvObject.processClientRequest(inputCmd[0]+"@");
                        System.out.println(displayMessage);
                    }
                }
                break;
            }
            default: {
                System.out.println("Error 400: BAD_REQUEST Invalid command");
                break;
            }
          }
        }
        catch(RemoteException e)
        {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    
    /**
     * reads and writes file from server to client
     * @param serverFilePath and clientFilePath
     * @return void
     */
    private void downloadServerFile(String serverFilePath, String clientFilePath) {
        try
        {
            File file = new File(clientFilePath);
            FileOutputStream fos = null;
            int byte_Download = 0;
            int byte_Pending = this.srvObject.getFileLengthOfServer(serverFilePath);
            
            //condtion if file is still not completely downloaded
            if (byte_Pending > 0) 
            {
            	//check if file exists and get the length of file
                if (file.exists())
                	byte_Download = (int)file.length();

                if (byte_Download > 0 && byte_Download < byte_Pending)
                	fos = new FileOutputStream(file, true);
                else
                {
                    byte_Download = 0;
                    fos = new FileOutputStream(file, false);
                }

                BufferedOutputStream bos = new BufferedOutputStream(fos);
                byte[] byteArray = this.srvObject.downloadServerFile(serverFilePath, byte_Download);

                //condition to keep reading and writing the file at client path
                while (byteArray != null && byteArray.length > 0)
                {
                    bos.write(byteArray, 0, byteArray.length);
                    bos.flush();
                    byte_Download += byteArray.length;
                    byteArray = this.srvObject.downloadServerFile(serverFilePath, byte_Download);
                    float trackProgress = (float) byte_Download / (float)byte_Pending * (float) 100.0;
                    System.out.print("\r");
                    System.out.print("downloading file ... " + ((int) trackProgress) + "%");
                }

                if (byte_Download > 0)
                	System.out.println("\n Download completed");
                else
                	System.out.println("\n Error: Download failed, try again");
                fos.close();
            }
            
            //condition if file not found
            else
            	System.out.println("Error 404: FILE NOT_FOUND");
        } 
        catch (Exception e)
        {
            System.out.println("\nError 400: BAD_REQUEST Downloading failed");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    
    /**
     * read and send file from client to server
     * @param serverFilePath and clientFilePath
     * @return void
     */
    private void uploadClientFile(String serverFilePath, String clientFilePath) {
        try
        {
            int getServerFileSize = this.srvObject.getFileLengthOfServer(serverFilePath);

            File file = new File(clientFilePath);
            
            //check if file exists at client path which is to be uploaded
            if (file.exists())
            {
                FileInputStream fis = new FileInputStream(file);
                int getFileLength = (int)file.length();
                int byte_Read = 0;

                boolean isAppendFlag = false;
                
                //condition if file to be uploaded is partially present at server path
                if (getServerFileSize > 0 && getServerFileSize < getFileLength)
                {
                    fis.skip(getServerFileSize);
                    isAppendFlag = true;
                    byte_Read = getServerFileSize;
                }
                byte[] byteArr = new byte[1024];
                boolean succFlag = true;

                //condition to read the file and send to the server for writing to the server path
                while(succFlag && byte_Read < getFileLength)
                {
                    byte_Read += fis.read(byteArr);
                    succFlag = this.srvObject.uploadClientFile(serverFilePath, byteArr, isAppendFlag);
                    isAppendFlag = true;
                    float trackProgress = (float) byte_Read / (float) getFileLength * (float) 100.0;
                    System.out.print("\r");
                    System.out.print("uploading file ... " + ((int) trackProgress) + "%");
                }
                
                //condition to check if file has been transferred completely
                if (succFlag)
                	System.out.println("\n File uploaded successfully");
                else
                	System.out.println("\n Error 400: BAD_REQUEST Uploading failed");
                fis.close();

            }
            else
            	System.out.println("Error 404: FILE NOT_FOUND");
        }
        catch (Exception e)
        {
            System.out.println("Error 400: BAD_REQUEST Uploading failed");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
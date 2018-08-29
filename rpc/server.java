import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

public class server extends UnicastRemoteObject implements serverInterface {

	int countClient=0;

    server() throws RemoteException {}
        
    /**
     * server runs from here
     * @param String args[]
     * @return void
     */
    public static void main(String[] args) {
        try
        {
            if (args.length >= 1 && 0 == args[0].compareTo("start"))
            {
                server srv = new server();
                Naming.bind("rmi://localhost/FileServer", srv);
                System.out.println("Server is up and running");
            }
            else 
            	System.out.println("Error: Start server as 'start <port #>'");
        }
        catch (Exception e) {
            System.out.println("Error loading server");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    /**
     * read client request and call specific method
     * @param command
     * @return void
     */
    public String processClientRequest(String command) throws RemoteException {
        String displayMessage = "Error 400: BAD_REQUEST";
        try
        {
        	countClient++;
            String[] inputCmd = command.split("@");
            if(inputCmd.length <= 0)
            	return displayMessage;
            
            //check for the operation to perform
            switch (inputCmd[0]) {
                case "dir": {
                    if (inputCmd.length <= 1)
                    	displayMessage = this.displayDirectoryFiles("./");
                    else
                    	displayMessage = this.displayDirectoryFiles(inputCmd[1]);
                    break;
                }
                case "shutdown": {
                    displayMessage = this.shutDown();
                    break;
                }
                case "mkdir": {
                    if (inputCmd.length > 1)
                    	displayMessage = this.createDirectory(inputCmd[1]);
                    break;
                }
                case "rmdir": {
                    if (inputCmd.length > 1)
                    	displayMessage = this.deleteDirectory(inputCmd[1]);
                    break;
                }
                case "rm": {
                    if (inputCmd.length > 1)
                    	displayMessage = this.deleteFile(inputCmd[1]);
                    break;
                }
                default: {
                    System.out.println("Error 400: BAD_REQUEST");
                    break;
                }
            }
        	countClient--;
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return displayMessage;
    }


    /**
     * calculate the length of the file which will be used for operation
     * @param getFilePath
     * @return int getFileSize
     */
    public int getFileLengthOfServer(String getFilePath) throws RemoteException {
        int getFileSize = -1;
        try
        {
            File file = new File(getFilePath);
            
            //condition if file already exist get the size of file
            if (file.exists())
            	getFileSize = (int)file.length();
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return getFileSize;
    }

    
    /**
     * read file and send to client for downloading
     * @param String getFilePath int byte_Download
     * @return byte[] byteArray
     */
    public byte[] downloadServerFile(String getFilePath, int byte_Download) throws RemoteException {
        byte[] byteArray = null;
        try
        {
        	countClient++;
            File file = new File(getFilePath);
            
            //condition when file already exist
            if (file.exists())
            {
                FileInputStream fis = new FileInputStream(file);
                
                //if partial file exists skip that length
                if (byte_Download > 0)
                	fis.skip(byte_Download);
             
                byteArray = new byte[1024];
                int store = fis.read(byteArray);
                if (store <= 0)
                	byteArray = null;
                fis.close();
            }
            countClient--;
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return byteArray;
    }


    /**
     * read file from client and write file to specified location at server
     * @param String getFilePath, byte[] bytes, boolean isAppendFlag
     * @return boolean value
     */
    public boolean uploadClientFile(String getFilePath, byte[] bytes, boolean isAppendFlag) throws RemoteException {
        try
        {
        	countClient++;
            File file = new File(getFilePath);
            FileOutputStream fos = new FileOutputStream(file, isAppendFlag);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            
            //write the bytes send from client
            bos.write(bytes, 0, bytes.length);
            bos.flush();
            fos.close();
            countClient--;
            return true;
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    
    /**
     * displays files present in specified directory
     * @param String getDirPath
     * @return String displayMsg
     */
    private String displayDirectoryFiles(String getDirPath) {
        String displayMsg = "";
        try
        {
            File currFile = new File(getDirPath);
            String listFileName = "";
            
            //condition if current directory exists display all files
            if (currFile.exists())
            {
                String[] files = new File(getDirPath).list();
                listFileName = "Root Directory: /";
                for (String file : files)
                	listFileName = listFileName + "\n" + file;
                displayMsg = listFileName;
            }
            
            //condition if directory not found
            else
            {
                displayMsg = "Error 404: NOT_FOUND";
                return displayMsg;
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return displayMsg;
    }

    
    /**
     * delete file at specified path if it exists
     * @param String getFilePath
     * @return String displayMsg
     */
    private String deleteFile(String getFilePath) {
        String displayMsg = "";
        try
        {
            File currFile = new File(getFilePath);
            
            //condition to delete file if it exists
            if (currFile.exists())
            {
                if (currFile.listFiles() != null && currFile.listFiles().length > 0)
                {
                     displayMsg = "Error 404: NOT_FOUND";
                     return displayMsg;
                }
                currFile.delete();
                displayMsg = "File deleted successfully";
                return displayMsg;
            }
            
            //condition when file does not exists
            else
            {
                displayMsg = "Error 404: NOT_FOUND";
                return displayMsg;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return displayMsg;
    }


    /**
     * delete directory at specified path if not empty
     * @param String getDirPath
     * @return String displayMsg
     */
    private String deleteDirectory(String getDirPath) {
        String displayMsg = "";
        try
        {
            File thisDirectory = new File(getDirPath);
            
            //condition to delete directory if it exists
            if (thisDirectory.exists())
            {
            	//condition to check if directory contains file
                if (thisDirectory.listFiles().length > 0)
                	displayMsg = "Error 403: Forbidden Directory not empty";
                
                //if directory does not contain files delete directory
                else 
                {
                    thisDirectory.delete();
                    displayMsg = "Directory deleted successfully";
                }
            }
            
            //condition when directory does not exists
            else
            	displayMsg = "Error 404: NOT_FOUND";
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return displayMsg;
    }

    
    /**
     * create directory at server side
     * @param String getDirPath
     * @return String displayMsg
     */
    private String createDirectory(String getDirPath) {
        String displayMsg = "";
        try
        {
            File thisDirectory = new File(getDirPath);
            
            //condition when directory does not exists
            if (!thisDirectory.exists())
            {
                thisDirectory.mkdir();
                displayMsg = "Directory created successfully";
            } 
            
            // condition when directory already exists
            else
            	displayMsg = "Error 409: Conflict Resource already exists";
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return displayMsg;
    }
    
    
    /**
     * Shutdown server if its in idle state
     * @param void
     * @return String message
     */
    private String shutDown()
    {
        try
        {
        	//condition when server is busy with more than one client
        	if(countClient>1)
        	{
        		System.out.println("server is currently busy serving other clients");
        		while(true)
        		{
        			//condition when server is now only serving one client
        			if(countClient<=1)
        			{
                        Naming.unbind("rmi://localhost/FileServer");
                        UnicastRemoteObject.unexportObject(this, true);
                        return "Server shutdown successfully";
        			}
        		}
        	}
        	
        	//server is busy with only one client
        	else
        	{
                Naming.unbind("rmi://localhost/FileServer");
                UnicastRemoteObject.unexportObject(this, true);
                return "Server shutdown successfully";
        	}
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return "Error 400: BAD_REQUEST";
    }
}
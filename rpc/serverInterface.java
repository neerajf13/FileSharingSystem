import java.io.BufferedInputStream;
import java.rmi.*;

public interface serverInterface extends java.rmi.Remote {

    public String processClientRequest(String command) throws RemoteException;

    public int getFileLengthOfServer(String getFilePath) throws RemoteException;

    public byte[] downloadServerFile(String getFilePath, int byte_Download) throws RemoteException;

    public boolean uploadClientFile(String getFilePath, byte[] bytes, boolean isAppendFlag) throws RemoteException;

}



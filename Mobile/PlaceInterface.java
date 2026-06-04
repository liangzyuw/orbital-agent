package Mobile;
import java.rmi.*;

public interface PlaceInterface extends Remote {
    public boolean transfer(String classname, byte[] bytecode, byte[] entity) throws RemoteException;

    public StationData getStationData() throws RemoteException;
}
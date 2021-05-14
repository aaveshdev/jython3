package javatests;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.concurrent.Callable;


public class ProxyDeserialization {

    public static void main(String[] args) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(args[0]));
            Callable cat = (Callable) ois.readObject(); // abuse! obviously not concurrent
            cat.call();
        }
        catch(Exception e) {
            System.err.println(e);
        }
    }
}

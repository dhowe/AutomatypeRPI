package automatype.net;

import java.io.*;
import java.net.*;
import java.util.*;

import automatype.Automatype;
import automatype.net.MulticastClient.NetWatcher;

public class MulticastClient extends Thread
{
  static int NETWORK_TIMEOUT = 50000;
  static int WATCHER_PAUSE = 5000;
  static String GROUP = "230.0.0.1";
  static int PORT = 4446;
  
  long lastNetworkUpdate;
  MulticastSocket socket;
  InetAddress address;
  boolean running = true;
  MulticastListener listener;
  NetWatcher watcher;
  boolean foundNetwork;

  public MulticastClient() 
  {
    this(null);
  }
  
  public MulticastClient(MulticastListener mr) 
  {
    this.listener = mr;
    try
    {
      socket = new MulticastSocket(PORT);
      address = InetAddress.getByName(GROUP);
      socket.joinGroup(address);
    }
    catch (Exception e)
    {
      Automatype.onError(e);
    }
  }
  
 
  @Override
  public synchronized void start()
  {
   
    super.start();
    (watcher = new NetWatcher()).start();
  }

  public void run()
  {
    DatagramPacket packet;

    try
    {
      while (running)
      {
          byte[] buf = new byte[256];
          packet = new DatagramPacket(buf, buf.length);
          socket.receive(packet);

          String received = new String(packet.getData(), 0, packet.getLength());
          lastNetworkUpdate = System.currentTimeMillis();
          if (listener.getAutoMode()) {
            System.err.println("[WARN] New UDP data, switching to network-mode...");
            listener.setAutoMode(false);
          }
          
          //System.out.println("Id: " + received);
          if (this.listener != null) {
            int id = -1;
            try
            {
              id = Integer.parseInt(received);
            }
            catch (Exception e)
            {
              Automatype.onError(e);
            }
            if (listener.getId() == id) {
              if (!listener.getAutoMode())
                if (!foundNetwork) {
                  foundNetwork = true;
                  System.out.println("[INFO] *** Receiving network messages ***");
                }
                listener.trigger();
            }
          }
          sleep(10);
      }

    }
    catch (Exception e)
    {
      running = false;
      Automatype.onError(e);
    }    
    finally 
    {
      try
      {
        socket.leaveGroup(address);
        socket.close();
      }
      catch (IOException e)
      {
        System.err.println(e.getMessage());
      }
    }
  }
  
 class NetWatcher extends Thread {
    
    boolean watching = true;

    public void run()
    {
      while (watching) {
        try
        {
          sleep(WATCHER_PAUSE);
          
          if (!listener.getAutoMode()) { 
            
            if (System.currentTimeMillis() - lastNetworkUpdate > NETWORK_TIMEOUT) {
              System.err.println("[WARN] No network activity in "+NETWORK_TIMEOUT+", starting auto-mode");
              listener.setAutoMode(true);
            }
            /*else {
              System.out.println("Watcher: OK");
            }*/
          }
         /* else { // auto-mode
            long now = System.currentTimeMillis();
            long elapsed = now - lastNetworkUpdate;
            if (elapsed < NETWORK_TIMEOUT) {
              System.err.println("[WARN] New network data after "+elapsed+"ms, stopping auto-mode");
              listener.setAutoMode(false);
            }
          }*/
        }
        catch (InterruptedException e) {}
      }
    }
  }
}

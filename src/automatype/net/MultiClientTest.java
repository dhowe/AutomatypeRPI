package automatype.net;

import java.io.IOException;
import java.net.*;

public class MultiClientTest extends Thread
{
  private boolean running = true;
  private int id;

  public void setIdFromIp()
  {
    String ip = getIpAddress();
    String[] split = ip.split("\\.");
    String lastByte = split[split.length-1];
    String idStr = lastByte.substring(lastByte.length()-1);
    try
    {
      this.id = Integer.parseInt(idStr);
      System.out.println("[INFO] Id reset via IpAddress Id#="+id);
    }
    catch (NumberFormatException e)
    {
      System.err.println(e.getMessage());
    }
  }
  
  public static String getIpAddress()
  {
    try
    {
      InetAddress addr = InetAddress.getLocalHost();
      String hostAddress = addr.getHostAddress();
      System.out.println("[INFO] "+hostAddress);
      return hostAddress;
    }
    catch (UnknownHostException e)
    {
      e.printStackTrace();
    }
    return null;
  }
  
  public void run()
  {
    setIdFromIp();
    try
    {
      MulticastSocket socket = new MulticastSocket(4446);
      InetAddress group = InetAddress.getByName("230.0.0.1");
      socket.joinGroup(group);
      System.out.println("Client joined...");
      
      while (running )
      {
          byte[] buf = new byte[256];
          DatagramPacket packet = new DatagramPacket(buf, buf.length);
          socket.receive(packet);
          String received = new String(packet.getData());
          System.out.println("Received " + received);
      }

      socket.leaveGroup(group);
      socket.close();
    }
    catch (Exception e)
    {
      running = false;
      e.printStackTrace();
    }
  }
  
  public static void main(String[] args)
  {
    new MultiClientTest().start();
  }
}

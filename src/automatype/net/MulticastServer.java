package automatype.net;

import java.io.*;
import java.net.*;
import java.util.*;

public class MulticastServer extends Thread
{
  static long MIN_PERIOD = 250, MAX_PERIOD = 250;
  static final String GROUP_NAME = "230.0.0.1";
  static final int PORT = 4446;

  static final String DQ = "";
  static int NUM_CLIENTS = 9;

  protected DatagramSocket socket = null;
  protected BufferedReader in = null;
  protected boolean running = true;
  private long packetCount = 0;

  public MulticastServer() throws IOException
  {
    socket = new DatagramSocket(4445);
  }

  public void run()
  {
    System.out.println("[INFO] Server started...");
    while (running)
    {
      
      try
      {
        byte[] buf = new byte[256];

        // construct quote
        String dString = getNextId();

        buf = dString.getBytes();

        // send it
        InetAddress group = InetAddress.getByName(GROUP_NAME);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, PORT);
        socket.send(packet);
        
        if (++packetCount % 500 == 499)
          System.out.println("[INFO] Sending: packet #" + (packetCount+1));

        // sleep for a while
        try
        {
          sleep((long) (MIN_PERIOD + (Math.random() * (MAX_PERIOD - MIN_PERIOD))));
        }
        catch (InterruptedException e)
        {
        }
      }
      catch (IOException e)
      {
        onError(e);
        // running = false;
      }
    }
    socket.close();
  }

  /**
   * Returns a randomly ordered array of unique integers from 1 to
   * <code>numElements</code>. The size of the array will be
   * <code>numElements</code>.
   */
  public static List randomOrdering(Stack<Integer> tmp, int numElements)
  {
    tmp.clear();

    int[] result = new int[numElements];
    // List tmp = new LinkedList();
    for (int i = 1; i <= result.length; i++)
      tmp.add(new Integer(i));
    Collections.shuffle(tmp);
    return tmp;
  }

  protected Stack<Integer> ids = new Stack<Integer>();

  protected String getNextId()
  {
    if (ids.isEmpty())
      randomOrdering(ids, NUM_CLIENTS);
    return DQ + ids.pop();
  }

  protected String getNextRandomId()
  {
    return DQ + ((int) (Math.random() * NUM_CLIENTS));
  }

  public static void onError(Exception e)
  {
    System.err.println("[ERROR] " + e != null ? e.getMessage() : "unknown");
  }

  public static void main(String[] args) throws IOException
  {

    if (args != null && args.length == 1)
    {
      try
      {
        MIN_PERIOD = MAX_PERIOD = Integer.parseInt(args[0]);
        System.out.println("[OPTS] Setting period=" + MIN_PERIOD);
      }
      catch (NumberFormatException e)
      {
        onError(e);
      }
    }
    new MulticastServer().start();
  }

}
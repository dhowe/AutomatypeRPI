package automatype;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Enumeration;

import processing.core.PApplet;
import processing.core.PFont;
import rita.*;
import automatype.Sample.Type;
import automatype.net.MulticastClient;
import automatype.net.MulticastListener;

public class Automatype extends PApplet implements TransformConstants, MulticastListener
{
  static String VERSION = "49";
  static String OS = System.getProperty("os.name");
  static String FONT = "Courier", NET_IF = "etho0";
  static float[] WHITE = {255,245,245,255};
  static float[] FLASH_COLOR = {0,0,0};
  static boolean USE_NETWORK = true;
  static boolean BORDERED = false;
  static boolean GRADIENTS = false;
  static boolean FULLSCREEN = true;
  static boolean ID_FROM_IP = true;
  static boolean PRODUCTION = true;
  static int FONT_SZ = 135;
  
  ///////////////////////////////////////////////////
  
  static Sample bell, type;
  static int id=1, cellW, cellH, GRID_X_OFF, gridYoff;
  static LexiconLookup lexLook;
  static long lastNetworkUpdate;
  public static boolean autoMode = false;

  ///////////////////////////////////////////////////

  CursoredRiText word;
  MulticastClient network;
  WordTransformCell[] cells;
  boolean running, started = true;

  public void setup()
  {
    noCursor();
    
    size(680, 490); 
    
    System.out.println("[INFO] Automatype.version ["+VERSION+"]");

    lexLook = new LexiconLookup(this, HISTORY_SIZE);
    bell = Sample.create(this, "bell.wav");
    type = Sample.create(this, "key.wav");

    if (USE_NETWORK) {
      
      if (ID_FROM_IP) this.setIdFromIp();
      (network = new MulticastClient(this)).start();
      autoMode = false;
    }
    
    this.computeSizes();
    this.createCells();
  }

  public static String fontName()
  {
    return FONT+"-"+FONT_SZ+".vlw";
  }

  private void createCells()
  {
    if (TEST_MANUAL_EVENTS) autoMode = false; 
      
    RiText.DEFAULT_FONT = fontName();
    cells = new WordTransformCell[NUM_CELLS];
    int idx = 0;
    int ypos = gridYoff;
    for (int j = 1; j <= NUM_ROWS; j++)
    {
      for (int i = 0; i < NUM_COLS; i++)
      {
        String word1 = lexLook.getRandomWord(7);//.MIN_WORD_LEN,MAX_WORD_LENGTH);
        Rectangle r = new Rectangle(Math.round(GRID_X_OFF + (i * cellW)), ypos, cellW, cellH);
        try
        {
          System.out.println("[INFO] Loading "+fontName()+" from filesystem");
          _loadFont(this, FONT+"-"+FONT_SZ+".vlw");
        }
        catch (Throwable e)
        {
          System.out.println("[INFO] Failed! Creating font: "+FONT+"-"+FONT_SZ);
          long ts = System.currentTimeMillis();
          PFont pf = createFont(FONT, FONT_SZ);
          if (!pf.getPostScriptName().equals(FONT)) {
            System.err.println("[INFO] Failed to create: "+FONT
                +"-"+FONT_SZ+" reverting to Dialog...");
          }
          RiText.createDefaultFont(FONT, FONT_SZ);
        }
        cells[idx] = new WordTransformCell(this, word1, r, getId());
      }
      ypos += cellH;
    }
  }
  

  public void computeSizes()
  {
    cellW = width;
    cellH = height;
    if (BORDERED) 
    {
      cellW = width-30;
      cellH = height-30;
      GRID_X_OFF = 15;
      gridYoff = 15;
    }
  }

  public void draw()
  {
    background(255);
    for (int i = 0; cells != null && i < cells.length; i++)
    {
      cells[i].draw(this);
    }
  }

  public void mouseClicked()
  {
    if (TEST_MANUAL_EVENTS)
      cells[0].nextEdit(lexLook);
  }

  public void keyPressed()
  {
    if (TEST_MANUAL_EVENTS && key == ' ')
      cells[0].nextEdit(lexLook);
    else
      started = true;
  }
  
  

  public void onRiTaEvent(RiTaEvent re)
  {
    if (!started) return;
    
    //System.out.println("Automatype.onRiTaEvent("+re+")");
    
    try
    {
      String name = re.getName();
      if (name.startsWith(UPDATE_PRE))
      {
        name = name.substring(UPDATE_PRE.length());
        int idx = Integer.parseInt(name);
        idx = Math.min(idx, cells.length-1); 
        //System.out.println("onRiTaEvent.idx="+idx);
        if (idx > -1 && idx < cells.length)
          cells[idx].nextEdit(lexLook);
        else
          throw new RiTaException("Unexpected event-tag: " + re);
      }
    }
    catch (Exception e)
    {
      //System.err.println("ERROR: "+e.getMessage());
      onError(e);
    }
  }

  // undecorated frame
  public void init()
  {
    frame.removeNotify();
    frame.setUndecorated(true);
    frame.addNotify();
    super.init();
  }
  
  private static int parseIntArg(String arg, int tagLen)
  {
    try
    {
      return Integer.parseInt(arg.substring(tagLen).trim());
    }
    catch (Exception e)
    {
      System.err.println(e.getMessage());
      System.err.println("Ignoring arg: "+arg);
      return -1;
    }
  }
  
  protected static PFont _loadFont(PApplet pApplet, String fname)
  {
    PFont pf = null;
    try
    {
      // System.out.println("looking for font: "+fontFileName);
      InputStream is = RiTa.openStream(pApplet, fname);
      pf = fontFromStream(is, fname);
    }
    catch (Throwable e)
    {
      throw new RiTaException("Could not load font '" + fname + "'");
    }
    return pf;
  } 
  
  private static PFont fontFromStream(InputStream is, String name)
  {
    // System.out.println("fontFromStream("+name+")");
    try
    {
      return new PFont(is);
    }
    catch (IOException e)
    {
      throw new RiTaException("creating font from stream: " + is + " with name=" + name);
    }
  }
  
  public static int[] unpack(int pix)
  {
    int a = (pix >> 24) & 0xff;
    int r = (pix >> 16) & 0xff;
    int g = (pix >> 8) & 0xff;
    int b = (pix) & 0xff;
    return new int[] { a, r, g, b };
  }
  
  public int getId()
  {      
    return id;
  }
  
  public void setIdFromIp()
  {
    String ip = getBestIpAddress();
    if (ip == null) {
      System.out.println("[WARN] No ip address found!!!");
      return;
    }
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

  public void trigger()
  {
    if (cells != null && cells.length > 0) {
      if (lexLook != null) {
        while (cells[0] == null) {
          
          //System.out.print("[INFO] waiting...");
          try
          {
            Thread.sleep(50);
          }
          catch (InterruptedException e)
          {
          }
        }

          //throw new RuntimeException("NO CELL!!!");
        cells[0].nextEdit(lexLook);
      }
    }
  }
  
  public void setAutoMode(boolean b)
  {
    if (autoMode == b) return;
    
    autoMode = b;
    for (int i = 0; cells!=null && i < cells.length; i++)
      cells[i].setAutoMode(b);
  }


  public boolean getAutoMode()
  {
    return autoMode;
  }
  
  public static String getBestIpAddress()
  {
    Enumeration<NetworkInterface> nics = null;
    try
    {
      nics = NetworkInterface.getNetworkInterfaces();
    }
    catch (SocketException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    while (nics.hasMoreElements()) {
      NetworkInterface ni = nics.nextElement();
      String ifname = ni.getName();
      String ip = getIpAddress(ifname);
      if (ip.equals("127.0.0.1") || ip.equals("localhost"))
        continue;
      return ip;
      //System.out.println(ifname+" "+getIpAddress(ifname));
    }
    
    return null;
  }

  public static String getIpAddress(String ifname)
  {
    Enumeration<InetAddress> inetAddresses = null;
    try
    {
      NetworkInterface ni = NetworkInterface.getByName(ifname);
      if (ni == null) throw new RuntimeException("IF: '"+ifname+"' not found!");
      inetAddresses = ni.getInetAddresses();
    }
    catch (SocketException e)
    {
      System.err.println("[WARN] Automatype.getIpAddress() :: "+e.getMessage());
    }
    
    while (inetAddresses.hasMoreElements())
    {
      InetAddress ia = inetAddresses.nextElement();
      if (!ia.isLinkLocalAddress())
      {
        return ia.getHostAddress();
      }
    }
    return "127.0.0.1"; // default
  }

  public static String getIpAddress2()
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
      onError(e);
    }
    return null;
  }
  
  public static void parseOpts(String[] args)
  {
    if (OS.startsWith("Mac")) {
      Sample.type = Type.MINIM;
      GRADIENTS = true;
    }
    
    for (int i = 0; i < args.length; i++)
    {
      if (args[i].equals("-b")) {
        BORDERED = true;
      }
      else if (args[i].startsWith("-c")) {
        String hex = args[i].substring(2).trim();
        int[] col = unpack(unhex(hex));
        WHITE = new float[] { col[1], col[2], col[3], 255 };
      }
      /*else if (args[i].startsWith("-c")) {
        String hex = args[i].substring(2).trim();
        int[] col = unpack(unhex(hex));
        FLASH_COLOR = new float[] { col[1], col[2], col[3] };
      }*/
      else if (args[i].startsWith("-c")) {
        String hex = args[i].substring(2).trim();
        int[] col = unpack(unhex(hex));
        FLASH_COLOR = new float[] { col[1], col[2], col[3] };
      } 
      else if (args[i].equals("-g")) {
        GRADIENTS = true;
      }
      else if (args[i].equals("-ng")) {
        GRADIENTS = false;
      }
      else if (args[i].equals("-np")) {
        PRODUCTION = false;
      }
      else if (args[i].equals("-m")) {
        Sample.type = Type.MINIM;
      }
      else if (args[i].equals("-nm")) {
        Sample.type = Type.NATIVE;
      }
      else if (args[i].startsWith("-f")) {
        FONT = args[i].substring(2).trim();
      }
      else if (args[i].startsWith("-if")) {
        NET_IF = args[i].substring(3).trim();
      }
      else if (args[i].equals("-nfs")) {
        FULLSCREEN = false;
      }
      else if (args[i].startsWith("-s")) {
        int iarg = parseIntArg(args[i],2);
        if (iarg>-1) FONT_SZ = iarg;
      }
      else if (args[i].startsWith("-net")) {
        USE_NETWORK = true;
      }
      else if (args[i].equals("-nnet")) {
        USE_NETWORK = false;
      }
      else if (args[i].startsWith("-i")) {
        int iarg = parseIntArg(args[i],2);
        if (iarg>-1) id = iarg;
      }
      else if (args[i].startsWith("-noip")) {
        ID_FROM_IP = false;
      }
    }
    
    System.out.println("[OPTS] Id#="+id);
    //System.out.println("[OPTS] Border="+BORDERED);
    System.out.println("[OPTS] Network="+USE_NETWORK);
    System.out.println("[OPTS] IdFromIP="+ID_FROM_IP);
    System.out.println("[OPTS] Gradients="+GRADIENTS);
    //System.out.println("[OPTS] FullScreen="+FULLSCREEN);
    System.out.println("[OPTS] Production="+PRODUCTION);
    //System.out.println("[OPTS] AudioLib="+Sample.type);
    //System.out.println("[OPTS] Font="+FONT+"-"+FONT_SZ);
    System.out.println("[OPTS] BgColor="+RiTa.asList(WHITE));
    //System.out.println("[OPTS] Color="+RiTa.asList(FLASH_COLOR));
  }
  
  public static void onError(Exception e)
  {
    System.err.println("[ERROR] "+e != null ? e.getMessage() : "unknown");
    if (!PRODUCTION)
      throw new RuntimeException(e);
  }
  
  public static void main(String[] args)
  {
    //System.out.println("Test Ok?"+getBestIpAddress().equals(getIpAddress("en0")));
    System.out.println("[INFO] "+System.getProperty("user.dir"));
    if (OS.startsWith("Mac"))  
      args = new String[] { "-ng", "-np", "-nfs", "-net","-i2", "-noip", "-cffffff" };
    
    if (args != null) parseOpts(args);
    
    args = FULLSCREEN ? 
        new String[] { "--present", "--bgcolor=#ffffff", "--hide-stop", Automatype.class.getName() } :
        new String[] { Automatype.class.getName() };
        
    PApplet.main(args);
  }

  
}// end

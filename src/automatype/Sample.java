package automatype;

import processing.core.PApplet;
import rita.RiTa;

public abstract class Sample
{
  public enum Type { NATIVE, MINIM }
  
  public static Type type = Type.NATIVE;
  private static boolean firstCreation = true;
  
  public static Sample create(Object p, String fname)
  {
    switch (type)
    {
      case MINIM:
        return new P5LibSample(RiTa.loadSample((PApplet) p, fname));

      default: 
        return new NativeSample(fname);
    }
  }
  
  public abstract void play();
}

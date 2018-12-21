package automatype;

import rita.RiSample;

public class P5LibSample extends Sample
{
  private RiSample rs;

  public P5LibSample(RiSample rs)
  {
    this.rs = rs;
  }

  public void play()
  {
    if (rs != null)
      rs.play();
  }

  public void setVolume(float i)
  {
    if (rs != null)
      rs.setVolume(i);
  }
}

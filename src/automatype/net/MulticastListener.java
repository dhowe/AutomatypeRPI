package automatype.net;

public interface MulticastListener
{
  int getId();
  
  void trigger();
  
  void setAutoMode(boolean b);
  
  boolean getAutoMode();
}

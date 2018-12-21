package automatype;

import java.io.*;
import java.util.concurrent.*;

import processing.core.PApplet;

public class NativeSample extends Sample
{
  protected static final String CR = "\n";

  private static PrintWriter OUT = new PrintWriter(System.out); 
  private static PrintWriter ERR = new PrintWriter(System.err);

  static String executable = Automatype.OS.startsWith("Mac") ? "afplay" : "aplay";
  static ExecutorService threads =  Executors.newFixedThreadPool(5);
  
  private String sampleFile;
  private Runtime runtime;
  private Future handle;

  
  public NativeSample(String file)
  {
    this.runtime = Runtime.getRuntime();
    this.sampleFile = file;
  }

  public void play()
  {

    if (handle != null) handle.cancel(false);
    
    handle = threads.submit(new Runnable()
    {
      public void run()
      {
        try
        {
          Process proc = runtime.exec(executable + " " + sampleFile);
          listen(proc, null, null, ERR);
        }
        catch (IOException e)
        {
          System.err.println(e != null ? e.getMessage() : "no message");
        }
      };
      
    });
  }

  private static void listen(Process process, final Reader _in, final Writer out, final Writer err)
  {
    // Buffer the input reader
    final BufferedReader in = _in != null ? new BufferedReader(_in) : null;
    final BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
    final BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    final BufferedWriter stdIn = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

    // Thread that reads std out and feeds the writer given in input
    if (out != null) {
      new Thread()
      {
        @Override
        public void run()
        {
          String line;
          try
          {
            while ((line = stdOut.readLine()) != null)
            {
              out.write(line + CR);
            }
          }
          catch (Exception e)
          {
            throw new Error(e);
          }
          try
          {
            out.flush();
            out.close();
          }
          catch (IOException e)
          { /* Who cares ? */
          }
        }
      }.start(); // Starts now
    }

    // Thread that reads std err and feeds the writer given in input
    if (err != null) {
      new Thread()
      {
        @Override
        public void run()
        {
          String line;
          try
          {
            while ((line = stdErr.readLine()) != null)
            {
              err.write(line + CR);
            }
          }
          catch (Exception e)
          {
            throw new Error(e);
          }
          try
          {
            err.flush();
            err.close();
          }
          catch (IOException e)
          { /* Who cares ? */
          }
        }
      }.start(); // Starts now
    }

    // Thread that reads the std in given in input and that feeds the input of the process
    if (in != null) {
      new Thread()
      {
  
        @Override
        public void run()
        {
          String line;
          try
          {
            while ((line = in.readLine()) != null)
            {
              stdIn.write(line + CR);
            }
          }
          catch (Exception e)
          {
            throw new Error(e);
          }
  
          try
          {
            stdIn.flush();
            stdIn.close();
          }
          catch (IOException e)
          { /* Who cares ? */
          }
        }
      }.start(); 
    }

    // Wait until the end of the process
    try
    {
      process.waitFor();
    }
    catch (Exception e)
    {
      throw new Error(e);
    }

  }

  public static void main(String[] args)
  {
    new NativeSample("key.wav").play();
  }
}

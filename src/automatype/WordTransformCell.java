package automatype;

import java.awt.Rectangle;
import java.util.concurrent.*;

import processing.core.PApplet;
import rita.*;

class WordTransformCell implements TransformConstants
{
  float x, y;
  char nextChar;
  boolean readyForReplace, firstWord = true;
  int id, nextCursPos, nextAction = REPLACE_ACTION;
  RiText target, bg;
  CursoredRiText word;
  Automatype parent;
  ScheduledExecutorService threads; // static??
  ScheduledFuture scheduledFuture;
  private RiTextBehavior timer;

  WordTransformCell(Automatype p, String word1, Rectangle r, int id)
  {
    this(p, word1, r.x, r.y, r.width, r.height, id);
  }
  
  WordTransformCell(Automatype p, String word1, float x, float y, float w, float h, int id)
  {
    //System.out.println("WordTransformCell: "+x+","+y+","+w+","+h+","+id);
    
    this.x = x;
    this.y = y;
    this.id = id;
    this.parent = p;
    this.word = new CursoredRiText(p, word1);
    word.setLocation(x + w/2, y + (h  * .6f));
    this.target = new RiText(p);
    this.target.fill(0, 0, 0, SHOW_TARGETS ? 30 : 0);
    this.bg = new RiText(p);
    bg.fill(Automatype.BG_COL); // hack for bg fades
    
    if (parent.autoMode) {
      setAutoMode(true);
    }
    if (!Automatype.GRADIENTS && threads == null)
      threads = Executors.newScheduledThreadPool(5);
  }
  
  void setAutoMode(boolean b)
  {
    //System.out.println("WordTransformCell.setAutoMode("+b+")");
    if (b)
      timer = RiTa.setCallbackTimer(parent, getTimerName(), INITIAL_KEY_PAUSE);
    else {
      if (timer != null) timer.delete();
    }
  }

  void draw(PApplet p)
  {
      float[] bgfill = bg.getColor();
      p.fill(bgfill[0], bgfill[1], bgfill[2], bgfill[3]);
      
      p.stroke(0);
      p.strokeWeight(STROKE_WEIGHT);
      if (!Automatype.BORDERED) p.noStroke();
      p.rect(x, y, Automatype.cellW, Automatype.cellH);
  }

  void nextEdit(LexiconLookup ll)
  {
    if (target.length() < 1)
    {
      pickNextTarget(ll);
      findNextEdit();
      return;
    }

    switch (nextAction)
    {
      case DELETE_ACTION:
        doInsertOrDelete(false);
        break;
      case INSERT_ACTION:
        doInsertOrDelete(true);
        break;
      default: // REPLACE
        doReplace();
    }
  }

  private void onWordCompletion()
  {
    firstWord = false;
    target.setText(""); // empty

    //bg.fill(Automatype.FLASH_COLOR);
    //System.out.println("fill: "+(id + 1) * 10+","+ (id + 1) * 6+","+ (id + 1) * 3);
    
    int k = 1+((id + 4) % 9);
    bg.fill(k * 10, k * 6, k * 3);
    word.fill(Automatype.BG_COL[0],Automatype.BG_COL[1],Automatype.BG_COL[2],Automatype.BG_COL[3]);

    //Automatype1.type.play();
    Automatype.bell.play();
    
    if (Automatype.GRADIENTS) {
      bg.fadeColor(Automatype.BG_COL, SUCCESS_PAUSE);
      word.fadeColor(0, SUCCESS_PAUSE);
    }
    else {
      if (scheduledFuture != null) {
        scheduledFuture.cancel(true);
      }
      scheduledFuture = threads.schedule
        (new Runnable()
        {
            public void run() {
              word.fill(0); 
              bg.fill(Automatype.BG_COL[0],Automatype.BG_COL[1],Automatype.BG_COL[2],Automatype.BG_COL[3]);
            }
        },
        (long) ((SUCCESS_PAUSE*1000)),TimeUnit.MILLISECONDS);
    }
  }

  private void doInsertOrDelete(boolean isInsert)
  {
    // System.out.println("DeleteAction: curr="+word.cursorIdx+" next="+nextCursPos);
    if (nextCursPos > word.cursorIdx)
    { // forward
      word.next();
      Automatype.type.play();
    }
    else if (nextCursPos < word.cursorIdx)
    { // back
      word.previous();
      Automatype.type.play();
    }
    else
    { // in position
      if (isInsert)
        word.insert(nextChar);
      else
        word.backspace();

      if (word.getText().equals(target.getText()))
      {
        onWordCompletion();
      }
      else
      {
        // pause the timer
        if (parent.autoMode)
          RiTa.pauseCallbackTimer(getPApplet(), getTimerName(), REPLACE_PAUSE);
        
        findNextEdit(); // pause on replace
      }
    }
  }

  private void doReplace()
  {
    if (nextCursPos > word.cursorIdx + 1)
    { 
      // forward
      word.showSelection(false);
      word.next();
      Automatype.type.play();
    }
    else if (nextCursPos < word.cursorIdx + 1)
    {
      // back
      word.showSelection(false);
      word.previous();
      if (nextCursPos != word.cursorIdx + 2)
        Automatype.type.play();
    }
    else if (!readyForReplace) // in position
    {
      readyForReplace = true;
      word.showSelection(true);
    }
    else
    {
      readyForReplace = false;
      word.showSelection(false);
      word.replace(nextChar);

      if (word.getText().equals(target.getText()))
      {
        onWordCompletion();
      }
      else
      {
        if (parent.autoMode)
          RiTa.pauseCallbackTimer(getPApplet(), getTimerName(), REPLACE_PAUSE);
        findNextEdit(); // pause on replace
      }
    }
  }

  private void pickNextTarget(LexiconLookup lexLook)
  {
    // String tmpHist = lexLook.getHistory(this)+"";
    String next = null;

    if (DO_DELETIONS)
    {
      double prob = Math.max(0, word.length() - MIN_WORD_LEN) * .1;
      if (Math.random() < prob)
        next = lexLook.getDeletions(this);
      if (next != null)
      {
        nextAction = DELETE_ACTION;
        // System.out.println("DELETE: next="+next+" curr="+word.cursorIdx);
      }
    }

    if (next == null && DO_INSERTIONS)
    {
      double prob = Math.max(0, MAX_WORD_LENGTH - word.length()) * .1;
      if (Math.random() < prob)
        next = lexLook.getInsertions(this);
      if (next != null)
      {
        nextAction = INSERT_ACTION;
      }
    }
    if (next == null)
    {
      nextAction = REPLACE_ACTION;
      next = lexLook.mutateWord(this);
    }

    // add to history and set target text
    lexLook.getHistory(this).add(next);
    target.setText(next);

    if (SHOW_TARGETS)
      onNewWordTarget();

    if (parent.autoMode)    {
      
      float pause = SUCCESS_PAUSE;
      if (firstWord)
        pause = id + .1f; // start cells staggered
      RiTa.pauseCallbackTimer(getPApplet(), getTimerName(), pause);
    }
  }

  void findNextEdit()
  {
    int cursIdx = word.cursorIdx;
    if (cursIdx == word.length())
      word.moveCursorTo(0);

    String curr = word.getText();
    String next = target.getText();

    int minLength = Math.min(curr.length(), next.length());
    while (cursIdx >= minLength)
      cursIdx--;

    if (curr.length() == next.length() + 1) // delete
      positionForDelete(curr, next);
    else if (curr.length() == next.length() - 1) // insert
      positionForInsert(curr, next);
    else
      positionForReplace(cursIdx, curr, next);      // replace
  }

  private void positionForReplace(int cursIdx, String current, String next)
  {
    int numChecks = 0;
    char a = current.charAt(cursIdx);
    char b = next.charAt(cursIdx);
    while (a == b && numChecks++ <= current.length())
    {
      if (++cursIdx == current.length())
        cursIdx = 0;
      a = current.charAt(cursIdx);
      b = next.charAt(cursIdx);
    }
    nextCursPos = cursIdx + 1;
    nextChar = b;
  }

  private void positionForDelete(String current, String next)
  {
    int idx = 0;
    for (; idx < next.length(); idx++)
    {
      char a = current.charAt(idx);
      char b = next.charAt(idx);
      if (a != b)
        break;
    }
    nextCursPos = idx + 1;
    nextChar = (char) DELETE_ACTION;
  }

  private void positionForInsert(String current, String next)
  {
    int idx = 0;
    char result = '~';
    for (; idx < current.length(); idx++)
    {
      char a = current.charAt(idx);
      char b = next.charAt(idx);
      if (a != b)
      {
        result = b;
        break;
      }
    }
    if (result == '~')
    {
      System.out.println("TAKING last char!!");
      result = next.charAt(idx);
    }
    nextCursPos = idx;
    nextChar = result;
  }

  private void onNewWordTarget()
  {
    target.x = word.x;
    target.y = word.y + 60;
    target.setAlpha(0);
    float[] tcol = target.getColor();
    tcol[3] = TARGET_MAX_A; // fade-in
    target.fadeColor(tcol, SUCCESS_PAUSE);
  }

  private String getTimerName()
  {
    return Automatype.UPDATE_PRE + id;
  }

  private PApplet getPApplet()
  {
    return word.getPApplet();
  }

}// end

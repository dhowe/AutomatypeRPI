package automatype;

public interface TransformConstants
{
  int MAX_WORD_LENGTH = 7, MIN_WORD_LEN = 3;
  boolean TEST_MANUAL_EVENTS = false, SHOW_TARGETS = false;
  boolean DO_DELETIONS = true, DO_INSERTIONS = true;
  
  String UPDATE_PRE = "update-";
  float BLINK_SPEED =.4f, INITIAL_KEY_PAUSE = .8f, REPLACE_PAUSE = 1.6f, SUCCESS_PAUSE = .8f;
  int NUM_ROWS = 1, NUM_COLS = 1, NUM_CELLS = NUM_ROWS * NUM_COLS;
  int REPLACE_ACTION = 1, DELETE_ACTION = 2, INSERT_ACTION = 3;
  int STROKE_WEIGHT = 2, HISTORY_SIZE = 20, TARGET_MAX_A = 0;

}

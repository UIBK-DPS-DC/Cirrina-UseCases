package at.ac.uibk.dps.smartfactory.object.error;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class SuddenPeakErrorDeterminer implements ErrorDeterminer {

  private static final float PEAK_CHANCE = 0.9F;
  private static final int PEAK_START_THRESHOLD = 80;
  private static final int PEAK_DURATION = 20;
  private static final int TOTAL_DURATION = PEAK_START_THRESHOLD + PEAK_DURATION;

  private final AtomicInteger callCount = new AtomicInteger(0);

  @Override
  public synchronized boolean isError(Random random) {
    int currentCount = callCount.updateAndGet(count -> (count + 1) % TOTAL_DURATION);

    if (currentCount >= PEAK_START_THRESHOLD) {
      return random.nextFloat() < PEAK_CHANCE;
    } else {
      return false;
    }
  }
}

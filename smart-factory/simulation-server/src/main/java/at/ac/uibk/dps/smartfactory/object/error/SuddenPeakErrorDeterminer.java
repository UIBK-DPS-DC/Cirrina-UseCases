package at.ac.uibk.dps.smartfactory.object.error;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Starts with a 0% error rate and suddenly changes to a high error rate if a certain
 * threshold of invocations is reached. Resets back to 0% after a certain duration.
 */
public class SuddenPeakErrorDeterminer implements ErrorDeterminer {

  private static final float PEAK_ERROR_CHANCE = 0.9F;
  private static final int PEAK_START_THRESHOLD = 80;
  private static final int PEAK_DURATION = 20;
  private static final int TOTAL_DURATION = PEAK_START_THRESHOLD + PEAK_DURATION;

  private final AtomicInteger callCount = new AtomicInteger(0);

  @Override
  public synchronized boolean isError(Random random) {
    int currentCount = callCount.updateAndGet(count -> (count + 1) % TOTAL_DURATION);

    return currentCount >= PEAK_START_THRESHOLD
        && random.nextFloat() < PEAK_ERROR_CHANCE;
  }

  @Override
  public float errorChance() {
    return callCount.get() >= PEAK_START_THRESHOLD
        ? PEAK_ERROR_CHANCE
        : 0F;
  }
}

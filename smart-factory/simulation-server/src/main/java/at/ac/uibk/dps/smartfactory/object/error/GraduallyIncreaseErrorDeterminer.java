package at.ac.uibk.dps.smartfactory.object.error;

import java.util.Random;

/**
 * Starts with a 0% error rate and increases gradually for each invocation until a certain
 * error rate is reached.
 */
public class GraduallyIncreaseErrorDeterminer implements ErrorDeterminer {

  private static final float START_CHANCE = 0F;
  private static final float CHANCE_INCREASE = 0.01F;
  private static final float MAX_CHANCE = 0.9F;

  private float errorChance = START_CHANCE;

  @Override
  public synchronized boolean isError(Random random) {
    final boolean isError = random.nextFloat() < errorChance;
    if (errorChance < MAX_CHANCE) {
      errorChance = Math.min(MAX_CHANCE, errorChance + CHANCE_INCREASE);
    }
    return isError;
  }

  @Override
  public float errorChance() {
    return errorChance;
  }
}

package at.ac.uibk.dps.smartfactory.object.error;

import java.util.Random;

public class RandomErrorDeterminer implements ErrorDeterminer {

  static final float ERROR_CHANCE_LOW = 0.2F;
  static final float ERROR_CHANCE_HIGH = 0.8F;

  private final float errorChance;

  public RandomErrorDeterminer(float errorChance) {
    this.errorChance = errorChance;
  }

  @Override
  public boolean isError(Random random) {
    return random.nextFloat() < errorChance;
  }
}

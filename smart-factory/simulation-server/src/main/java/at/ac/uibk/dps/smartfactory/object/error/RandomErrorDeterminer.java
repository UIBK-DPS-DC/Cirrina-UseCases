package at.ac.uibk.dps.smartfactory.object.error;

import java.util.Random;

/**
 * Fixed error chance.
 */
public record RandomErrorDeterminer(float errorChance) implements ErrorDeterminer {

  static final float ERROR_CHANCE_LOW = 0.2F;
  static final float ERROR_CHANCE_HIGH = 0.8F;

  @Override
  public boolean isError(Random random) {
    return random.nextFloat() < errorChance;
  }
}

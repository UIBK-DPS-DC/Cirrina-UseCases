package at.ac.uibk.dps.smartfactory.object.error;

import java.util.Random;

public interface ErrorDeterminer {
  ErrorDeterminer NO_ERRORS = new ErrorDeterminer() {
    @Override
    public boolean isError(Random random) {
      return false;
    }

    @Override
    public float errorChance() {
      return 0F;
    }
  };

  boolean isError(Random random);

  float errorChance();
}

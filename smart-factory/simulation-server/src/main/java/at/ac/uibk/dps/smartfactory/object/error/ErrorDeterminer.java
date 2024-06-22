package at.ac.uibk.dps.smartfactory.object.error;

import java.util.Random;

public interface ErrorDeterminer {
  ErrorDeterminer NO_ERRORS = random -> false;

  boolean isError(Random random);
}

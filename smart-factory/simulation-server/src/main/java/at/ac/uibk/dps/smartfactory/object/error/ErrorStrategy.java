package at.ac.uibk.dps.smartfactory.object.error;

import java.util.function.Supplier;

/**
 * Different error strategies which construct corresponding {@link ErrorDeterminer}s.
 */
public enum ErrorStrategy {

  NO_ERRORS(() -> ErrorDeterminer.NO_ERRORS),
  RANDOM_ERRORS_LOW(() -> new RandomErrorDeterminer(RandomErrorDeterminer.ERROR_CHANCE_LOW)),
  RANDOM_ERRORS_HIGH(() -> new RandomErrorDeterminer(RandomErrorDeterminer.ERROR_CHANCE_HIGH)),
  GRADUALLY_INCREASE(GraduallyIncreaseErrorDeterminer::new),
  SUDDEN_PEAK(SuddenPeakErrorDeterminer::new);

  private final Supplier<ErrorDeterminer> errorDeterminerSupplier;

  ErrorStrategy(Supplier<ErrorDeterminer> errorDeterminerSupplier) {
    this.errorDeterminerSupplier = errorDeterminerSupplier;
  }

  public ErrorDeterminer getErrorDeterminer() {
    return errorDeterminerSupplier.get();
  }
}

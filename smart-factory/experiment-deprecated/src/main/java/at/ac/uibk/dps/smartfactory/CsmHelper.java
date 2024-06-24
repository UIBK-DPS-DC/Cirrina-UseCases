package at.ac.uibk.dps.smartfactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public final class CsmHelper {

  public static String readCsm(String csmFile) throws FileNotFoundException {
    return new Scanner(new File("../csml/" + csmFile)).useDelimiter("\\Z").next();
  }
}

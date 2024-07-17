package spacefiller.modelmapper;

import processing.core.PMatrix3D;

// A simple data only class that represents calibration parameters which can be applied
// to a graphics context in order to achieve projection mapping alignment.
public class CalibrationData {
  public PMatrix3D projectionMatrix;
  public PMatrix3D modelViewMatrix;

  public CalibrationData(PMatrix3D projectionMatrix, PMatrix3D modelViewMatrix) {
    this.projectionMatrix = projectionMatrix;
    this.modelViewMatrix = modelViewMatrix;
  }

  public CalibrationData() {
  }

  public static CalibrationData empty() {
    return new CalibrationData();
  }

  public boolean isReady() {
    return projectionMatrix != null && modelViewMatrix != null;
  }

  @Override
  public String toString() {
    return "Calibration{" + "projectionMatrix=" + projectionMatrix.toString() + ", modelViewMatrix=" + modelViewMatrix.toString() + '}';
  }
}
package spacefiller.modelmapper;

import processing.core.PVector;
import processing.opengl.PGraphics3D;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static spacefiller.modelmapper.GeometryUtils.getClosestPointByMappedPoint;

public class Mapping {
  private PGraphics3D parentGraphics;
  private Map<PVector, PVector> points;
  private GraphicsTransform transform;

  public Mapping(PGraphics3D parentGraphics) {
    this.parentGraphics = parentGraphics;
    this.points = new HashMap<>();
  }

  public void put(PVector from, PVector to) {
    this.points.put(from, to);
    transform = CalibrationUtils.calibrate(
        this.points, parentGraphics.width, parentGraphics.height);
  }

  public void remove(PVector from) {
    this.points.remove(from);
    transform = CalibrationUtils.calibrate(
        this.points, parentGraphics.width, parentGraphics.height);
  }

  public Set<PVector> getMappedPoints() {
    return points.keySet();
  }

  public PVector get(PVector key) {
    return points.get(key);
  }

  public PVector getClosestMappedPointTo(PVector query) {
    return getClosestPointByMappedPoint(query, points);
  }


  public boolean isReady() {
    return transform.isReady();
  }


  public void begin() {
    begin(parentGraphics);
  }

  public void end() {
    end(parentGraphics);
  }

  public void begin(PGraphics3D graphics) {
    graphics.background(0);

    if (isReady()) {
      graphics.pushMatrix();
      graphics.pushProjection();

      graphics.resetMatrix();
      graphics.setProjection(transform.projectionMatrix);
      graphics.camera(0, 0, 0, 0, 0, 1, 0, -1, 0);
      graphics.applyMatrix(transform.modelViewMatrix);
    }
  }

  public void end(PGraphics3D graphics) {
    if (isReady()) {
      graphics.popMatrix();
      graphics.popProjection();
    }
  }
}

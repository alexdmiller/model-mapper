package spacefiller.modelmapper;

import peasy.PeasyCam;
import processing.core.PApplet;
import processing.core.PShape;
import processing.core.PVector;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.opengl.PGraphics3D;

import java.util.HashMap;
import java.util.Map;

import static processing.core.PConstants.P3D;
import static spacefiller.modelmapper.Utils.*;

public class ModelMapper {
  private enum Mode {
    CALIBRATE, RENDER
  }

  private enum CalibrationSpace {
    MODEL_SPACE, PIXEL_SPACE
  }

  private PApplet parent;
  private PGraphics3D modelCanvas;
  private PShape model;
  private Mode mode;
  private CalibrationSpace space;
  private PeasyCam camera;

  private PVector selectedVertex;
  private Map<PVector, PVector> pointMapping;
  private CalibrationData calibrationData;

  public ModelMapper(PApplet parent, PShape model) {
    this.parent = parent;
    this.modelCanvas = (PGraphics3D) parent.createGraphics(parent.width, parent.height, P3D);
    this.model = model;
    this.mode = Mode.RENDER;
    this.space = CalibrationSpace.MODEL_SPACE;
    this.camera = new PeasyCam(parent, modelCanvas, 400);
    this.pointMapping = new HashMap<>();

    this.parent.registerMethod("draw", this);
    this.parent.registerMethod("mouseEvent", this);
    this.parent.registerMethod("keyEvent", this);
    this.parent.registerMethod("post", this);
  }

  public void draw() {
    PVector mouse = new PVector(parent.mouseX, parent.mouseY);
    if (mode == Mode.CALIBRATE) {
      if (space == CalibrationSpace.MODEL_SPACE) {
        modelCanvas.beginDraw();
        modelCanvas.background(0);
        model.disableStyle();

        // Only turn peasycam on when in calibrate mode and in model space; otherwise use
        // calibrated camera.
        camera.setActive(true);

        // TODO: make fill/stroke customizable when in calibration mode?
        modelCanvas.fill(50);
        modelCanvas.stroke(255);
        modelCanvas.strokeWeight(2);
        modelCanvas.shape(model);
        modelCanvas.endDraw();

        parent.image(modelCanvas, 0, 0);

        PVector closestPoint = getClosestPointOnShape(mouse, model, modelCanvas);

        for (PVector modelPoint : pointMapping.keySet()) {
          PVector projectedPoint = worldToScreen(modelPoint, modelCanvas);
          parent.strokeWeight(5);
          parent.stroke(100);
          parent.noFill();
          parent.ellipse(projectedPoint.x, projectedPoint.y, 15, 15);
        }

        if (closestPoint != null) {
          PVector projectedVertex = worldToScreen(closestPoint, modelCanvas);
          parent.noFill();
          parent.stroke(100, 0, 100);
          parent.strokeWeight(5);
          parent.ellipse(projectedVertex.x, projectedVertex.y, 15, 15);
        }

        if (selectedVertex != null) {
          PVector projectedVertex = worldToScreen(selectedVertex, modelCanvas);
          parent.noFill();
          parent.stroke(255, 0, 255);
          parent.strokeWeight(5);
          parent.ellipse(projectedVertex.x, projectedVertex.y, 15, 15);
        }
      } else if (space == CalibrationSpace.PIXEL_SPACE) {
        camera.setActive(false);
      }
    } else if (mode == Mode.RENDER) {
      camera.setActive(false);
    }
  }

  public void post() {
  }

  public void mouseEvent(MouseEvent event) {
    PVector mouse = new PVector(event.getX(), event.getY());

    if (mode != Mode.CALIBRATE) {
      // Library only responds to mouse input when in calibrate mode
      return;
    }

    if (space == CalibrationSpace.MODEL_SPACE) {
      if (event.getAction() == MouseEvent.CLICK) {
        selectedVertex = getClosestPointOnShape(mouse, model, modelCanvas);
      }
    } else if (space == CalibrationSpace.PIXEL_SPACE) {
      switch (event.getAction()) {
        case MouseEvent.PRESS:
          PVector newSelection = getClosestPointByMappedPoint(mouse, pointMapping);
          if (newSelection != null) {
            selectedVertex = newSelection;
          }
          break;
        case MouseEvent.DRAG:
        case MouseEvent.CLICK:
          if (selectedVertex != null) {
            pointMapping.put(selectedVertex, mouse);
            calibrationData = Calibration.calibrate(pointMapping, parent.width, parent.height);
            // save();
          }
          break;
      }
    }
  }

  public void keyEvent(KeyEvent event) {
    if (event.getAction() == KeyEvent.PRESS) {
      if (event.getKeyCode() == 32) { // space
        mode = (mode == Mode.CALIBRATE) ? Mode.RENDER : Mode.CALIBRATE;
      } else if (event.getKeyCode() == 9) { // tab
        space = (space == CalibrationSpace.MODEL_SPACE)
            ? CalibrationSpace.PIXEL_SPACE
            : CalibrationSpace.MODEL_SPACE;
      }
    }
  }

  public void calibrateMode() {
    this.mode = Mode.CALIBRATE;
  }

  public void renderMode() {
    this.mode = Mode.RENDER;
  }
}

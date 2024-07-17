package spacefiller.modelmapper;

import peasy.PeasyCam;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.core.PVector;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.opengl.PGraphics3D;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static processing.core.PConstants.*;
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
  private PGraphics3D projectionCanvas;
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
    this.projectionCanvas = (PGraphics3D) parent.createGraphics(parent.width, parent.height, P3D);
    this.model = model;
    this.mode = Mode.RENDER;
    this.space = CalibrationSpace.MODEL_SPACE;
    this.camera = new PeasyCam(parent, modelCanvas, 400);
    this.pointMapping = new HashMap<>();

    this.parent.registerMethod("draw", this);
    this.parent.registerMethod("mouseEvent", this);
    this.parent.registerMethod("keyEvent", this);
    this.parent.registerMethod("post", this);

    loadCalibration();
    calibrationData = Calibration.calibrate(pointMapping, parent.width, parent.height);
  }

  public void drawProjectionWithCalibration(PGraphics3D canvas, CalibrationData calibration, int color) {
    if (!calibration.isReady()) {
      return;
    }

    canvas.resetMatrix();
    canvas.setProjection(calibration.projectionMatrix);
    canvas.camera(0, 0, 0, 0, 0, 1, 0, -1, 0);
    canvas.applyMatrix(calibration.modelViewMatrix);
    int j = 0;
    for (PShape shape : model.getChildren()) {
      canvas.beginShape();
      canvas.stroke(255);
      canvas.strokeWeight(3);
      canvas.fill(0);
      j++;
//      canvas.noStroke();
      for (int i = 0; i < shape.getVertexCount(); i++) {
        PVector v = shape.getVertex(i);
        canvas.vertex(v.x, v.y, v.z);
      }
      canvas.endShape(CLOSE);
    }

//    for (PShape shape : objOutside.getChildren()) {
//      canvas.beginShape();
//      canvas.stroke(color);
//      canvas.fill(((frameCount / 2 + 1) % 2) * 255);
//      canvas.noStroke();
//      for (int i = 0; i < shape.getVertexCount(); i++) {
//        PVector v = shape.getVertex(i);
//        canvas.vertex(v.x, v.y, v.z);
//      }
//      canvas.endShape(CLOSE);
//    }
  }

  private void drawModel(PShape model, PGraphics canvas) {
    model.disableStyle();
    canvas.fill(20);
    canvas.stroke(255);
    canvas.strokeWeight(2);
    canvas.shape(model);
    canvas.endDraw();
  }

  public void draw() {
    PVector mouse = new PVector(parent.mouseX, parent.mouseY);
    if (mode == Mode.CALIBRATE) {
      parent.background(0);

      if (space == CalibrationSpace.MODEL_SPACE) {
        // Only turn peasycam on when in calibrate mode and in model space; otherwise use
        // calibrated camera.
        camera.setActive(true);

        modelCanvas.beginDraw();
        modelCanvas.scale(1, -1, 1);
        modelCanvas.background(0);

        drawModel(model, modelCanvas);

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

        if (calibrationData.isReady()) {
          projectionCanvas.beginDraw();
          projectionCanvas.clear();

          projectionCanvas.resetMatrix();
          projectionCanvas.setProjection(calibrationData.projectionMatrix);
          projectionCanvas.camera(0, 0, 0, 0, 0, 1, 0, -1, 0);
          projectionCanvas.applyMatrix(calibrationData.modelViewMatrix);

          drawModel(model, projectionCanvas);

          projectionCanvas.endDraw();
        } else {
          parent.textMode(CENTER);
          parent.text("No calibration", (float) parent.width / 2, (float) parent.height / 2);
        }

        parent.image(projectionCanvas, 0, 0);

        for (PVector modelPoint : pointMapping.keySet()) {
          PVector projectedPoint = pointMapping.get(modelPoint);
          parent.strokeWeight(5);
          if (selectedVertex != null && selectedVertex.equals(modelPoint)) {
            parent.stroke(0, 255, 255);
          } else {
            parent.stroke(100);
          }
          parent.noFill();
          parent.ellipse(projectedPoint.x, projectedPoint.y, 15, 15);
        }
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
            saveCalibration();
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

  private void saveCalibration() {
    try {
      FileOutputStream fileOutputStream = new FileOutputStream(parent.dataPath("calibration.ser"));
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
      objectOutputStream.writeObject(pointMapping);
      objectOutputStream.flush();
      objectOutputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void loadCalibration() {
    pointMapping = new HashMap<>();
    try {
      FileInputStream fileInputStream = new FileInputStream(parent.dataPath("calibration.ser"));
      ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
      pointMapping = (Map<PVector, PVector>) objectInputStream.readObject();
      objectInputStream.close();
    } catch (IOException | ClassNotFoundException e) {
      System.out.println("ModelMapper: Attempted to load calibration data, but either it does not exist.");
      System.out.println("ModelMapper: If you have not yet calibrated your projection, this is normal.");
    }
  }
}

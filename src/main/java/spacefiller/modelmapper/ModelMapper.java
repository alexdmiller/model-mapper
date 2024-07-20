package spacefiller.modelmapper;

import peasy.PeasyCam;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.core.PVector;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.opengl.PGraphics3D;
import processing.opengl.PShader;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static processing.core.PConstants.*;
import static spacefiller.modelmapper.Utils.*;

public class ModelMapper {
  private static final float UI_CIRCLE_RADIUS = 10;

  private enum Mode {
    CALIBRATE, RENDER
  }

  private enum CalibrationSpace {
    MODEL_SPACE, PIXEL_SPACE
  }

  private PApplet parent;

  private PGraphics3D parentGraphics;
  private PGraphics3D modelCanvas;
  private PGraphics3D projectionCanvas;

  private PShape model;
  private Mode mode;
  private CalibrationSpace space;
  private PeasyCam camera;

  private PVector selectedVertex;
  private Map<PVector, PVector> pointMapping;
  private CalibrationData calibrationData;

  PShader modelRenderShader;

  public ModelMapper(PApplet parent, PShape model) {
    // If we share the model with the client, then when the client renders it, they can
    // update state that will impact our ability to render it. For consistent rendering,
    // make our own private copy.
    this.model = Shapes.createShape(parent, model);

    this.parent = parent;
    try {
      this.parentGraphics = (PGraphics3D) parent.getGraphics();
    } catch (ClassCastException e) {
      System.out.println("ModelMapper: Must use P3D rendering mode with ModelMapper library");
      System.out.println("ModelMapper:   size(P3D, 500, 500)");
    }
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

    File tempFile = IO.extractResourceToFile("/model.frag.glsl");
    String filePath = tempFile.getAbsolutePath();
    modelRenderShader = parent.loadShader(filePath);

    loadCalibration();
    calibrationData = Calibration.calibrate(pointMapping, parent.width, parent.height);
  }

  public void calibrateMode() {
    this.mode = Mode.CALIBRATE;
  }

  public void renderMode() {
    this.mode = Mode.RENDER;
  }

  public void begin() {
    parentGraphics.background(0);

    if (calibrationData.isReady()) {
      parentGraphics.pushMatrix();
      parentGraphics.pushProjection();

      parentGraphics.resetMatrix();
      parentGraphics.setProjection(calibrationData.projectionMatrix);
      parentGraphics.camera(0, 0, 0, 0, 0, 1, 0, -1, 0);
      parentGraphics.applyMatrix(calibrationData.modelViewMatrix);
    }
  }

  public void end() {
    if (calibrationData.isReady()) {
      parentGraphics.popMatrix();
      parentGraphics.popProjection();
    }
  }

  private void drawModel(PShape model, PGraphics canvas) {
    canvas.resetShader();

    model.disableStyle();

    canvas.fill(0);
    canvas.stroke(255);
    canvas.strokeWeight(2);
    canvas.shape(model);
    canvas.endDraw();
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

  /**
   * Processing hooks
   */

  public void draw() {
    parentGraphics.resetShader();

    PVector mouse = new PVector(parent.mouseX, parent.mouseY);
    if (mode == Mode.CALIBRATE) {
      parent.noCursor();
      parent.background(0);

      if (space == CalibrationSpace.MODEL_SPACE) {
        parent.background(0);

        // Only turn peasycam on when in calibrate mode and in model space; otherwise use
        // calibrated camera.
        camera.setActive(true);
        camera.feed();

        modelCanvas.beginDraw();
        modelCanvas.clear();
        modelCanvas.scale(1, -1, 1);

        drawModel(model, modelCanvas);

        parent.resetShader();

        parent.textureMode(NORMAL);
        parent.beginShape();
        parent.texture(modelCanvas);

        modelRenderShader.set("time", parent.frameCount);
        parent.shader(modelRenderShader);

        parent.noStroke();
        parent.vertex(0, 0, 0, 0);
        parent.vertex(parent.width, 0, 1, 0);
        parent.vertex(parent.width, parent.height, 1, 1);
        parent.vertex(0, parent.height, 0, 1);
        parent.endShape();

        PVector closestPoint = getClosestPointOnShape(mouse, model, modelCanvas);

        for (PVector modelPoint : pointMapping.keySet()) {
          PVector projectedPoint = worldToScreen(modelPoint, modelCanvas);
          parent.noStroke();
          parent.fill(255, 200);
          parent.ellipse(projectedPoint.x, projectedPoint.y, UI_CIRCLE_RADIUS, UI_CIRCLE_RADIUS);
        }

        if (closestPoint != null) {
          PVector projectedVertex = worldToScreen(closestPoint, modelCanvas);
          parent.stroke(255);
          parent.strokeWeight(2);
          parent.noFill();
          parent.ellipse(projectedVertex.x, projectedVertex.y, UI_CIRCLE_RADIUS, UI_CIRCLE_RADIUS);
        }

        if (selectedVertex != null) {
          PVector projectedVertex = worldToScreen(selectedVertex, modelCanvas);
          drawCrossHairs(projectedVertex.x, projectedVertex.y, parent.color(255, 0, 255));
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

          parent.noStroke();
          parent.fill(255, 200);
          parent.ellipse(projectedPoint.x, projectedPoint.y, UI_CIRCLE_RADIUS, UI_CIRCLE_RADIUS);
          parent.fill(255);
          parent.ellipse(projectedPoint.x, projectedPoint.y, 2, 2);

          if (selectedVertex != null && selectedVertex.equals(modelPoint)) {
            drawCrossHairs(projectedPoint.x, projectedPoint.y, parent.color(0, 255, 255));
          }
        }

        PVector closestPoint = getClosestPointByMappedPoint(mouse, pointMapping);
        if (closestPoint != null) {
          PVector projectedPoint = pointMapping.get(closestPoint);
          parent.stroke(255);
          parent.strokeWeight(2);
          parent.noFill();
          parent.ellipse(projectedPoint.x, projectedPoint.y, UI_CIRCLE_RADIUS, UI_CIRCLE_RADIUS);
        }
      }

      // Draw mouse cross-hairs
      drawCrossHairs(parent.mouseX, parent.mouseY, parent.color(255));
//      calibrationData = Calibration.calibrate(pointMapping, parent.width, parent.height, parent.mouseX - parent.width / 2f, parent.mouseY - parent.height / 2f);
    } else if (mode == Mode.RENDER) {
      camera.setActive(false);
      parent.cursor();
    }
  }

  private void drawCrossHairs(float x, float y, int color) {
    parent.stroke(color, 150);
    parent.strokeWeight(2);
    parent.line(0, y, parent.width, y);
    parent.line(x, 0, x, parent.height);
    parent.noStroke();
    parent.fill(color);
    parent.ellipseMode(CENTER);
    parent.ellipse(x, y, UI_CIRCLE_RADIUS, UI_CIRCLE_RADIUS);
  }

  // TODO: is this needed?
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
}

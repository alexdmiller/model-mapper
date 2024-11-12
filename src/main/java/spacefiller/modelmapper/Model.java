package spacefiller.modelmapper;

import processing.core.PApplet;
import processing.core.PShape;
import processing.core.PVector;
import processing.opengl.PGraphics3D;

import java.util.ArrayList;
import java.util.List;

import static spacefiller.modelmapper.GeometryUtils.getClosestPointByMappedPoint;
import static spacefiller.modelmapper.GeometryUtils.getClosestPointOnShape;

public class Model {
  private PApplet parent;
  private PGraphics3D parentGraphics;
  private PShape shape;
  private List<Mapping> mappings;

  public Model(PApplet parent, PShape shape) {
    this.parent = parent;

    try {
      this.parentGraphics = (PGraphics3D) parent.getGraphics();
    } catch (ClassCastException e) {
      System.out.println("ModelMapper: Must use P3D rendering mode with ModelMapper library. Example:");
      System.out.println("ModelMapper:   size(500, 500, P3D)");
    }

    // If we share the model with the client, then when the client renders it, they can
    // update state that will impact our ability to render it. For consistent rendering,
    // make our own private copy.
    this.shape = ShapeUtils.createShape(parent, shape);
    this.mappings = new ArrayList<>();

    // Each model starts with one mapping
    createMapping();
  }

  public void createMapping() {
    Mapping m = new Mapping(this.parentGraphics);
    mappings.add(m);
  }

  public Iterable<Mapping> getMappings() {
    return mappings;
  }

  public Mapping getMapping(int index) {
    return mappings.get(index);
  }

  public void draw(PGraphics3D canvas) {
    canvas.resetShader();

    this.shape.disableStyle();

    canvas.fill(0);
    canvas.stroke(255);
    canvas.strokeWeight(2);
    canvas.shape(this.shape);
    canvas.endDraw();
  }


  public PVector getClosestPointTo(PVector mouse, PGraphics3D modelCanvas) {
    return getClosestPointOnShape(mouse, shape, modelCanvas);
  }
}

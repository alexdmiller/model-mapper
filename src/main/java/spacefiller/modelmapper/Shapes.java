package spacefiller.modelmapper;

import processing.core.PApplet;
import processing.core.PShape;

import static processing.core.PShape.GROUP;
import static processing.core.PShape.GEOMETRY;
import static processing.core.PShape.X;
import static processing.core.PShape.Y;
import static processing.core.PShape.Z;


public class Shapes {
  static public PShape createShape(PApplet parent, PShape src) {
    PShape dest = null;
    if (src.getFamily() == GROUP) {
      dest = parent.createShape(GROUP);
      copyGroup(parent, src, dest);
    } else if (src.getFamily() == GEOMETRY) {
      dest = parent.createShape(GEOMETRY);
      copyGeometry(src, dest);
    }
    dest.setName(src.getName());
    return dest;
  }

  static public void copyGroup(PApplet parent, PShape src, PShape dest) {
    for (int i = 0; i < src.getChildCount(); i++) {
      PShape c = createShape(parent, src.getChild(i));
      dest.addChild(c);
    }
  }

  static public void copyGeometry(PShape src, PShape dest) {
    dest.beginShape(src.getKind());

    for (int i = 0; i < src.getVertexCount(); i++) {
      float[] vert = src.getVertex(i).array();
      if (vert[Z] == 0) {
        dest.vertex(vert[X], vert[Y]);
      } else {
        dest.vertex(vert[X], vert[Y], vert[Z]);
      }
    }

    dest.endShape();
  }
}

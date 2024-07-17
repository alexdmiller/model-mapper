package spacefiller.modelmapper;

import processing.core.PShape;
import processing.core.PVector;
import processing.opengl.PGraphics3D;

import java.util.Map;

public class Utils {
  public static PVector worldToScreen(PVector vertex, PGraphics3D graphics) {
    float screenX = graphics.screenX(vertex.x, vertex.y, vertex.z);
    float screenY = graphics.screenY(vertex.x, vertex.y, vertex.z);
    float screenZ = graphics.screenZ(vertex.x, vertex.y, vertex.z);
    return new PVector(screenX, screenY, screenZ);
  }

  public static PVector getClosestPointOnShape(PVector point, PShape shape, PGraphics3D graphics) {
    PVector closest = null;
    float minDistance = 1000;
    float selectionRadius = 10;

    for (PShape child : shape.getChildren()) {
      for (int i = 0; i < child.getVertexCount(); i++) {
        PVector vertex = child.getVertex(i);
        PVector projectedVertex = worldToScreen(vertex, graphics);
        float dist = projectedVertex.dist(point);
        if (dist < selectionRadius && projectedVertex.z < minDistance) {
          closest = vertex;
          minDistance = projectedVertex.z;
        }
      }
    }
    return closest;
  }

//  public static PVector getClosestPointByMappedPoint(PVector queryPoint) {
//    return getClosestPointByMappedPoint(queryPoint, pointMapping);
//  }

  public static PVector getClosestPointByMappedPoint(PVector queryPoint, Map<PVector, PVector> map) {
    float selectionRadius = 10;
    for (PVector referencePoint : map.keySet()) {
      PVector mapped = map.get(referencePoint);
      float dist = mapped.dist(queryPoint);
      if (dist < selectionRadius) {
        return referencePoint;
      }

    }
    return null;
  }
}

/**
 * This is a basic example of how to use the ModelMapper library to
 * projection map a simple cube shape. This example assumes that you
 * have a physical cube of some sort and a projector pointed at that
 * cube. You can then use the ModelMapper UI to calibrate the projection
 * to align with the cube.
 *
 * After you run the sketch, press space to switch to calibration mode.
 * To use the calibration UI:
 *
 * 1. Click and drag to rotate the 3D model, and command + drag to pan.
 * 2. Click a corner vertex of the model to select it.
 * 3. Press tab to switch to projection view.
 * 4. Looking at your physical cube now, click again to place the
 *    corresponding point on the physical object.
 * 5. Repeat for 6 points.
 *
 * After 6 points have been placed, a calibration will be attempted.
 * You can place more points to fine tune the mapping.
 */

import spacefiller.modelmapper.ModelMapper;

ModelMapper mapper;
PShape model1;
PShape model2;

void setup() {
  fullScreen(P3D);
  model1 = createShape(BOX, 150);
  model2 = createShape(BOX, 200);
  mapper = new ModelMapper(this);
  mapper.addModel(model1);
  mapper.addModel(model2);
}

void draw() {
  background(0);

  mapper.begin(model1);

  pointLight(
    0,
    255,
    255,
    cos(frameCount / 10f) * 300,
    sin(frameCount / 10f) * 300,
    cos(frameCount / 20f) * 300);
  pointLight(
    255,
    255,
    0,
    cos(-frameCount / 20) * 300,
    sin(-frameCount / 20f) * 300,
    cos(-frameCount / 15f) * 300);

  // Draw cube itself
  shape(model1);
  mapper.end();

  mapper.begin(model2);

  pointLight(
    0,
    255,
    255,
    cos(frameCount / 10f) * 300,
    sin(frameCount / 10f) * 300,
    cos(frameCount / 20f) * 300);
  pointLight(
    255,
    255,
    0,
    cos(-frameCount / 20) * 300,
    sin(-frameCount / 20f) * 300,
    cos(-frameCount / 15f) * 300);

  // Draw cube itself
  shape(model2);
  mapper.end();
}

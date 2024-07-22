import spacefiller.modelmapper.ModelMapper;

ModelMapper mapper;
PShape model;

void setup() {
  fullScreen(P3D);
  model = createShape(BOX, 150);
  mapper = new ModelMapper(this, model);
}

void draw() {
  background(0);

  mapper.begin();
  fill(255);
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
  shape(model);
  mapper.end();
}

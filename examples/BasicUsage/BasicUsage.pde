import spacefiller.modelmapper.ModelMapper;

ModelMapper mapper;
PShape obj;

void setup() {
  size(400,400);
  smooth();

  obj = loadShape("model.obj");
  mapper = new ModelMapper(this, obj);
}

void draw() {
}
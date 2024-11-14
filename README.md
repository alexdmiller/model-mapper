# ModelMapper

Library for projection mapping with Processing. WIP.

## TODO:

- Bring over UI library code into actual library and decide on the interface
  - Maybe no keyboard / mouse input, just calls to functions
    - calibrateMode() / renderMode()
  - Or maybe utility classes to help with UI, but not actually draw anything
  - PointMap class?
    - mapPoint(PVector from, PVector to)
    - CalibrationPoints(PShape shape)
      - getClosestPoint(input)
      - mapPoint(input, output)
- What utilities to provide for drawing?
  - Stroke vs fills
  - Shader with normals

- number faces for easier orientation?
- experiment with nicer/more concise interface for drawing
- Figure out UI
  - is it important?

## Building & running

The fastest way I've found to build and test the library:

```
./gradlew build && cp build/libs/ModelMapper.jar ~/Documents/Processing/libraries/ModelMapper/library/
```

Then run a Processing sketch that uses the library.
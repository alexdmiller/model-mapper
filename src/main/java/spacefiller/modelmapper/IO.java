package spacefiller.modelmapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Stream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class IO {
  public static String[] getFileContents(String filename) {
    try {
      InputStream resourceAsStream = ModelMapper.class.getResourceAsStream("/model.frag.glsl");
      BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));
      Stream<String> lineStream = reader.lines();
      List<String> lines = lineStream.toList();
      return lines.toArray(new String[] {});
    } catch (Exception e) {
      e.printStackTrace();
      return new String[] {};
    }
  }

  public static File extractResourceToFile(String resourcePath) {
    try {
      InputStream resourceAsStream = ModelMapper.class.getResourceAsStream(resourcePath);
      if (resourceAsStream == null) {
        throw new IllegalArgumentException("Resource not found: " + resourcePath);
      }

      File tempFile = Files.createTempFile("temp", ".glsl").toFile();
      tempFile.deleteOnExit();

      try (FileOutputStream out = new FileOutputStream(tempFile)) {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = resourceAsStream.read(buffer)) != -1) {
          out.write(buffer, 0, bytesRead);
        }
      }

      return tempFile;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }
}

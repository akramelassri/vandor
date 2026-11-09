package com.vandor.services;

import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;

public class ImageService {
  public static final long MAX_SIZE = 5 * 1024 * 1024;
  private final Path directory;

  public ImageService() {
    this(
        Path.of(
            System.getenv()
                .getOrDefault(
                    "UPLOAD_DIR", System.getProperty("java.io.tmpdir") + "/vandor-uploads")));
  }

  public ImageService(Path directory) {
    this.directory = directory;
  }

  public String store(Part part) throws IOException {
    if (part == null || part.getSize() == 0) {
      return null;
    }
    if (part.getSize() > MAX_SIZE) {
      throw new IllegalArgumentException("Choose a JPG or PNG image under 5 MB.");
    }
    try (InputStream input = part.getInputStream();
        var stream = ImageIO.createImageInputStream(input)) {
      var readers = ImageIO.getImageReaders(stream);
      if (!readers.hasNext()) {
        throw new IllegalArgumentException("Choose a valid JPG or PNG image.");
      }
      var reader = readers.next();
      try {
        String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
        if (!format.equals("png") && !format.equals("jpeg") && !format.equals("jpg")) {
          throw new IllegalArgumentException("Only JPG and PNG images are supported.");
        }
        reader.setInput(stream);
        if (reader.getWidth(0) > 4096 || reader.getHeight(0) > 4096) {
          throw new IllegalArgumentException("Image dimensions cannot exceed 4096 pixels.");
        }
        var image = reader.read(0);
        String extension = format.equals("png") ? "png" : "jpg";
        String name = UUID.randomUUID() + "." + extension;
        Files.createDirectories(directory);
        Path destination = directory.resolve(name);
        try {
          if (!ImageIO.write(image, extension, destination.toFile())) {
            throw new IOException("Could not encode image.");
          }
        } catch (IOException e) {
          Files.deleteIfExists(destination);
          throw e;
        }
        return name;
      } finally {
        reader.dispose();
      }
    } catch (javax.imageio.IIOException e) {
      throw new IllegalArgumentException("The image is damaged or invalid.");
    }
  }

  public Path file(String name) {
    if (name == null || !name.matches("[0-9a-fA-F-]{36}\\.(jpg|png)")) {
      return null;
    }
    Path path = directory.resolve(name);
    return Files.isRegularFile(path) ? path : null;
  }

  public void discard(String name) throws IOException {
    Path file = file(name);
    if (file != null) {
      Files.deleteIfExists(file);
    }
  }
}

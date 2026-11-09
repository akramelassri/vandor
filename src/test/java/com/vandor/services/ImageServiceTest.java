package com.vandor.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.servlet.http.Part;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageServiceTest {
  @TempDir Path dir;

  Part part(byte[] bytes, long size) {
    return (Part)
        Proxy.newProxyInstance(
            Part.class.getClassLoader(),
            new Class<?>[] {Part.class},
            (p, m, a) ->
                switch (m.getName()) {
                  case "getSize" -> size;
                  case "getInputStream" -> new ByteArrayInputStream(bytes);
                  default -> null;
                });
  }

  byte[] image(String format, int width, int height) throws IOException {
    var output = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, output);
    return output.toByteArray();
  }

  @Test
  void storesRealImagesWithSchemaSizedNamesAndRejectsPathTraversal() throws Exception {
    ImageService service = new ImageService(dir);
    for (String format : new String[] {"png", "jpg"}) {
      byte[] bytes = image(format, 4, 4);
      String name = service.store(part(bytes, bytes.length));
      assertEquals(40, name.length());
      assertNotNull(service.file(name));
      assertNotNull(ImageIO.read(service.file(name).toFile()));
      service.discard(name);
      assertNull(service.file(name));
    }
    assertNull(service.file("../secret.png"));
    assertNull(service.file("default.jpg"));
    assertNull(service.store(null));
  }

  @Test
  void rejectsUnsupportedDamagedOversizedAndHugeDimensionImages() throws Exception {
    ImageService service = new ImageService(dir);
    assertThrows(
        IllegalArgumentException.class, () -> service.store(part(new byte[] {1, 2, 3}, 3)));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.store(part(new byte[] {1}, ImageService.MAX_SIZE + 1)));
    byte[] gif = image("gif", 2, 2);
    assertThrows(IllegalArgumentException.class, () -> service.store(part(gif, gif.length)));
    byte[] large = image("png", 4097, 1);
    assertThrows(IllegalArgumentException.class, () -> service.store(part(large, large.length)));
    try (var files = Files.list(dir)) {
      assertEquals(0, files.count());
    }
  }
}

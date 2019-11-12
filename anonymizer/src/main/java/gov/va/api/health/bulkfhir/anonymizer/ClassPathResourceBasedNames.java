package gov.va.api.health.bulkfhir.anonymizer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class ClassPathResourceBasedNames implements Names {

  private static final ClassPathResourceBasedNames FBN_INSTANCE =
      new ClassPathResourceBasedNames(loadList());

  List<String> names;

  public static ClassPathResourceBasedNames instance() {
    return FBN_INSTANCE;
  }

  @SneakyThrows
  private static List<String> loadList() {
    int namesCount = 32459;
    String sharedNamesClassPathResource = "yob2005.txt";
    List<String> list = new ArrayList<>(namesCount);
    BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(
                ClassLoader.getSystemResourceAsStream(sharedNamesClassPathResource),
                StandardCharsets.UTF_8));
    reader.lines().forEach(line -> list.add(line.split(",")[0]));
    reader.close();
    return Collections.unmodifiableList(list);
  }

  /** Safely handle wrapping any value into an array index. */
  @Override
  public String getName(long index) {
    return names.get((int) (index % (names.size())));
  }
}

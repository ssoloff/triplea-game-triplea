package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

class GameDataVariableParserTest {

  private static final String FOLDER = "src/test/resources/variable_parsing/";
  private static final String EMPTY_LIST = FOLDER + "empty_list.xml";
  private static final String SINGLE_ELEMENT_LIST = FOLDER + "single_element_list.xml";
  private static final String MANY_ELEMENT_LIST = FOLDER + "many_element_list.xml";
  private static final String NESTED_VARIABLE = FOLDER + "nested_variable.xml";

  private final GameDataVariableParser parser = new GameDataVariableParser();

  @Test
  void emptyList() throws Exception {
    final Element xmlSample = readFile(EMPTY_LIST);

    final Map<String, List<String>> result = parser.parseVariables(xmlSample);

    assertThat(result.keySet(), empty());
  }

  private static Element readFile(final String fileName) throws Exception {
    final File file = new File(fileName);
    checkState(file.isFile());

    final InputStream inputStream = new DataInputStream(new FileInputStream(file));

    return XmlReader.parseDom(fileName, inputStream, new ArrayList<>());
  }

  @Test
  void singleElementList() throws Exception {
    final Element xmlSample = readFile(SINGLE_ELEMENT_LIST);

    final Map<String, List<String>> result = parser.parseVariables(xmlSample);

    assertThat(result, hasEntry("$key$", singletonList("value")));
    assertThat(result.keySet(), hasSize(1));
  }

  @Test
  void manyElementList() throws Exception {
    final Element xmlSample = readFile(MANY_ELEMENT_LIST);

    final Map<String, List<String>> result = parser.parseVariables(xmlSample);

    assertThat(result, hasEntry("$key1$", asList("value1", "value2")));
    assertThat(result, hasEntry("$key2$", asList("value3", "value4")));
    assertThat(result, hasEntry("$no-values$", Collections.emptyList()));
    assertThat(result, hasEntry("$empty-value$", singletonList("")));
    assertThat(result.keySet(), hasSize(4));
  }

  @Test
  void nestedVariable() throws Exception {
    final Element xmlSample = readFile(NESTED_VARIABLE);

    final Map<String, List<String>> result = parser.parseVariables(xmlSample);

    assertThat(result, hasEntry("$nested$", singletonList("nested-value")));
    assertThat(result, hasEntry("$contains-nested$", singletonList("nested-value")));
    assertThat(result, hasEntry("$many-nested$", asList("nested-value", "nested-value")));
    assertThat(result.keySet(), hasSize(3));
  }
}

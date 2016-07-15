package org.camunda.bpm.spring.boot.starter.configuration.impl;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration.join;

import java.util.ArrayList;

import org.junit.Test;

public class AbstractCamundaConfigurationTest {

  @Test
  public void joinLists() {
    assertThat(join(asList("a"), asList("b"))).containsExactly("a", "b");
    assertThat(join(null, asList("b"))).containsExactly("b");
    assertThat(join(new ArrayList<String>(), asList("b"))).containsExactly("b");
    assertThat(join(asList("a"), null)).containsExactly("a");
    assertThat(join(asList("a"), new ArrayList<String>())).containsExactly("a");
    assertThat(join(null, null)).isEmpty();
  }

}

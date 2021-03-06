package org.camunda.bpm.spring.boot.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;
import static org.twdata.maven.mojoexecutor.MojoExecutor.ExecutionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.attribute;
import static org.twdata.maven.mojoexecutor.MojoExecutor.attributes;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

@Mojo(
  name = "create",
  defaultPhase = GENERATE_SOURCES,
  requiresDependencyResolution = COMPILE_PLUS_RUNTIME
)
@SuppressWarnings("WeakerAccess")
public class CamundaWebjarMojo extends AbstractMojo {

  public static final String EE_SUFFIX = "-ee";
  //public static final String WEBJAR_PATH = "/META-INF/resources/webjars/camunda";
  public static final String WEBJAR_PATH = "/";
  public static final String CAMUNDA_SECURITY_FILTER_RULES_JSON = "/securityFilterRules.json";

  @Parameter(property = "project", required = true, readonly = true)
  protected MavenProject project;

  @Parameter(property = "camunda.version", required = true)
  protected String camundaVersion;

  @Parameter(property = "webjar.path", required = true, defaultValue = WEBJAR_PATH)
  protected String webjarPath;

  @Parameter(property = "webjar.webJarSecurityFilterRules", required = true, defaultValue = CAMUNDA_SECURITY_FILTER_RULES_JSON)
  protected String webJarSecurityFilterRules;

  @Parameter(property = "webjar.outputDirectory")
  protected String webJarOutputDirectory;

  @Parameter(property = "session", required = true, readonly = true)
  protected MavenSession session;

  @Component
  protected BuildPluginManager buildPluginManager;

  private final List<Dependency> dependencies = new ArrayList<>();

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    final ExecutionEnvironment environment = executionEnvironment(project, session, buildPluginManager);

    final boolean isEnterprise = camundaVersion.endsWith("-ee");

    final String target = target(Optional.ofNullable(webJarOutputDirectory), project.getBuild().getOutputDirectory(), webjarPath);
    getLog().info("Creating camunda-webjar: " + target);
    getLog().info(String.format("   withProperties: camundaVersion=%s, webjarPath=%s, webJarSecurityFilterRules=%s, warJarOutputDirectory=%s", camundaVersion, webjarPath, webJarSecurityFilterRules, webJarOutputDirectory));


    String webappArtifactId = isEnterprise ? "camunda-webapp-ee-plugins" : "camunda-webapp";

//    if (isEnterprise) {
//      final Dependency dependency = new Dependency();
//      dependency.setGroupId("org.camunda.bpm.webapp");
//      dependency.setArtifactId("camunda-webapp-ee-plugins");
//      dependency.setVersion(camundaVersion);
//      dependency.setType("jar");
//      dependency.setClassifier("classes");
//      dependencies.add(dependency);
//
//      getLog().info("adding dependency: " + ToStringBuilder.reflectionToString(dependency));
//    }

    executeMojo(plugin(groupId("org.apache.maven.plugins"),
      artifactId("maven-dependency-plugin"),
      version("2.10"),
      dependencies),
      goal("unpack"),
      configuration(
        element("artifactItems",
          element("artifactItem",
            element("groupId", "org.camunda.bpm.webapp"),
            element("artifactId", webappArtifactId),
            element("version", camundaVersion),
            element("type", "war"),
            element("overWrite", "true"),
            element("outputDirectory", target),
            element("excludes", "META-INF/**")
          )
        )
      ), environment);

    executeMojo(plugin(groupId("org.apache.maven.plugins"),
      artifactId("maven-antrun-plugin"),
      version("1.8"),
      dependencies),
      goal("run"),
      configuration(
        element("target",
          element("move",
            attributes(
              attribute("file", target + "/WEB-INF/" + CAMUNDA_SECURITY_FILTER_RULES_JSON),
              attribute("toFile", target + leadingSlash(webJarSecurityFilterRules))

            )
          ),
          element("delete",
            attributes(
              attribute("dir", target + "/WEB-INF")
            )
          )
        )
      ), environment);

  }

  static String leadingSlash(String value) {
    return value.startsWith("/") ? value : "/" + value;
  }

  /**
   * Determine the maven target path for webjar generation.
   *
   * @param configuredOutput optional configured outPutPath
   * @param defaultOutput    default output (target/classes)
   * @param webjarPath       internal webjar path, see WEBJAR_PATH
   * @return target director for build (target/classes/&lt;WEBJAR_PATH>
   */
  static String target(Optional<String> configuredOutput, String defaultOutput, String webjarPath) {
    return Optional.of(webjarPath)
      .map(CamundaWebjarMojo::leadingSlash)
      .map(s -> configuredOutput.orElse(defaultOutput) + s)
      .get();
  }
}

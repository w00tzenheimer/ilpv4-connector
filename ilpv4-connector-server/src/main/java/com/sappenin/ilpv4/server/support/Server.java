package com.sappenin.ilpv4.server.support;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.ArrayList;
import java.util.Properties;

/**
 * A Server that runs on a particular port, with various properties. This class is used to be able to operate multiple
 * instances on different ports in the same JVM, which is especially useful for integration testing purposes.
 */
public class Server {

  private final Properties properties;
  private SpringApplication application;
  private ConfigurableApplicationContext context;

  public Server(Class<?>... configurations) {
    this.application = new SpringApplication(configurations);
    this.properties = new Properties();

    final ArrayList<ApplicationContextInitializer<?>> initializers = new ArrayList<>();
    initializers.add(context -> context.getEnvironment().getPropertySources()
      .addFirst(new PropertiesPropertySource("node", properties)));
    application.setInitializers(initializers);
  }

  public void start() {
    context = application.run();
  }

  public void stop() {
    if (context != null) {
      context.close();
    }
  }

  public Server setProperty(String key, String value) {
    this.properties.setProperty(key, value);
    return this;
  }

  public ConfigurableApplicationContext getContext() {
    return context;
  }

  public int getPort() {
    if (context != null) {
      return context.getBean(ConnectorServerSettings.class).getPort();
    } else {
      throw new IllegalStateException("Server not started!");
    }
  }
}

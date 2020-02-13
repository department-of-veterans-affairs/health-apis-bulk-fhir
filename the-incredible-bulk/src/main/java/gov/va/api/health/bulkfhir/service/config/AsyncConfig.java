package gov.va.api.health.bulkfhir.service.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
  public static final String PUBLICATION_BUILD_EXECUTOR = "publicationBuildExecutor";

  /** Provides an execute that will be used for File Building only. */
  @Bean(name = PUBLICATION_BUILD_EXECUTOR)
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(3);
    executor.setMaxPoolSize(3);
    executor.setQueueCapacity(5000);
    executor.setThreadNamePrefix("Publication-Build-");
    executor.initialize();
    return executor;
  }
}

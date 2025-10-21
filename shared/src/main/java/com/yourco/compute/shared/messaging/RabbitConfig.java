package com.yourco.compute.shared.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
  public static final String EXCHANGE = "compute.events";
  public static final String JOB_QUEUE = "compute.jobs";

  @Bean TopicExchange topicExchange() { return new TopicExchange(EXCHANGE, true, false); }
  @Bean Queue jobQueue() { return QueueBuilder.durable(JOB_QUEUE).build(); }
  @Bean Binding jobBinding(TopicExchange ex, Queue jobQueue) { return BindingBuilder.bind(jobQueue).to(ex).with("job.#"); }
}

/*
 * * Copyright 2017 Lookout, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.ecs.provider.config;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.netflix.spectator.api.Registry;
import com.netflix.spinnaker.cats.agent.Agent;
import com.netflix.spinnaker.clouddriver.aws.security.AmazonClientProvider;
import com.netflix.spinnaker.clouddriver.aws.security.NetflixAmazonCredentials;
import com.netflix.spinnaker.clouddriver.ecs.EcsCloudProvider;
import com.netflix.spinnaker.clouddriver.ecs.provider.EcsProvider;
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsRepository;
import com.netflix.spinnaker.clouddriver.security.ProviderUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.netflix.spinnaker.clouddriver.aws.security.AmazonCredentials.AWSRegion;

@Configuration
public class EcsProviderConfig {

  @Bean
  @DependsOn("netflixECSCredentials")
  public EcsProvider ecsProvider(AccountCredentialsRepository accountCredentialsRepository, AmazonClientProvider amazonClientProvider, AWSCredentialsProvider awsCredentialsProvider, Registry registry) {
    EcsProvider provider = new EcsProvider(accountCredentialsRepository, Collections.newSetFromMap(new ConcurrentHashMap<Agent, Boolean>()));
    synchronizeEcsProvider(provider, accountCredentialsRepository, amazonClientProvider, awsCredentialsProvider, registry);
    return provider;
  }

  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  @Bean
  public EcsProviderSynchronizer synchronizeEcsProvider(EcsProvider ecsProvider, AccountCredentialsRepository accountCredentialsRepository,
                                                        AmazonClientProvider amazonClientProvider, AWSCredentialsProvider awsCredentialsProvider, Registry registry) {

    Set<String> scheduledAccounts = ProviderUtils.getScheduledAccounts(ecsProvider);
    Set<NetflixAmazonCredentials> allAccounts = ProviderUtils.buildThreadSafeSetOfAccounts(accountCredentialsRepository, NetflixAmazonCredentials.class);
    List<Agent> newAgents = new LinkedList<>();

    for (NetflixAmazonCredentials credentials : allAccounts) {
      if (credentials.getCloudProvider().equals(EcsCloudProvider.ID)) {

        for (AWSRegion region : credentials.getRegions()) {
          if (!scheduledAccounts.contains(credentials.getName())) {
            //Soon to come:
            //newAgents.add(new EcsClusterCachingAgent(credentials.getName(), region.getName(), amazonClientProvider, awsCredentialsProvider));
          }
        }
      }
    }

    ecsProvider.getAgents().addAll(newAgents);
    ecsProvider.synchronizeHealthAgents();
    return new EcsProviderSynchronizer();
  }

  class EcsProviderSynchronizer {
  }
}

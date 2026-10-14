package br.edu.atitus.currencyapi.configs;

import br.edu.atitus.currencyapi.clients.BcbClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

@Configuration
public class BcbClientConfig {

    @Bean
    BcbClient getBcbClient() {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        // Builder simples (sem @LoadBalanced): a API do BCB é externa e não está registrada no Consul
        RestClient restClient = RestClient.builder()
                .baseUrl("https://olinda.bcb.gov.br")
                .requestFactory(requestFactory)
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(adapter).build();

        return factory.createClient(BcbClient.class);
    }
}

package br.edu.atitus.currencyapi.servicesimpl;

import br.edu.atitus.currencyapi.clients.BcbClient;
import br.edu.atitus.currencyapi.dtos.BcbCotacaoResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
public class BcbClientService {

    private final BcbClient bcbClient;

    public BcbClientService(BcbClient bcbClient) {
        this.bcbClient = bcbClient;
    }

    /**
     * Retorna a cotação de venda (em BRL) da moeda na data informada (formato MM-dd-yyyy).
     * Retorna null quando o BCB responde normalmente, mas sem cotação para a moeda/data.
     * Falhas de comunicação são propagadas como exceção (contam para Retry e Circuit Breaker).
     * Resultados null e exceções não são armazenados no cache.
     */
    @Cacheable(
            value = "bcb-api",
            key = "#moeda + '-' + #dataCotacao",
            unless = "#result == null"
    )
    @Retry(name = "bcb-api")
    @CircuitBreaker(name = "bcb-api")
    public Double getCotacaoVenda(String moeda, String dataCotacao) {
        BcbCotacaoResponse response;
        try {
            response = bcbClient.getCotacaoMoedaDia(
                    "'" + moeda + "'",
                    "'" + dataCotacao + "'");
        } catch (HttpClientErrorException e) {
            // 4xx (exceto limite de requisições): requisição inválida / moeda sem cotação -> ausência de cotação
            if (e.getStatusCode().value() == 429 || e.getStatusCode().value() == 408) {
                throw e;
            }
            return null;
        }
        if (response == null || response.value() == null || response.value().isEmpty()) {
            return null;
        }
        // O último boletim do dia corresponde à PTAX de fechamento
        return response.value().get(response.value().size() - 1).cotacaoVenda();
    }
}

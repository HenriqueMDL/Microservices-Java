package br.edu.atitus.currencyapi.servicesimpl;

import br.edu.atitus.currencyapi.dtos.CurrencyResponse;
import br.edu.atitus.currencyapi.repositories.CurrencyRepository;
import br.edu.atitus.currencyapi.services.CurrencyService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CurrencyServiceJpa implements CurrencyService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyServiceJpa.class);

    private final CurrencyRepository repository;
    private final BcbClientService bcbClientService;

    @Value("${server.port:8080}")
    private String serverPort;

    // Data fixa (MM-dd-yyyy) de um dia útil com cotação disponível no BCB
    @Value("${app.bcb.quotation-date:10-13-2026}")
    private String quotationDate;

    public CurrencyServiceJpa(CurrencyRepository repository,
                              BcbClientService bcbClientService) {
        this.repository = repository;
        this.bcbClientService = bcbClientService;
    }

    @Override
    public CurrencyResponse findBySourceCurrencyAndTargetCurrency(String sourceCurrency, String targetCurrency) throws Exception {
        String source = sourceCurrency.toUpperCase();
        String target = targetCurrency.toUpperCase();
        String environment = "Currency API running in port " + serverPort;

        Double rate = null;
        try {
            rate = getRateFromBcb(source, target);
        } catch (Exception e) {
            // Falha de comunicação, circuito aberto etc. -> fallback para o banco local
            log.warn("Falha ao consultar o BCB ({} -> {}): {}", source, target, e.toString());
        }

        if (rate != null) {
            return new CurrencyResponse(source, target, rate, environment + " - source: BCB");
        }

        // Fallback: falha na comunicação externa ou ausência de cotação no BCB
        var currency = repository.findBySourceCurrencyAndTargetCurrency(source, target)
                .orElseThrow(() -> new EntityNotFoundException("Currency não encontrado"));
        return new CurrencyResponse(
                currency.getSourceCurrency(),
                currency.getTargetCurrency(),
                currency.getConversionRate(),
                environment + " - source: local database (fallback)");
    }

    // O BCB informa quantos BRL vale 1 unidade da moeda; a taxa entre duas moedas usa o BRL como pivô.
    private Double getRateFromBcb(String source, String target) {
        Double sourceInBrl = getRateInBrl(source);
        if (sourceInBrl == null) return null;
        Double targetInBrl = getRateInBrl(target);
        if (targetInBrl == null || targetInBrl == 0) return null;
        return sourceInBrl / targetInBrl;
    }

    private Double getRateInBrl(String currency) {
        if (currency.equals("BRL")) return 1.0;
        return bcbClientService.getCotacaoVenda(currency, quotationDate);
    }
}

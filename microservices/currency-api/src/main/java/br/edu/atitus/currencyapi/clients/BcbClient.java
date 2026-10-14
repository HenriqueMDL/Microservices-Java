package br.edu.atitus.currencyapi.clients;

import br.edu.atitus.currencyapi.dtos.BcbCotacaoResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface BcbClient {

    // Os valores de moeda e dataCotacao devem ser enviados entre aspas simples (padrão OData)
    @GetExchange("/olinda/servico/PTAX/versao/v1/odata/CotacaoMoedaDia(moeda=@moeda,dataCotacao=@dataCotacao)"
            + "?@moeda={moeda}&@dataCotacao={dataCotacao}&$format=json")
    BcbCotacaoResponse getCotacaoMoedaDia(
            @PathVariable String moeda,
            @PathVariable String dataCotacao);
}

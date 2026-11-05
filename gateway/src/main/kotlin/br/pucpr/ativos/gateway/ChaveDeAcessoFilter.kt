package br.pucpr.ativos.gateway

import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Segurança na entrada: toda requisição encaminhada ao BFF precisa do cabeçalho X-API-Key
 * com a chave configurada em GATEWAY_API_KEY. Sem a chave configurada (desenvolvimento local),
 * o filtro não bloqueia nada. As requisições de pré-verificação do CORS (OPTIONS) passam direto.
 */
@Component
class ChaveDeAcessoFilter(
    @Value("\${gateway.api-key:}") private val chaveEsperada: String,
) : GlobalFilter, Ordered {

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val requisicao = exchange.request
        if (chaveEsperada.isBlank() || requisicao.method == HttpMethod.OPTIONS) {
            return chain.filter(exchange)
        }
        if (requisicao.headers.getFirst(CABECALHO) == chaveEsperada) {
            return chain.filter(exchange)
        }

        val resposta = exchange.response
        resposta.setStatusCode(HttpStatus.UNAUTHORIZED)
        resposta.headers.contentType = MediaType.APPLICATION_JSON
        val corpo = """{"error":"Chave de acesso ausente ou inválida. Envie o cabeçalho $CABECALHO."}"""
            .toByteArray(Charsets.UTF_8)
        return resposta.writeWith(Mono.just(resposta.bufferFactory().wrap(corpo)))
    }

    // Roda antes do roteamento.
    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    companion object {
        const val CABECALHO = "X-API-Key"
    }
}

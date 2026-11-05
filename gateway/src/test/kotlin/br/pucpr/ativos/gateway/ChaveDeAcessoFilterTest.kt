package br.pucpr.ativos.gateway

import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import reactor.core.publisher.Mono
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** O filtro de chave de acesso, testado sem subir o gateway. */
class ChaveDeAcessoFilterTest {

    private var encaminhou = false
    private val cadeia = GatewayFilterChain {
        encaminhou = true
        Mono.empty()
    }

    @Test
    fun `sem a chave a requisicao e recusada com 401`() {
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/ativos"))

        ChaveDeAcessoFilter("segredo").filter(exchange, cadeia).block()

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
        assertFalse(encaminhou)
    }

    @Test
    fun `com a chave correta a requisicao segue para o BFF`() {
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/ativos").header(ChaveDeAcessoFilter.CABECALHO, "segredo"),
        )

        ChaveDeAcessoFilter("segredo").filter(exchange, cadeia).block()

        assertTrue(encaminhou)
    }

    @Test
    fun `pre-verificacao do CORS passa sem chave`() {
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.options("/api/ativos"))

        ChaveDeAcessoFilter("segredo").filter(exchange, cadeia).block()

        assertTrue(encaminhou)
    }

    @Test
    fun `sem chave configurada nada e bloqueado`() {
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/ativos"))

        ChaveDeAcessoFilter("").filter(exchange, cadeia).block()

        assertTrue(encaminhou)
    }
}

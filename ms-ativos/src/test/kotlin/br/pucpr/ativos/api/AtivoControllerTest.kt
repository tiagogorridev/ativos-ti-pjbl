package br.pucpr.ativos.api

import br.pucpr.ativos.api.ativos.AtivoController
import br.pucpr.ativos.api.erros.ApiExceptionHandler
import br.pucpr.ativos.apoio.AtivoRepositoryEmMemoria
import br.pucpr.ativos.apoio.ManutencaoRepositoryEmMemoria
import br.pucpr.ativos.apoio.umAtivo
import br.pucpr.ativos.application.ativos.AtualizarAtivo
import br.pucpr.ativos.application.ativos.CriarAtivo
import br.pucpr.ativos.application.ativos.ExcluirAtivo
import br.pucpr.ativos.application.ativos.ListarAtivos
import br.pucpr.ativos.application.ativos.ObterAtivo
import br.pucpr.ativos.application.ativos.RegistrarDevolucaoDoAtivo
import br.pucpr.ativos.application.ativos.RegistrarUsoDoAtivo
import br.pucpr.ativos.domain.ativo.StatusAtivo
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import kotlin.test.Test

/**
 * Teste da camada HTTP com os casos de uso reais e repositórios em memória:
 * valida o contrato JSON usado pelo frontend e os códigos de status, sem subir o Spring nem o MongoDB.
 */
class AtivoControllerTest {

    private val ativos = AtivoRepositoryEmMemoria(
        umAtivo(numeroPatrimonio = "PAT-0001", id = 1, status = StatusAtivo.EM_USO),
        umAtivo(numeroPatrimonio = "PAT-0003", id = 3),
    )
    private val manutencoes = ManutencaoRepositoryEmMemoria()

    private val mvc: MockMvc = MockMvcBuilders
        .standaloneSetup(
            AtivoController(
                ListarAtivos(ativos),
                ObterAtivo(ativos),
                CriarAtivo(ativos),
                AtualizarAtivo(ativos),
                ExcluirAtivo(ativos, manutencoes),
                RegistrarUsoDoAtivo(ativos),
                RegistrarDevolucaoDoAtivo(ativos),
            ),
        )
        .setControllerAdvice(ApiExceptionHandler())
        .build()

    @Test
    fun `GET lista os ativos no formato do frontend`() {
        mvc.perform(get("/api/ativos"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].numeroPatrimonio").value("PAT-0001"))
            .andExpect(jsonPath("$[0].status").value("Em uso"))
            .andExpect(jsonPath("$[0].colaborador").value("Marina Alves"))
    }

    @Test
    fun `GET filtra por status`() {
        mvc.perform(get("/api/ativos").param("status", "Em estoque"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].numeroPatrimonio").value("PAT-0003"))
    }

    @Test
    fun `GET de patrimonio inexistente devolve 404 com o campo error`() {
        mvc.perform(get("/api/ativos/PAT-0404"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Nenhum ativo encontrado com o patrimônio PAT-0404."))
    }

    @Test
    fun `POST cadastra e devolve 201`() {
        mvc.perform(
            post("/api/ativos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"numeroPatrimonio":"PAT-0100","tipo":"Monitor","marca":"LG","modelo":"24MK430H",
                     "numeroSerie":"LG0001","dataAquisicao":"2024-01-10","valorAquisicao":899.00}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(4))
            .andExpect(jsonPath("$.status").value("Em estoque"))
    }

    @Test
    fun `POST sem campo obrigatorio devolve 400 indicando o campo`() {
        mvc.perform(
            post("/api/ativos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"numeroPatrimonio":"PAT-0100","tipo":"Monitor","dataAquisicao":"2024-01-10"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.campo").value("marca"))
    }

    @Test
    fun `POST com patrimonio duplicado devolve 409`() {
        mvc.perform(
            post("/api/ativos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"numeroPatrimonio":"PAT-0003","tipo":"Monitor","marca":"LG","modelo":"24MK430H",
                     "numeroSerie":"LG0001","dataAquisicao":"2024-01-10","valorAquisicao":899.00}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").exists())
    }

    @Test
    fun `PUT altera parcialmente o ativo`() {
        mvc.perform(
            put("/api/ativos/PAT-0003")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"Em manutenção"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("Em manutenção"))
            .andExpect(jsonPath("$.marca").value("Dell"))
    }

    @Test
    fun `DELETE de ativo alocado devolve 409`() {
        mvc.perform(delete("/api/ativos/PAT-0001"))
            .andExpect(status().isConflict)
    }

    @Test
    fun `DELETE de ativo sem historico confirma a exclusao`() {
        mvc.perform(delete("/api/ativos/PAT-0003"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.mensagem").value("Ativo PAT-0003 excluído com sucesso."))
    }

    @Test
    fun `PUT uso marca o ativo como em uso e DELETE uso devolve ao estoque`() {
        mvc.perform(
            put("/api/ativos/PAT-0003/uso")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"colaborador":"Eduardo Lima","setor":"TI"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("Em uso"))
            .andExpect(jsonPath("$.colaborador").value("Eduardo Lima"))

        mvc.perform(delete("/api/ativos/PAT-0003/uso"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("Em estoque"))
    }
}

package br.pucpr.alocacoes.api

import br.pucpr.alocacoes.api.alocacoes.AlocacaoController
import br.pucpr.alocacoes.api.colaboradores.ColaboradorController
import br.pucpr.alocacoes.api.erros.ApiExceptionHandler
import br.pucpr.alocacoes.apoio.AlocacaoRepositoryEmMemoria
import br.pucpr.alocacoes.apoio.ColaboradorRepositoryEmMemoria
import br.pucpr.alocacoes.apoio.RELOGIO_FIXO
import br.pucpr.alocacoes.apoio.umColaborador
import br.pucpr.alocacoes.apoio.umaAlocacao
import br.pucpr.alocacoes.application.alocacoes.AlocarAtivo
import br.pucpr.alocacoes.application.alocacoes.AtualizarAlocacao
import br.pucpr.alocacoes.application.alocacoes.DevolverAtivo
import br.pucpr.alocacoes.application.alocacoes.ExcluirAlocacao
import br.pucpr.alocacoes.application.alocacoes.ListarAlocacoes
import br.pucpr.alocacoes.application.alocacoes.ObterAlocacao
import br.pucpr.alocacoes.application.colaboradores.AtualizarColaborador
import br.pucpr.alocacoes.application.colaboradores.CadastrarColaborador
import br.pucpr.alocacoes.application.colaboradores.InativarColaborador
import br.pucpr.alocacoes.application.colaboradores.ListarColaboradores
import br.pucpr.alocacoes.application.colaboradores.ObterColaborador
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import kotlin.test.Test

/**
 * Teste da camada HTTP com os casos de uso reais e repositórios em memória:
 * contrato JSON e códigos de status, sem subir o Spring nem o banco.
 */
class ControllersTest {

    private val marina = umColaborador(id = 1)
    private val colaboradores = ColaboradorRepositoryEmMemoria(marina, umColaborador(id = 4, nome = "Eduardo Lima", matricula = "0029045", setor = "TI"))
    private val alocacoes = AlocacaoRepositoryEmMemoria(umaAlocacao(id = 1, numeroPatrimonio = "PAT-0001", colaborador = marina))

    private val mvc: MockMvc = MockMvcBuilders
        .standaloneSetup(
            ColaboradorController(
                ListarColaboradores(colaboradores, alocacoes),
                ObterColaborador(colaboradores, alocacoes),
                CadastrarColaborador(colaboradores),
                AtualizarColaborador(colaboradores, alocacoes),
                InativarColaborador(colaboradores, alocacoes),
            ),
            AlocacaoController(
                ListarAlocacoes(alocacoes),
                ObterAlocacao(alocacoes),
                AlocarAtivo(colaboradores, alocacoes, RELOGIO_FIXO),
                AtualizarAlocacao(alocacoes),
                DevolverAtivo(alocacoes, RELOGIO_FIXO),
                ExcluirAlocacao(alocacoes),
            ),
        )
        .setControllerAdvice(ApiExceptionHandler())
        .build()

    @Test
    fun `GET colaboradores traz a situacao e a contagem de ativos`() {
        mvc.perform(get("/api/colaboradores"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[1].nome").value("Marina Alves"))
            .andExpect(jsonPath("$[1].situacao").value("Ativo"))
            .andExpect(jsonPath("$[1].ativosAlocados").value(1))
    }

    @Test
    fun `POST colaborador sem nome devolve 400 indicando o campo`() {
        mvc.perform(
            post("/api/colaboradores")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"matricula":"0099999","setor":"TI"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.campo").value("nome"))
    }

    @Test
    fun `DELETE de colaborador com ativo alocado devolve 409`() {
        mvc.perform(delete("/api/colaboradores/1"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").exists())
    }

    @Test
    fun `GET alocacoes vigentes no formato do frontend`() {
        mvc.perform(get("/api/alocacoes").param("vigentes", "true"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].numeroPatrimonio").value("PAT-0001"))
            .andExpect(jsonPath("$[0].colaborador").value("Marina Alves"))
            .andExpect(jsonPath("$[0].setor").value("Financeiro"))
            .andExpect(jsonPath("$[0].vigente").value(true))
    }

    @Test
    fun `POST alocacao devolve 201 e a segunda alocacao do mesmo ativo devolve 409`() {
        val corpo = """{"numeroPatrimonio":"PAT-0003","equipamento":"LG 24MK430H","colaboradorId":4}"""

        mvc.perform(post("/api/alocacoes").contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(2))
            .andExpect(jsonPath("$.colaborador").value("Eduardo Lima"))

        mvc.perform(post("/api/alocacoes").contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").exists())
    }

    @Test
    fun `POST devolucao encerra a alocacao`() {
        mvc.perform(post("/api/alocacoes/1/devolucao").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.vigente").value(false))
    }

    @Test
    fun `GET de alocacao inexistente devolve 404`() {
        mvc.perform(get("/api/alocacoes/99"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Nenhuma alocação encontrada com o id 99."))
    }
}

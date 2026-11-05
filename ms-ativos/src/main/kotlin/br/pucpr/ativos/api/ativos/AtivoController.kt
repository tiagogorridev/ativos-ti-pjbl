package br.pucpr.ativos.api.ativos

import br.pucpr.ativos.application.ativos.AtualizarAtivo
import br.pucpr.ativos.application.ativos.CriarAtivo
import br.pucpr.ativos.application.ativos.ExcluirAtivo
import br.pucpr.ativos.application.ativos.ListarAtivos
import br.pucpr.ativos.application.ativos.ObterAtivo
import br.pucpr.ativos.application.ativos.RegistrarDevolucaoDoAtivo
import br.pucpr.ativos.application.ativos.RegistrarUsoDoAtivo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** Endpoints de Ativos. Só converte HTTP ⇄ comandos e delega a cada caso de uso. */
@RestController
@RequestMapping("/api/ativos")
@Tag(name = "Ativos", description = "Cadastro e ciclo de vida dos ativos de TI (MongoDB)")
class AtivoController(
    private val listarAtivos: ListarAtivos,
    private val obterAtivo: ObterAtivo,
    private val criarAtivo: CriarAtivo,
    private val atualizarAtivo: AtualizarAtivo,
    private val excluirAtivo: ExcluirAtivo,
    private val registrarUsoDoAtivo: RegistrarUsoDoAtivo,
    private val registrarDevolucaoDoAtivo: RegistrarDevolucaoDoAtivo,
) {

    @GetMapping
    @Operation(summary = "Pesquisa ativos", description = "Filtra por texto livre (busca) e por status. status=Todos desliga o filtro.")
    fun listar(
        @RequestParam("busca", required = false) busca: String?,
        @RequestParam("status", required = false) status: String?,
    ): List<AtivoResponse> = listarAtivos.executar(busca, status).map { it.paraResposta() }

    @GetMapping("/{numeroPatrimonio}")
    @Operation(
        summary = "Consulta um ativo pelo número de patrimônio",
        responses = [
            ApiResponse(responseCode = "200", description = "Ativo encontrado"),
            ApiResponse(responseCode = "404", description = "Patrimônio não cadastrado"),
        ],
    )
    fun obter(@PathVariable("numeroPatrimonio") numeroPatrimonio: String): AtivoResponse =
        obterAtivo.executar(numeroPatrimonio).paraResposta()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Cadastra um ativo",
        responses = [
            ApiResponse(responseCode = "201", description = "Ativo cadastrado"),
            ApiResponse(responseCode = "400", description = "Campo obrigatório ausente ou inválido"),
            ApiResponse(responseCode = "409", description = "Patrimônio já cadastrado ou status 'Em uso' informado"),
        ],
    )
    fun criar(@RequestBody(required = false) request: NovoAtivoRequest?): AtivoResponse =
        criarAtivo.executar((request ?: NovoAtivoRequest()).paraComando()).paraResposta()

    @PutMapping("/{numeroPatrimonio}")
    @Operation(
        summary = "Altera um ativo (atualização parcial)",
        description = "Também troca o status manualmente (manutenção, estoque, baixa). 'Em uso' só pela alocação.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ativo alterado"),
            ApiResponse(responseCode = "404", description = "Patrimônio não cadastrado"),
            ApiResponse(responseCode = "409", description = "Troca de status não permitida"),
        ],
    )
    fun atualizar(
        @PathVariable("numeroPatrimonio") numeroPatrimonio: String,
        @RequestBody(required = false) request: AtualizarAtivoRequest?,
    ): AtivoResponse =
        atualizarAtivo.executar(numeroPatrimonio, (request ?: AtualizarAtivoRequest()).paraComando()).paraResposta()

    @DeleteMapping("/{numeroPatrimonio}")
    @Operation(
        summary = "Exclui um ativo cadastrado por engano",
        responses = [
            ApiResponse(responseCode = "200", description = "Ativo excluído"),
            ApiResponse(responseCode = "409", description = "Ativo alocado ou com histórico de manutenção"),
        ],
    )
    fun excluir(@PathVariable("numeroPatrimonio") numeroPatrimonio: String): MensagemResponse {
        excluirAtivo.executar(numeroPatrimonio)
        return MensagemResponse("Ativo $numeroPatrimonio excluído com sucesso.")
    }

    @PutMapping("/{numeroPatrimonio}/uso")
    @Operation(
        summary = "Marca o ativo como 'Em uso' por um colaborador",
        description = "Chamado pelo serviço de Alocações depois de gravar a alocação.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ativo em uso"),
            ApiResponse(responseCode = "409", description = "Ativo não está 'Em estoque'"),
        ],
    )
    fun registrarUso(
        @PathVariable("numeroPatrimonio") numeroPatrimonio: String,
        @RequestBody(required = false) request: UsoRequest?,
    ): AtivoResponse =
        registrarUsoDoAtivo.executar(numeroPatrimonio, (request ?: UsoRequest()).paraComando()).paraResposta()

    @DeleteMapping("/{numeroPatrimonio}/uso")
    @Operation(
        summary = "Devolve o ativo ao estoque",
        description = "Chamado pelo serviço de Alocações depois de registrar a devolução.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ativo de volta ao estoque"),
            ApiResponse(responseCode = "409", description = "Ativo não está 'Em uso'"),
        ],
    )
    fun registrarDevolucao(@PathVariable("numeroPatrimonio") numeroPatrimonio: String): AtivoResponse =
        registrarDevolucaoDoAtivo.executar(numeroPatrimonio).paraResposta()
}

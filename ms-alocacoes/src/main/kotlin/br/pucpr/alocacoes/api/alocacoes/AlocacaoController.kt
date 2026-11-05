package br.pucpr.alocacoes.api.alocacoes

import br.pucpr.alocacoes.application.alocacoes.AlocarAtivo
import br.pucpr.alocacoes.application.alocacoes.AtualizarAlocacao
import br.pucpr.alocacoes.application.alocacoes.DevolverAtivo
import br.pucpr.alocacoes.application.alocacoes.ExcluirAlocacao
import br.pucpr.alocacoes.application.alocacoes.ListarAlocacoes
import br.pucpr.alocacoes.application.alocacoes.ObterAlocacao
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

/** Endpoints de Alocações. Só converte HTTP ⇄ comandos e delega a cada caso de uso. */
@RestController
@RequestMapping("/api/alocacoes")
@Tag(name = "Alocações", description = "Entrega e devolução de ativos aos colaboradores (Azure SQL)")
class AlocacaoController(
    private val listarAlocacoes: ListarAlocacoes,
    private val obterAlocacao: ObterAlocacao,
    private val alocarAtivo: AlocarAtivo,
    private val atualizarAlocacao: AtualizarAlocacao,
    private val devolverAtivo: DevolverAtivo,
    private val excluirAlocacao: ExcluirAlocacao,
) {

    @GetMapping
    @Operation(
        summary = "Lista alocações",
        description = "vigentes=true só as abertas, vigentes=false só o histórico; sem o parâmetro, todas.",
    )
    fun listar(
        @RequestParam("vigentes", required = false) vigentes: Boolean?,
        @RequestParam("colaboradorId", required = false) colaboradorId: Long?,
    ): List<AlocacaoResponse> = listarAlocacoes.executar(vigentes, colaboradorId).map { it.paraResposta() }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta uma alocação")
    fun obter(@PathVariable("id") id: Long): AlocacaoResponse = obterAlocacao.executar(id).paraResposta()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Aloca um ativo a um colaborador",
        responses = [
            ApiResponse(responseCode = "201", description = "Alocação registrada"),
            ApiResponse(responseCode = "400", description = "Campo obrigatório ausente"),
            ApiResponse(responseCode = "404", description = "Colaborador não cadastrado"),
            ApiResponse(responseCode = "409", description = "Ativo já alocado ou colaborador inativo"),
        ],
    )
    fun alocar(@RequestBody(required = false) request: NovaAlocacaoRequest?): AlocacaoResponse =
        alocarAtivo.executar((request ?: NovaAlocacaoRequest()).paraComando()).paraResposta()

    @PutMapping("/{id}")
    @Operation(
        summary = "Corrige uma alocação vigente (atualização parcial)",
        responses = [
            ApiResponse(responseCode = "200", description = "Alocação alterada"),
            ApiResponse(responseCode = "409", description = "Alocação já encerrada"),
        ],
    )
    fun atualizar(
        @PathVariable("id") id: Long,
        @RequestBody(required = false) request: AtualizarAlocacaoRequest?,
    ): AlocacaoResponse =
        atualizarAlocacao.executar(id, (request ?: AtualizarAlocacaoRequest()).paraComando()).paraResposta()

    @PostMapping("/{id}/devolucao")
    @Operation(
        summary = "Registra a devolução do ativo",
        description = "Encerra a alocação; sem dataDevolucao, usa a data de hoje.",
        responses = [
            ApiResponse(responseCode = "200", description = "Devolução registrada"),
            ApiResponse(responseCode = "409", description = "Alocação já encerrada"),
        ],
    )
    fun devolver(
        @PathVariable("id") id: Long,
        @RequestBody(required = false) request: DevolucaoRequest?,
    ): AlocacaoResponse = devolverAtivo.executar(id, request?.dataDevolucao).paraResposta()

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Exclui uma alocação vigente lançada por engano",
        responses = [
            ApiResponse(responseCode = "200", description = "Alocação excluída"),
            ApiResponse(responseCode = "409", description = "Alocação encerrada (histórico)"),
        ],
    )
    fun excluir(@PathVariable("id") id: Long): MensagemResponse {
        excluirAlocacao.executar(id)
        return MensagemResponse("Alocação $id excluída com sucesso.")
    }
}

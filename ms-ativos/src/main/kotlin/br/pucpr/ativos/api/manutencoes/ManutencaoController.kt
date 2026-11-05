package br.pucpr.ativos.api.manutencoes

import br.pucpr.ativos.api.ativos.MensagemResponse
import br.pucpr.ativos.application.manutencoes.AtualizarManutencao
import br.pucpr.ativos.application.manutencoes.ExcluirManutencao
import br.pucpr.ativos.application.manutencoes.ListarManutencoes
import br.pucpr.ativos.application.manutencoes.ObterManutencao
import br.pucpr.ativos.application.manutencoes.RegistrarManutencao
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

/** Endpoints de Manutenções. Só converte HTTP ⇄ comandos e delega a cada caso de uso. */
@RestController
@RequestMapping("/api/manutencoes")
@Tag(name = "Manutenções", description = "Manutenções e custos por ativo (MongoDB)")
class ManutencaoController(
    private val listarManutencoes: ListarManutencoes,
    private val obterManutencao: ObterManutencao,
    private val registrarManutencao: RegistrarManutencao,
    private val atualizarManutencao: AtualizarManutencao,
    private val excluirManutencao: ExcluirManutencao,
) {

    @GetMapping
    @Operation(summary = "Lista o histórico de manutenções", description = "Mais recentes primeiro; filtra por numeroPatrimonio.")
    fun listar(@RequestParam("numeroPatrimonio", required = false) numeroPatrimonio: String?): List<ManutencaoResponse> =
        listarManutencoes.executar(numeroPatrimonio).map { it.paraResposta() }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta uma manutenção")
    fun obter(@PathVariable("id") id: Long): ManutencaoResponse = obterManutencao.executar(id).paraResposta()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Registra uma manutenção",
        responses = [
            ApiResponse(responseCode = "201", description = "Manutenção registrada"),
            ApiResponse(responseCode = "400", description = "Campo obrigatório ausente ou inválido"),
            ApiResponse(responseCode = "404", description = "Ativo não cadastrado"),
            ApiResponse(responseCode = "409", description = "Ativo baixado"),
        ],
    )
    fun registrar(@RequestBody(required = false) request: NovaManutencaoRequest?): ManutencaoResponse =
        registrarManutencao.executar((request ?: NovaManutencaoRequest()).paraComando()).paraResposta()

    @PutMapping("/{id}")
    @Operation(summary = "Corrige uma manutenção (atualização parcial)")
    fun atualizar(
        @PathVariable("id") id: Long,
        @RequestBody(required = false) request: AtualizarManutencaoRequest?,
    ): ManutencaoResponse =
        atualizarManutencao.executar(id, (request ?: AtualizarManutencaoRequest()).paraComando()).paraResposta()

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui uma manutenção lançada por engano")
    fun excluir(@PathVariable("id") id: Long): MensagemResponse {
        excluirManutencao.executar(id)
        return MensagemResponse("Manutenção $id excluída com sucesso.")
    }
}

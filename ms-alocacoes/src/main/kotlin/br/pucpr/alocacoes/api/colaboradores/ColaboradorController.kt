package br.pucpr.alocacoes.api.colaboradores

import br.pucpr.alocacoes.application.colaboradores.AtualizarColaborador
import br.pucpr.alocacoes.application.colaboradores.CadastrarColaborador
import br.pucpr.alocacoes.application.colaboradores.InativarColaborador
import br.pucpr.alocacoes.application.colaboradores.ListarColaboradores
import br.pucpr.alocacoes.application.colaboradores.ObterColaborador
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** Endpoints de Colaboradores. Só converte HTTP ⇄ comandos e delega a cada caso de uso. */
@RestController
@RequestMapping("/api/colaboradores")
@Tag(name = "Colaboradores", description = "Cadastro dos colaboradores que recebem ativos (Azure SQL)")
class ColaboradorController(
    private val listarColaboradores: ListarColaboradores,
    private val obterColaborador: ObterColaborador,
    private val cadastrarColaborador: CadastrarColaborador,
    private val atualizarColaborador: AtualizarColaborador,
    private val inativarColaborador: InativarColaborador,
) {

    @GetMapping
    @Operation(summary = "Lista os colaboradores com a quantidade de ativos alocados")
    fun listar(): List<ColaboradorResponse> = listarColaboradores.executar().map { it.paraResposta() }

    @GetMapping("/{id}")
    @Operation(
        summary = "Consulta um colaborador",
        responses = [
            ApiResponse(responseCode = "200", description = "Colaborador encontrado"),
            ApiResponse(responseCode = "404", description = "Colaborador não cadastrado"),
        ],
    )
    fun obter(@PathVariable("id") id: Long): ColaboradorResponse = obterColaborador.executar(id).paraResposta()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Cadastra um colaborador",
        responses = [
            ApiResponse(responseCode = "201", description = "Colaborador cadastrado"),
            ApiResponse(responseCode = "400", description = "Campo obrigatório ausente"),
            ApiResponse(responseCode = "409", description = "Matrícula já cadastrada"),
        ],
    )
    fun cadastrar(@RequestBody(required = false) request: NovoColaboradorRequest?): ColaboradorResponse =
        cadastrarColaborador.executar((request ?: NovoColaboradorRequest()).paraComando()).paraResposta(0)

    @PutMapping("/{id}")
    @Operation(
        summary = "Altera um colaborador (atualização parcial)",
        responses = [
            ApiResponse(responseCode = "200", description = "Colaborador alterado"),
            ApiResponse(responseCode = "409", description = "Matrícula repetida ou inativação com ativo alocado"),
        ],
    )
    fun atualizar(
        @PathVariable("id") id: Long,
        @RequestBody(required = false) request: AtualizarColaboradorRequest?,
    ): ColaboradorResponse {
        atualizarColaborador.executar(id, (request ?: AtualizarColaboradorRequest()).paraComando())
        return obterColaborador.executar(id).paraResposta()
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Inativa um colaborador (exclusão lógica)",
        description = "O registro não é apagado, para preservar o histórico de alocações.",
        responses = [
            ApiResponse(responseCode = "200", description = "Colaborador inativado"),
            ApiResponse(responseCode = "409", description = "Colaborador com ativo alocado"),
        ],
    )
    fun inativar(@PathVariable("id") id: Long): ColaboradorResponse =
        inativarColaborador.executar(id).paraResposta(0)
}

package br.pucpr.ativos.api.erros

import br.pucpr.ativos.domain.ConflitoException
import br.pucpr.ativos.domain.RecursoNaoEncontradoException
import br.pucpr.ativos.domain.ValidacaoException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

/**
 * Tradução centralizada das exceções de negócio para HTTP. Restrito aos controllers deste
 * serviço, para não interferir no Swagger. Detalhes internos nunca são devolvidos ao cliente.
 */
@RestControllerAdvice(basePackages = ["br.pucpr.ativos.api"])
class ApiExceptionHandler {

    private val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    @ExceptionHandler(RecursoNaoEncontradoException::class)
    fun naoEncontrado(e: RecursoNaoEncontradoException): ResponseEntity<ApiError> =
        resposta(HttpStatus.NOT_FOUND, e.message)

    @ExceptionHandler(ConflitoException::class)
    fun conflito(e: ConflitoException): ResponseEntity<ApiError> =
        resposta(HttpStatus.CONFLICT, e.message)

    @ExceptionHandler(ValidacaoException::class)
    fun validacao(e: ValidacaoException): ResponseEntity<ApiError> =
        resposta(HttpStatus.BAD_REQUEST, e.message, e.campo)

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun corpoInvalido(e: HttpMessageNotReadableException): ResponseEntity<ApiError> {
        log.debug("Corpo da requisição ilegível", e)
        return resposta(
            HttpStatus.BAD_REQUEST,
            "Corpo da requisição inválido. Envie um JSON válido; datas no formato AAAA-MM-DD.",
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun parametroInvalido(e: MethodArgumentTypeMismatchException): ResponseEntity<ApiError> =
        resposta(HttpStatus.BAD_REQUEST, "Parâmetro '${e.name}' inválido.", e.name)

    @ExceptionHandler(RuntimeException::class)
    fun inesperado(e: RuntimeException): ResponseEntity<ApiError> {
        log.error("Erro inesperado", e)
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor.")
    }

    private fun resposta(status: HttpStatus, mensagem: String?, campo: String? = null): ResponseEntity<ApiError> =
        ResponseEntity.status(status).body(ApiError(mensagem ?: status.reasonPhrase, campo))
}

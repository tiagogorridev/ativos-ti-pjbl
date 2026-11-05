package br.pucpr.ativos.application.ativos

import br.pucpr.ativos.apoio.AtivoRepositoryEmMemoria
import br.pucpr.ativos.apoio.ManutencaoRepositoryEmMemoria
import br.pucpr.ativos.apoio.umAtivo
import br.pucpr.ativos.apoio.umaManutencao
import br.pucpr.ativos.domain.ConflitoException
import br.pucpr.ativos.domain.RecursoNaoEncontradoException
import br.pucpr.ativos.domain.ValidacaoException
import br.pucpr.ativos.domain.ativo.StatusAtivo
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Casos de uso de Ativos com repositórios em memória (sem MongoDB). */
class CasosDeUsoDeAtivoTest {

    private val comandoValido = CriarAtivoComando(
        numeroPatrimonio = "PAT-0100",
        tipo = "Notebook",
        marca = "Lenovo",
        modelo = "IdeaPad 3",
        numeroSerie = "LNV0341",
        dataAquisicao = LocalDate.of(2024, 4, 2),
        valorAquisicao = BigDecimal("3450.00"),
    )

    @Test
    fun `cadastra ativo novo em estoque com o proximo id`() {
        val ativos = AtivoRepositoryEmMemoria(umAtivo(id = 7))

        val criado = CriarAtivo(ativos).executar(comandoValido)

        assertEquals(8, criado.id)
        assertEquals(StatusAtivo.EM_ESTOQUE, criado.status)
        assertTrue(ativos.existePatrimonio("PAT-0100"))
    }

    @Test
    fun `patrimonio duplicado e recusado`() {
        val ativos = AtivoRepositoryEmMemoria(umAtivo(numeroPatrimonio = "PAT-0100"))

        assertFailsWith<ConflitoException> { CriarAtivo(ativos).executar(comandoValido) }
    }

    @Test
    fun `campo obrigatorio ausente informa qual campo corrigir`() {
        val erro = assertFailsWith<ValidacaoException> {
            CriarAtivo(AtivoRepositoryEmMemoria()).executar(comandoValido.copy(marca = " "))
        }
        assertEquals("marca", erro.campo)
    }

    @Test
    fun `ativo novo nao pode nascer em uso`() {
        assertFailsWith<ConflitoException> {
            CriarAtivo(AtivoRepositoryEmMemoria()).executar(comandoValido.copy(status = "Em uso"))
        }
    }

    @Test
    fun `valor de aquisicao negativo e recusado`() {
        val erro = assertFailsWith<ValidacaoException> {
            CriarAtivo(AtivoRepositoryEmMemoria()).executar(comandoValido.copy(valorAquisicao = BigDecimal("-1")))
        }
        assertEquals("valorAquisicao", erro.campo)
    }

    @Test
    fun `pesquisa filtra por texto e por status e ignora o filtro Todos`() {
        val ativos = AtivoRepositoryEmMemoria(
            umAtivo(numeroPatrimonio = "PAT-0001", id = 1),
            umAtivo(numeroPatrimonio = "PAT-0002", id = 2, status = StatusAtivo.EM_USO),
        )
        val listar = ListarAtivos(ativos)

        assertEquals(listOf("PAT-0002"), listar.executar(null, "Em uso").map { it.numeroPatrimonio })
        assertEquals(listOf("PAT-0001"), listar.executar("0001", null).map { it.numeroPatrimonio })
        assertEquals(2, listar.executar(null, "Todos").size)
    }

    @Test
    fun `atualizacao parcial mantem os campos nao enviados`() {
        val ativos = AtivoRepositoryEmMemoria(umAtivo())

        val alterado = AtualizarAtivo(ativos).executar("PAT-0001", AtualizarAtivoComando(modelo = "Latitude 7440"))

        assertEquals("Latitude 7440", alterado.modelo)
        assertEquals("Dell", alterado.marca)
        assertEquals("Latitude 7440", ativos.buscarPorPatrimonio("PAT-0001")?.modelo)
    }

    @Test
    fun `atualizacao pode baixar o ativo`() {
        val ativos = AtivoRepositoryEmMemoria(umAtivo())

        val baixado = AtualizarAtivo(ativos).executar("PAT-0001", AtualizarAtivoComando(status = "Baixado"))

        assertEquals(StatusAtivo.BAIXADO, baixado.status)
    }

    @Test
    fun `atualizar ativo inexistente devolve nao encontrado`() {
        assertFailsWith<RecursoNaoEncontradoException> {
            AtualizarAtivo(AtivoRepositoryEmMemoria()).executar("PAT-0404", AtualizarAtivoComando(modelo = "X"))
        }
    }

    @Test
    fun `exclui ativo sem historico`() {
        val ativos = AtivoRepositoryEmMemoria(umAtivo())

        ExcluirAtivo(ativos, ManutencaoRepositoryEmMemoria()).executar("PAT-0001")

        assertFalse(ativos.existePatrimonio("PAT-0001"))
    }

    @Test
    fun `nao exclui ativo com manutencao registrada`() {
        val ativos = AtivoRepositoryEmMemoria(umAtivo())
        val manutencoes = ManutencaoRepositoryEmMemoria(umaManutencao(numeroPatrimonio = "PAT-0001"))

        assertFailsWith<ConflitoException> { ExcluirAtivo(ativos, manutencoes).executar("PAT-0001") }
        assertTrue(ativos.existePatrimonio("PAT-0001"))
    }

    @Test
    fun `nao exclui ativo alocado`() {
        val ativos = AtivoRepositoryEmMemoria(umAtivo(status = StatusAtivo.EM_USO))

        assertFailsWith<ConflitoException> {
            ExcluirAtivo(ativos, ManutencaoRepositoryEmMemoria()).executar("PAT-0001")
        }
    }

    @Test
    fun `uso e devolucao chamados pelo servico de Alocacoes atualizam o ativo`() {
        val ativos = AtivoRepositoryEmMemoria(umAtivo())

        RegistrarUsoDoAtivo(ativos).executar("PAT-0001", RegistrarUsoComando("Marina Alves", "Financeiro"))
        assertEquals(StatusAtivo.EM_USO, ativos.buscarPorPatrimonio("PAT-0001")?.status)

        RegistrarDevolucaoDoAtivo(ativos).executar("PAT-0001")
        val devolvido = ativos.buscarPorPatrimonio("PAT-0001")
        assertEquals(StatusAtivo.EM_ESTOQUE, devolvido?.status)
        assertNull(devolvido?.colaborador)
    }

    @Test
    fun `segunda alocacao do mesmo ativo e recusada`() {
        val ativos = AtivoRepositoryEmMemoria(umAtivo())
        val registrarUso = RegistrarUsoDoAtivo(ativos)
        registrarUso.executar("PAT-0001", RegistrarUsoComando("Marina Alves", "Financeiro"))

        assertFailsWith<ConflitoException> {
            registrarUso.executar("PAT-0001", RegistrarUsoComando("Eduardo Lima", "TI"))
        }
    }
}

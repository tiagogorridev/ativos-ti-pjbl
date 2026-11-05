package br.pucpr.alocacoes.application

import br.pucpr.alocacoes.apoio.AlocacaoRepositoryEmMemoria
import br.pucpr.alocacoes.apoio.ColaboradorRepositoryEmMemoria
import br.pucpr.alocacoes.apoio.HOJE
import br.pucpr.alocacoes.apoio.RELOGIO_FIXO
import br.pucpr.alocacoes.apoio.umColaborador
import br.pucpr.alocacoes.apoio.umaAlocacao
import br.pucpr.alocacoes.application.alocacoes.AlocarAtivo
import br.pucpr.alocacoes.application.alocacoes.AlocarAtivoComando
import br.pucpr.alocacoes.application.alocacoes.DevolverAtivo
import br.pucpr.alocacoes.application.alocacoes.ExcluirAlocacao
import br.pucpr.alocacoes.application.alocacoes.ListarAlocacoes
import br.pucpr.alocacoes.application.colaboradores.AtualizarColaborador
import br.pucpr.alocacoes.application.colaboradores.AtualizarColaboradorComando
import br.pucpr.alocacoes.application.colaboradores.CadastrarColaborador
import br.pucpr.alocacoes.application.colaboradores.CadastrarColaboradorComando
import br.pucpr.alocacoes.application.colaboradores.InativarColaborador
import br.pucpr.alocacoes.application.colaboradores.ListarColaboradores
import br.pucpr.alocacoes.domain.ConflitoException
import br.pucpr.alocacoes.domain.RecursoNaoEncontradoException
import br.pucpr.alocacoes.domain.ValidacaoException
import br.pucpr.alocacoes.domain.colaborador.Situacao
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Casos de uso de Colaboradores e Alocações com repositórios em memória (sem banco). */
class CasosDeUsoTest {

    private val marina = umColaborador(id = 1)
    private val eduardo = umColaborador(id = 4, nome = "Eduardo Lima", matricula = "0029045", setor = "TI")

    @Test
    fun `aloca ativo com data de hoje quando a data nao e informada`() {
        val alocacoes = AlocacaoRepositoryEmMemoria()
        val alocar = AlocarAtivo(ColaboradorRepositoryEmMemoria(marina), alocacoes, RELOGIO_FIXO)

        val alocacao = alocar.executar(AlocarAtivoComando(numeroPatrimonio = "PAT-0003", equipamento = "LG 24MK430H", colaboradorId = 1))

        assertEquals(1, alocacao.id)
        assertEquals(HOJE, alocacao.dataRetirada)
        assertEquals("Financeiro", alocacao.setor)
        assertTrue(alocacao.vigente)
    }

    @Test
    fun `mesmo ativo nao pode ter duas alocacoes vigentes`() {
        val alocacoes = AlocacaoRepositoryEmMemoria(umaAlocacao(numeroPatrimonio = "PAT-0001", colaborador = marina))
        val alocar = AlocarAtivo(ColaboradorRepositoryEmMemoria(marina, eduardo), alocacoes, RELOGIO_FIXO)

        val erro = assertFailsWith<ConflitoException> {
            alocar.executar(AlocarAtivoComando(numeroPatrimonio = "PAT-0001", colaboradorId = 4))
        }
        assertTrue(erro.message!!.contains("Marina Alves"))
        assertEquals(1, alocacoes.listar(vigentes = true, colaboradorId = null).size)
    }

    @Test
    fun `depois da devolucao o ativo pode ser alocado de novo`() {
        val alocacoes = AlocacaoRepositoryEmMemoria(umaAlocacao(id = 1, numeroPatrimonio = "PAT-0001", colaborador = marina))
        DevolverAtivo(alocacoes, RELOGIO_FIXO).executar(1, null)

        val nova = AlocarAtivo(ColaboradorRepositoryEmMemoria(marina, eduardo), alocacoes, RELOGIO_FIXO)
            .executar(AlocarAtivoComando(numeroPatrimonio = "PAT-0001", colaboradorId = 4))

        assertEquals("Eduardo Lima", nova.colaboradorNome)
        assertEquals(1, alocacoes.listar(vigentes = false, colaboradorId = null).size)
    }

    @Test
    fun `colaborador inativo nao recebe ativo`() {
        val inativo = umColaborador(id = 6, nome = "Tiago Moraes", situacao = Situacao.INATIVO)

        assertFailsWith<ConflitoException> {
            AlocarAtivo(ColaboradorRepositoryEmMemoria(inativo), AlocacaoRepositoryEmMemoria(), RELOGIO_FIXO)
                .executar(AlocarAtivoComando(numeroPatrimonio = "PAT-0003", colaboradorId = 6))
        }
    }

    @Test
    fun `alocacao exige patrimonio e colaborador existente`() {
        val alocar = AlocarAtivo(ColaboradorRepositoryEmMemoria(marina), AlocacaoRepositoryEmMemoria(), RELOGIO_FIXO)

        assertEquals("numeroPatrimonio", assertFailsWith<ValidacaoException> { alocar.executar(AlocarAtivoComando(colaboradorId = 1)) }.campo)
        assertEquals("colaboradorId", assertFailsWith<ValidacaoException> { alocar.executar(AlocarAtivoComando(numeroPatrimonio = "PAT-0003")) }.campo)
        assertFailsWith<RecursoNaoEncontradoException> {
            alocar.executar(AlocarAtivoComando(numeroPatrimonio = "PAT-0003", colaboradorId = 99))
        }
    }

    @Test
    fun `devolucao nao pode ser anterior a retirada nem repetida`() {
        val alocacoes = AlocacaoRepositoryEmMemoria(umaAlocacao(id = 1, dataRetirada = LocalDate.of(2026, 3, 14)))
        val devolver = DevolverAtivo(alocacoes, RELOGIO_FIXO)

        assertEquals("dataDevolucao", assertFailsWith<ValidacaoException> { devolver.executar(1, LocalDate.of(2026, 1, 1)) }.campo)
        devolver.executar(1, null)
        assertFailsWith<ConflitoException> { devolver.executar(1, null) }
    }

    @Test
    fun `alocacao encerrada e historico e nao pode ser excluida`() {
        val alocacoes = AlocacaoRepositoryEmMemoria(umaAlocacao(id = 1, dataDevolucao = LocalDate.of(2026, 6, 1)))

        assertFailsWith<ConflitoException> { ExcluirAlocacao(alocacoes).executar(1) }
    }

    @Test
    fun `lista alocacoes vigentes de um colaborador`() {
        val alocacoes = AlocacaoRepositoryEmMemoria(
            umaAlocacao(id = 1, numeroPatrimonio = "PAT-0001", colaborador = marina),
            umaAlocacao(id = 2, numeroPatrimonio = "PAT-0005", colaborador = eduardo),
            umaAlocacao(id = 3, numeroPatrimonio = "PAT-0007", colaborador = marina, dataDevolucao = LocalDate.of(2026, 5, 1)),
        )

        val resultado = ListarAlocacoes(alocacoes).executar(vigentes = true, colaboradorId = 1)

        assertEquals(listOf("PAT-0001"), resultado.map { it.numeroPatrimonio })
    }

    @Test
    fun `cadastra colaborador ativo e recusa matricula repetida`() {
        val colaboradores = ColaboradorRepositoryEmMemoria(marina)
        val cadastrar = CadastrarColaborador(colaboradores)

        val novo = cadastrar.executar(CadastrarColaboradorComando("Carlos Nunes", "0038114", "Suporte"))
        assertEquals(2, novo.id)
        assertEquals(Situacao.ATIVO, novo.situacao)

        assertFailsWith<ConflitoException> { cadastrar.executar(CadastrarColaboradorComando("Outra", "0041287", "TI")) }
        assertEquals("setor", assertFailsWith<ValidacaoException> { cadastrar.executar(CadastrarColaboradorComando("Ana", "0099999", "")) }.campo)
    }

    @Test
    fun `lista colaboradores com a contagem de ativos alocados`() {
        val alocacoes = AlocacaoRepositoryEmMemoria(
            umaAlocacao(id = 1, numeroPatrimonio = "PAT-0001", colaborador = marina),
            umaAlocacao(id = 2, numeroPatrimonio = "PAT-0007", colaborador = marina),
        )

        val lista = ListarColaboradores(ColaboradorRepositoryEmMemoria(marina, eduardo), alocacoes).executar()

        assertEquals(2, lista.first { it.colaborador.id == 1L }.ativosAlocados)
        assertEquals(0, lista.first { it.colaborador.id == 4L }.ativosAlocados)
    }

    @Test
    fun `colaborador com ativo alocado nao pode ser inativado`() {
        val colaboradores = ColaboradorRepositoryEmMemoria(marina)
        val alocacoes = AlocacaoRepositoryEmMemoria(umaAlocacao(colaborador = marina))

        assertFailsWith<ConflitoException> { InativarColaborador(colaboradores, alocacoes).executar(1) }
        assertFailsWith<ConflitoException> {
            AtualizarColaborador(colaboradores, alocacoes).executar(1, AtualizarColaboradorComando(situacao = "Inativo"))
        }
        assertEquals(Situacao.ATIVO, colaboradores.buscarPorId(1)?.situacao)
    }

    @Test
    fun `colaborador sem ativos e inativado sem ser apagado`() {
        val colaboradores = ColaboradorRepositoryEmMemoria(eduardo)

        InativarColaborador(colaboradores, AlocacaoRepositoryEmMemoria()).executar(4)

        assertEquals(Situacao.INATIVO, colaboradores.buscarPorId(4)?.situacao)
        assertFalse(colaboradores.listar().isEmpty())
    }
}

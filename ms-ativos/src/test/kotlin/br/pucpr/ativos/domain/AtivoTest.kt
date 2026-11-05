package br.pucpr.ativos.domain

import br.pucpr.ativos.apoio.umAtivo
import br.pucpr.ativos.domain.ativo.StatusAtivo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Regras do ciclo de vida do ativo, testadas direto na entidade (sem banco e sem Spring). */
class AtivoTest {

    @Test
    fun `ativo em estoque pode ser entregue a um colaborador`() {
        val emUso = umAtivo().registrarUso("Marina Alves", "Financeiro")

        assertEquals(StatusAtivo.EM_USO, emUso.status)
        assertEquals("Marina Alves", emUso.colaborador)
        assertEquals("Financeiro", emUso.setor)
    }

    @Test
    fun `ativo em manutencao nao pode ser alocado`() {
        assertFailsWith<ConflitoException> {
            umAtivo(status = StatusAtivo.EM_MANUTENCAO).registrarUso("Marina Alves", "Financeiro")
        }
    }

    @Test
    fun `devolucao volta o ativo ao estoque e limpa o responsavel`() {
        val devolvido = umAtivo(status = StatusAtivo.EM_USO).registrarDevolucao()

        assertEquals(StatusAtivo.EM_ESTOQUE, devolvido.status)
        assertNull(devolvido.colaborador)
    }

    @Test
    fun `status em uso nao pode ser definido manualmente`() {
        assertFailsWith<ConflitoException> { umAtivo().alterarStatus(StatusAtivo.EM_USO) }
    }

    @Test
    fun `ativo alocado nao muda de status sem devolucao`() {
        assertFailsWith<ConflitoException> {
            umAtivo(status = StatusAtivo.EM_USO).alterarStatus(StatusAtivo.EM_MANUTENCAO)
        }
    }

    @Test
    fun `ativo baixado nao muda mais de status nem recebe manutencao`() {
        val baixado = umAtivo(status = StatusAtivo.BAIXADO)

        assertFailsWith<ConflitoException> { baixado.alterarStatus(StatusAtivo.EM_ESTOQUE) }
        assertFailsWith<ConflitoException> { baixado.garantirQueAceitaManutencao() }
    }

    @Test
    fun `ativo em estoque pode ir para manutencao e ser baixado`() {
        val baixado = umAtivo().alterarStatus(StatusAtivo.EM_MANUTENCAO).alterarStatus(StatusAtivo.BAIXADO)

        assertEquals(StatusAtivo.BAIXADO, baixado.status)
    }

    @Test
    fun `status aceita rotulo ou nome da constante e recusa texto desconhecido`() {
        assertEquals(StatusAtivo.EM_MANUTENCAO, StatusAtivo.converter("Em manutenção"))
        assertEquals(StatusAtivo.EM_USO, StatusAtivo.converter("EM_USO"))
        assertNull(StatusAtivo.converter(" "))
        val erro = assertFailsWith<ValidacaoException> { StatusAtivo.converter("Quebrado") }
        assertEquals("status", erro.campo)
    }
}

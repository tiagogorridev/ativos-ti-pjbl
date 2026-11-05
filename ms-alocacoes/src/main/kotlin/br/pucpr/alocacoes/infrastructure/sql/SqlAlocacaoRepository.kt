package br.pucpr.alocacoes.infrastructure.sql

import br.pucpr.alocacoes.domain.ConflitoException
import br.pucpr.alocacoes.domain.alocacao.Alocacao
import br.pucpr.alocacoes.domain.alocacao.AlocacaoRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.sql.Date
import java.sql.Types

/**
 * Adaptador da porta [AlocacaoRepository] para a tabela "alocacao" do Azure SQL.
 * Todas as consultas são parametrizadas; os trechos de SQL montados aqui são fixos (nunca vêm do usuário).
 */
@Component
class SqlAlocacaoRepository(private val jdbc: JdbcClient) : AlocacaoRepository {

    override fun listar(vigentes: Boolean?, colaboradorId: Long?): List<Alocacao> {
        val condicoes = mutableListOf<String>()
        val parametros = mutableMapOf<String, Any>()
        when (vigentes) {
            true -> condicoes.add("a.data_devolucao IS NULL")
            false -> condicoes.add("a.data_devolucao IS NOT NULL")
            null -> Unit
        }
        if (colaboradorId != null) {
            condicoes.add("a.colaborador_id = :colaboradorId")
            parametros["colaboradorId"] = colaboradorId
        }
        val where = if (condicoes.isEmpty()) "" else " WHERE " + condicoes.joinToString(" AND ")
        return jdbc.sql("$SELECT$where ORDER BY a.data_retirada DESC, a.id DESC")
            .params(parametros)
            .query(MAPEADOR)
            .list()
    }

    override fun buscarPorId(id: Long): Alocacao? =
        jdbc.sql("$SELECT WHERE a.id = :id")
            .param("id", id)
            .query(MAPEADOR)
            .optional()
            .orElse(null)

    override fun buscarVigentePorPatrimonio(numeroPatrimonio: String): Alocacao? =
        jdbc.sql("$SELECT WHERE a.numero_patrimonio = :patrimonio AND a.data_devolucao IS NULL")
            .param("patrimonio", numeroPatrimonio)
            .query(MAPEADOR)
            .optional()
            .orElse(null)

    override fun contarVigentesDoColaborador(colaboradorId: Long): Int =
        jdbc.sql("SELECT COUNT(*) FROM alocacao WHERE colaborador_id = :id AND data_devolucao IS NULL")
            .param("id", colaboradorId)
            .query(RowMapper { rs, _ -> rs.getInt(1) })
            .single()

    override fun contarVigentesPorColaborador(): Map<Long, Int> =
        jdbc.sql(
            "SELECT colaborador_id, COUNT(*) AS total FROM alocacao WHERE data_devolucao IS NULL GROUP BY colaborador_id",
        )
            .query(RowMapper { rs, _ -> rs.getLong("colaborador_id") to rs.getInt("total") })
            .list()
            .toMap()

    override fun inserir(alocacao: Alocacao): Long =
        try {
            jdbc.sql(
                """
                INSERT INTO alocacao (numero_patrimonio, equipamento, colaborador_id, data_retirada, data_devolucao)
                OUTPUT INSERTED.id
                VALUES (:patrimonio, :equipamento, :colaboradorId, :retirada, :devolucao)
                """.trimIndent(),
            )
                .param("patrimonio", alocacao.numeroPatrimonio)
                .param("equipamento", alocacao.equipamento)
                .param("colaboradorId", alocacao.colaboradorId)
                .param("retirada", Date.valueOf(alocacao.dataRetirada), Types.DATE)
                .param("devolucao", alocacao.dataDevolucao?.let { Date.valueOf(it) }, Types.DATE)
                .query(RowMapper { rs, _ -> rs.getLong(1) })
                .single()
        } catch (e: DataIntegrityViolationException) {
            // Índice único uk_alocacao_vigente: duas alocações vigentes para o mesmo ativo.
            throw ConflitoException(
                "O ativo ${alocacao.numeroPatrimonio} já possui uma alocação vigente. Registre a devolução antes de uma nova alocação.",
            )
        }

    override fun atualizar(alocacao: Alocacao) {
        jdbc.sql(
            """
            UPDATE alocacao
            SET equipamento = :equipamento, data_retirada = :retirada, data_devolucao = :devolucao
            WHERE id = :id
            """.trimIndent(),
        )
            .param("equipamento", alocacao.equipamento)
            .param("retirada", Date.valueOf(alocacao.dataRetirada), Types.DATE)
            .param("devolucao", alocacao.dataDevolucao?.let { Date.valueOf(it) }, Types.DATE)
            .param("id", alocacao.id)
            .update()
    }

    override fun excluir(id: Long) {
        jdbc.sql("DELETE FROM alocacao WHERE id = :id").param("id", id).update()
    }

    private companion object {
        const val SELECT = """
            SELECT a.id, a.numero_patrimonio, a.equipamento, a.colaborador_id,
                   c.nome AS colaborador_nome, c.setor AS colaborador_setor,
                   a.data_retirada, a.data_devolucao
            FROM alocacao a
            JOIN colaborador c ON c.id = a.colaborador_id
        """

        val MAPEADOR = RowMapper { rs, _ ->
            Alocacao(
                id = rs.getLong("id"),
                numeroPatrimonio = rs.getString("numero_patrimonio"),
                equipamento = rs.getString("equipamento"),
                colaboradorId = rs.getLong("colaborador_id"),
                colaboradorNome = rs.getString("colaborador_nome"),
                setor = rs.getString("colaborador_setor"),
                dataRetirada = rs.getDate("data_retirada").toLocalDate(),
                dataDevolucao = rs.getDate("data_devolucao")?.toLocalDate(),
            )
        }
    }
}

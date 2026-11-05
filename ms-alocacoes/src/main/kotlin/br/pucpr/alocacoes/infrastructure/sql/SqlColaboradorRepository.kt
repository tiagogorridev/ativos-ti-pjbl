package br.pucpr.alocacoes.infrastructure.sql

import br.pucpr.alocacoes.domain.ConflitoException
import br.pucpr.alocacoes.domain.colaborador.Colaborador
import br.pucpr.alocacoes.domain.colaborador.ColaboradorRepository
import br.pucpr.alocacoes.domain.colaborador.Situacao
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component

/** Adaptador da porta [ColaboradorRepository] para a tabela "colaborador" do Azure SQL. */
@Component
class SqlColaboradorRepository(private val jdbc: JdbcClient) : ColaboradorRepository {

    override fun listar(): List<Colaborador> =
        jdbc.sql("$SELECT ORDER BY nome")
            .query(MAPEADOR)
            .list()

    override fun buscarPorId(id: Long): Colaborador? =
        jdbc.sql("$SELECT WHERE id = :id")
            .param("id", id)
            .query(MAPEADOR)
            .optional()
            .orElse(null)

    override fun idsComMatricula(matricula: String): List<Long> =
        jdbc.sql("SELECT id FROM colaborador WHERE matricula = :matricula")
            .param("matricula", matricula)
            .query(RowMapper { rs, _ -> rs.getLong("id") })
            .list()

    override fun inserir(colaborador: Colaborador): Long =
        traduzirDuplicidade(colaborador) {
            jdbc.sql(
                """
                INSERT INTO colaborador (nome, matricula, setor, situacao)
                OUTPUT INSERTED.id
                VALUES (:nome, :matricula, :setor, :situacao)
                """.trimIndent(),
            )
                .param("nome", colaborador.nome)
                .param("matricula", colaborador.matricula)
                .param("setor", colaborador.setor)
                .param("situacao", colaborador.situacao.name)
                .query(RowMapper { rs, _ -> rs.getLong(1) })
                .single()
        }

    override fun atualizar(colaborador: Colaborador) {
        traduzirDuplicidade(colaborador) {
            jdbc.sql(
                """
                UPDATE colaborador
                SET nome = :nome, matricula = :matricula, setor = :setor, situacao = :situacao
                WHERE id = :id
                """.trimIndent(),
            )
                .param("nome", colaborador.nome)
                .param("matricula", colaborador.matricula)
                .param("setor", colaborador.setor)
                .param("situacao", colaborador.situacao.name)
                .param("id", colaborador.id)
                .update()
        }
    }

    private fun <T> traduzirDuplicidade(colaborador: Colaborador, operacao: () -> T): T =
        try {
            operacao()
        } catch (e: DataIntegrityViolationException) {
            throw ConflitoException("Já existe um colaborador com a matrícula ${colaborador.matricula}.")
        }

    private companion object {
        const val SELECT = "SELECT id, nome, matricula, setor, situacao FROM colaborador"

        val MAPEADOR = RowMapper { rs, _ ->
            Colaborador(
                id = rs.getLong("id"),
                nome = rs.getString("nome"),
                matricula = rs.getString("matricula"),
                setor = rs.getString("setor"),
                situacao = Situacao.valueOf(rs.getString("situacao")),
            )
        }
    }
}

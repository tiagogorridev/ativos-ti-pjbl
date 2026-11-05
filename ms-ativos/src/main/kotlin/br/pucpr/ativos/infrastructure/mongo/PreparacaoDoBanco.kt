package br.pucpr.ativos.infrastructure.mongo

import com.mongodb.MongoException
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * Ao subir: cria os índices (patrimônio único, filtros de status e de ativo) e, se SEED_ENABLED=true
 * e as coleções estiverem vazias, carrega os dados de exemplo. Nunca apaga nem sobrescreve dados.
 */
@Component
class PreparacaoDoBanco(
    private val database: MongoDatabase,
    @Value("\${app.seed.enabled:false}") private val carregarDadosDeExemplo: Boolean,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(PreparacaoDoBanco::class.java)

    override fun run(args: ApplicationArguments) {
        try {
            criarIndices()
            if (carregarDadosDeExemplo) {
                carregar("ativos", "/seed/ativos.json")
                carregar("manutencoes", "/seed/manutencoes.json")
            }
        } catch (e: MongoException) {
            // O serviço continua no ar; as operações vão reportar o erro de conexão.
            log.warn("Não foi possível preparar o MongoDB: {}", e.message)
        }
    }

    private fun criarIndices() {
        val ativos = database.getCollection("ativos")
        ativos.createIndex(
            Indexes.ascending("numeroPatrimonio"),
            IndexOptions().unique(true).name("uk_ativos_patrimonio"),
        )
        ativos.createIndex(Indexes.ascending("status"), IndexOptions().name("idx_ativos_status"))
        database.getCollection("manutencoes").createIndex(
            Indexes.ascending("numeroPatrimonio"),
            IndexOptions().name("idx_manutencoes_patrimonio"),
        )
    }

    private fun carregar(nomeDaColecao: String, recurso: String) {
        val colecao = database.getCollection(nomeDaColecao)
        if (colecao.countDocuments() > 0) return

        val json = PreparacaoDoBanco::class.java.getResourceAsStream(recurso)
            ?.use { String(it.readAllBytes(), Charsets.UTF_8) }
            ?: return
        val itens: List<Document> = Document.parse("{\"itens\": $json}").getList("itens", Document::class.java)
        if (itens.isNotEmpty()) {
            colecao.insertMany(itens)
            log.info("{} documentos de exemplo carregados em '{}'.", itens.size, nomeDaColecao)
        }
    }
}

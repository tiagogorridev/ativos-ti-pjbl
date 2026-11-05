package br.pucpr.ativos.infrastructure.mongo

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Conexão com o MongoDB (Atlas na nuvem, ou um container local em desenvolvimento).
 * A string de conexão vem da variável de ambiente MONGO_URI e nunca fica no código.
 */
@Configuration
class MongoConfig {

    @Bean(destroyMethod = "close")
    fun mongoClient(@Value("\${app.mongo.uri}") uri: String): MongoClient = MongoClients.create(uri)

    @Bean
    fun mongoDatabase(
        client: MongoClient,
        @Value("\${app.mongo.database}") nomeDoBanco: String,
    ): MongoDatabase = client.getDatabase(nomeDoBanco)
}

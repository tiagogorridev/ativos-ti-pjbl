package br.pucpr.alocacoes.infrastructure.config

import br.pucpr.alocacoes.application.alocacoes.AlocarAtivo
import br.pucpr.alocacoes.application.alocacoes.AtualizarAlocacao
import br.pucpr.alocacoes.application.alocacoes.DevolverAtivo
import br.pucpr.alocacoes.application.alocacoes.ExcluirAlocacao
import br.pucpr.alocacoes.application.alocacoes.ListarAlocacoes
import br.pucpr.alocacoes.application.alocacoes.ObterAlocacao
import br.pucpr.alocacoes.application.colaboradores.AtualizarColaborador
import br.pucpr.alocacoes.application.colaboradores.CadastrarColaborador
import br.pucpr.alocacoes.application.colaboradores.InativarColaborador
import br.pucpr.alocacoes.application.colaboradores.ListarColaboradores
import br.pucpr.alocacoes.application.colaboradores.ObterColaborador
import br.pucpr.alocacoes.domain.alocacao.AlocacaoRepository
import br.pucpr.alocacoes.domain.colaborador.ColaboradorRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.simple.JdbcClient
import java.time.Clock
import java.time.ZoneId
import javax.sql.DataSource

/**
 * Composição: acesso ao banco e casos de uso. As classes da Application não usam Spring;
 * é aqui, na Infrastructure, que recebem os adaptadores concretos das portas.
 */
@Configuration
class InfraestruturaConfig {

    @Bean
    fun jdbcClient(dataSource: DataSource): JdbcClient = JdbcClient.create(dataSource)

    @Bean
    fun relogio(): Clock = Clock.system(ZoneId.of("America/Sao_Paulo"))

    @Bean
    fun cadastrarColaborador(colaboradores: ColaboradorRepository) = CadastrarColaborador(colaboradores)

    @Bean
    fun listarColaboradores(colaboradores: ColaboradorRepository, alocacoes: AlocacaoRepository) =
        ListarColaboradores(colaboradores, alocacoes)

    @Bean
    fun obterColaborador(colaboradores: ColaboradorRepository, alocacoes: AlocacaoRepository) =
        ObterColaborador(colaboradores, alocacoes)

    @Bean
    fun atualizarColaborador(colaboradores: ColaboradorRepository, alocacoes: AlocacaoRepository) =
        AtualizarColaborador(colaboradores, alocacoes)

    @Bean
    fun inativarColaborador(colaboradores: ColaboradorRepository, alocacoes: AlocacaoRepository) =
        InativarColaborador(colaboradores, alocacoes)

    @Bean
    fun alocarAtivo(colaboradores: ColaboradorRepository, alocacoes: AlocacaoRepository, relogio: Clock) =
        AlocarAtivo(colaboradores, alocacoes, relogio)

    @Bean
    fun listarAlocacoes(alocacoes: AlocacaoRepository) = ListarAlocacoes(alocacoes)

    @Bean
    fun obterAlocacao(alocacoes: AlocacaoRepository) = ObterAlocacao(alocacoes)

    @Bean
    fun atualizarAlocacao(alocacoes: AlocacaoRepository) = AtualizarAlocacao(alocacoes)

    @Bean
    fun devolverAtivo(alocacoes: AlocacaoRepository, relogio: Clock) = DevolverAtivo(alocacoes, relogio)

    @Bean
    fun excluirAlocacao(alocacoes: AlocacaoRepository) = ExcluirAlocacao(alocacoes)
}

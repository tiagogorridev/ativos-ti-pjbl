package br.pucpr.ativos.infrastructure.config

import br.pucpr.ativos.application.ativos.AtualizarAtivo
import br.pucpr.ativos.application.ativos.CriarAtivo
import br.pucpr.ativos.application.ativos.ExcluirAtivo
import br.pucpr.ativos.application.ativos.ListarAtivos
import br.pucpr.ativos.application.ativos.ObterAtivo
import br.pucpr.ativos.application.ativos.RegistrarDevolucaoDoAtivo
import br.pucpr.ativos.application.ativos.RegistrarUsoDoAtivo
import br.pucpr.ativos.application.indicadores.ConsultarIndicadores
import br.pucpr.ativos.application.manutencoes.AtualizarManutencao
import br.pucpr.ativos.application.manutencoes.ExcluirManutencao
import br.pucpr.ativos.application.manutencoes.ListarManutencoes
import br.pucpr.ativos.application.manutencoes.ObterManutencao
import br.pucpr.ativos.application.manutencoes.RegistrarManutencao
import br.pucpr.ativos.domain.ativo.AtivoRepository
import br.pucpr.ativos.domain.manutencao.ManutencaoRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Composição dos casos de uso. As classes da camada Application não usam Spring;
 * é aqui, na Infrastructure, que elas recebem os adaptadores concretos das portas.
 */
@Configuration
class CasosDeUsoConfig {

    @Bean
    fun listarAtivos(ativos: AtivoRepository) = ListarAtivos(ativos)

    @Bean
    fun obterAtivo(ativos: AtivoRepository) = ObterAtivo(ativos)

    @Bean
    fun criarAtivo(ativos: AtivoRepository) = CriarAtivo(ativos)

    @Bean
    fun atualizarAtivo(ativos: AtivoRepository) = AtualizarAtivo(ativos)

    @Bean
    fun excluirAtivo(ativos: AtivoRepository, manutencoes: ManutencaoRepository) = ExcluirAtivo(ativos, manutencoes)

    @Bean
    fun registrarUsoDoAtivo(ativos: AtivoRepository) = RegistrarUsoDoAtivo(ativos)

    @Bean
    fun registrarDevolucaoDoAtivo(ativos: AtivoRepository) = RegistrarDevolucaoDoAtivo(ativos)

    @Bean
    fun registrarManutencao(ativos: AtivoRepository, manutencoes: ManutencaoRepository) =
        RegistrarManutencao(ativos, manutencoes)

    @Bean
    fun listarManutencoes(manutencoes: ManutencaoRepository) = ListarManutencoes(manutencoes)

    @Bean
    fun obterManutencao(manutencoes: ManutencaoRepository) = ObterManutencao(manutencoes)

    @Bean
    fun atualizarManutencao(manutencoes: ManutencaoRepository) = AtualizarManutencao(manutencoes)

    @Bean
    fun excluirManutencao(manutencoes: ManutencaoRepository) = ExcluirManutencao(manutencoes)

    @Bean
    fun consultarIndicadores(ativos: AtivoRepository, manutencoes: ManutencaoRepository) =
        ConsultarIndicadores(ativos, manutencoes)
}

package br.pucpr.alocacoes.application.colaboradores

import br.pucpr.alocacoes.domain.ConflitoException
import br.pucpr.alocacoes.domain.colaborador.Colaborador
import br.pucpr.alocacoes.domain.colaborador.ColaboradorRepository

data class CadastrarColaboradorComando(
    val nome: String? = null,
    val matricula: String? = null,
    val setor: String? = null,
)

/** Caso de uso: cadastrar um colaborador (RF09). A matrícula é única. */
class CadastrarColaborador(private val colaboradores: ColaboradorRepository) {

    fun executar(comando: CadastrarColaboradorComando): Colaborador {
        val novo = Colaborador.novo(comando.nome, comando.matricula, comando.setor)
        if (colaboradores.idsComMatricula(novo.matricula).isNotEmpty()) {
            throw ConflitoException("Já existe um colaborador com a matrícula ${novo.matricula}.")
        }
        val id = colaboradores.inserir(novo)
        return novo.copy(id = id)
    }
}

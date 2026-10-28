package com.example.data.model

import com.example.R

data class Exercise(
    val id: Int,
    val name: String,
    val muscleGroup: String,
    val instruction: String,
    val durationSeconds: Int = 45,
    val restSeconds: Int = 15,
    val drawableResId: Int = 0,
    val phaseNumber: Int = 1,
    val phaseName: String = "Fase 1 - Mobilidade",
    val beginnerTip: String = ""
)

object CalisthenicsExercises {
    /**
     * Periodização Biomecânica Otimizada para Iniciantes (21 Exercícios em 4 Fases):
     * - Fase 1 - Mobilidade (1 a 5): Preparação articular de punhos, ombros, cadeia posterior e escápulas.
     * - Fase 2 - Força (6 a 10): Alternância periférica MMII / MMSS (sem estabilização pura).
     * - Fase 3 - Core (11 a 16): Inicia com Elevação Pélvica (lombar), seguida por estabilização anterior/lateral (Pranchas e Canoa), sem metabólicos.
     * - Fase 4 - Finisher (17 a 21): Inicia com Caminhada de Urso, seguida por saltos, corrida, escalador e burpee.
     */
    val routine20: List<Exercise> = listOf(
        // =========================================================================
        // FASE 1 - MOBILIDADE (id_ordem 1 a 5)
        // =========================================================================
        Exercise(
            id = 1,
            name = "Mobilidade de Punhos (Wrist Warm-up)",
            muscleGroup = "Punhos, Antebraços & Tendões",
            instruction = "Ajoelhado em 4 apoios com palmas no solo, faça transferências suaves de peso para frente e para os lados, lubrificando as articulações dos punhos.",
            durationSeconds = 40,
            restSeconds = 15,
            drawableResId = R.drawable.ex_11,
            phaseNumber = 1,
            phaseName = "Fase 1 - Mobilidade",
            beginnerTip = "Lubrifica a articulação radiocarpal e reduz a tensão nos tendões flexores antes da descarga de peso em apoios de mãos."
        ),
        Exercise(
            id = 2,
            name = "Abertura e Alongamento de Ombros",
            muscleGroup = "Deltóides, Peitoral & Coluna Torácica",
            instruction = "Com joelhos apoiados e braços estendidos à frente no chão, deixe o peitoral descer suavemente em direção ao solo enquanto respira fundo.",
            durationSeconds = 40,
            restSeconds = 15,
            drawableResId = R.drawable.ex_12,
            phaseNumber = 1,
            phaseName = "Fase 1 - Mobilidade",
            beginnerTip = "Aumenta a amplitude de flexão glenoumeral e extensão torácica sem sobrecarga compressiva."
        ),
        Exercise(
            id = 3,
            name = "Alongamento Posterior (Downward Dog)",
            muscleGroup = "Isquiotibiais, Panturrilhas & Coluna",
            instruction = "Forme um V invertido elevando o quadril para o alto, empurrando o chão com as mãos e aproximando alternadamente os calcanhares do solo.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.ex_14,
            phaseNumber = 1,
            phaseName = "Fase 1 - Mobilidade",
            beginnerTip = "Alongamento dinâmico da cadeia superficial posterior, liberando isquiotibiais e gastrocnêmios para o agachamento."
        ),
        Exercise(
            id = 4,
            name = "Prancha Alta Escapular (Scapular Plank)",
            muscleGroup = "Serrátil Anterior & Estabilidade Escapular",
            instruction = "Em posição de prancha alta, empurre o chão ativamente afastando as escápulas e ativando a musculatura que protege os ombros.",
            durationSeconds = 40,
            restSeconds = 20,
            drawableResId = R.drawable.ex_13,
            phaseNumber = 1,
            phaseName = "Fase 1 - Mobilidade",
            beginnerTip = "Recruta o serrátil anterior em protração escapular para estabilizar a cintura escapular em cadeia cinética fechada."
        ),
        Exercise(
            id = 5,
            name = "Ativação Escapular em Prancha",
            muscleGroup = "Trapézio Médio/Inferior & Rombóides",
            instruction = "Com cotovelos totalmente travados e estendidos, junte e afaste as escápulas em movimentos curtos e controlados.",
            durationSeconds = 40,
            restSeconds = 20,
            drawableResId = R.drawable.ex_17,
            phaseNumber = 1,
            phaseName = "Fase 1 - Mobilidade",
            beginnerTip = "Refina a dissociação escapulotorácica (retração e protração) preparando os estabilizadores para a fase de força."
        ),

        // =========================================================================
        // FASE 2 - FORÇA (id_ordem 6 a 10 • Alternância MMII / MMSS)
        // =========================================================================
        Exercise(
            id = 6,
            name = "Agachamento Livre (Air Squats)",
            muscleGroup = "Quadríceps, Glúteos & Core (MMII)",
            instruction = "Pés na largura dos ombros, peito aberto. Empurre o quadril para trás e suba empurrando o solo com os calcanhares.",
            durationSeconds = 45,
            restSeconds = 20,
            drawableResId = R.drawable.ex_1,
            phaseNumber = 2,
            phaseName = "Fase 2 - Força",
            beginnerTip = "Inicia a fase de força com membros inferiores enquanto os membros superiores descansam da ativação escapular."
        ),
        Exercise(
            id = 7,
            name = "Flexão de Braços (Push-ups)",
            muscleGroup = "Peitoral, Tríceps & Deltoides Anteriores (MMSS)",
            instruction = "Corpo firme em linha reta. Desça o peito flexionando os cotovelos a cerca de 45° do tronco e empurre de volta.",
            durationSeconds = 40,
            restSeconds = 20,
            drawableResId = R.drawable.ex_2,
            phaseNumber = 2,
            phaseName = "Fase 2 - Força",
            beginnerTip = "Padrão de empurrar horizontal executado com o peitoral e tríceps descansados enquanto as pernas recuperam."
        ),
        Exercise(
            id = 8,
            name = "Afundo Alternado (Lunges)",
            muscleGroup = "Quadríceps, Isquiotibiais & Glúteos (MMII)",
            instruction = "Dê um passo à frente e flexione ambos os joelhos a 90°. Empurre o chão com a perna da frente para voltar e alterne.",
            durationSeconds = 45,
            restSeconds = 20,
            drawableResId = R.drawable.ex_3,
            phaseNumber = 2,
            phaseName = "Fase 2 - Força",
            beginnerTip = "Retoma o estímulo de membros inferiores de forma unilateral, permitindo recuperação periférica da cintura escapular."
        ),
        Exercise(
            id = 9,
            name = "Superman com Puxada Dorsal",
            muscleGroup = "Dorsais, Rombóides & Paravertebrais (MMSS - Puxar)",
            instruction = "Deitado de bruços, eleve levemente o peito do solo e puxe os cotovelos para trás formando um 'W', contraindo as costas.",
            durationSeconds = 45,
            restSeconds = 20,
            drawableResId = R.drawable.ex_21_superman,
            phaseNumber = 2,
            phaseName = "Fase 2 - Força",
            beginnerTip = "Trabalha a musculatura antagonista de tração dorsal e extensão torácica sem fadigar os empurradores antes do Pike."
        ),
        Exercise(
            id = 10,
            name = "Flexão Pike (Pike Push-ups)",
            muscleGroup = "Deltoides, Tríceps & Trapézio Superior (MMSS - Empurrar)",
            instruction = "Em V invertido com quadril alto, flexione os cotovelos levando o topo da cabeça à frente da linha das mãos e empurre.",
            durationSeconds = 35,
            restSeconds = 25,
            drawableResId = R.drawable.ex_19,
            phaseNumber = 2,
            phaseName = "Fase 2 - Força",
            beginnerTip = "Fecha o bloco de força com empurrar vertical após recuperação completa de deltoides e tríceps."
        ),

        // =========================================================================
        // FASE 3 - CORE (id_ordem 11 a 16 • Lombar + Estabilização Pura)
        // =========================================================================
        Exercise(
            id = 11,
            name = "Elevação Pélvica (Glute Bridge)",
            muscleGroup = "Glúteos, Lombar & Cadeia Posterior",
            instruction = "Deitado de costas com joelhos dobrados e pés no solo, eleve o quadril alinhando joelhos, quadril e ombros.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.ex_7,
            phaseNumber = 3,
            phaseName = "Fase 3 - Core",
            beginnerTip = "Posicionado na abertura do Core para aquecer a região lombopélvica e ativar glúteos antes das pranchas."
        ),
        Exercise(
            id = 12,
            name = "Prancha Abdominal (Plank)",
            muscleGroup = "Core Profundo, Transverso & Reto Abdominal",
            instruction = "Apoie antebraços e pontas dos pés no chão. Contraia abdômen e glúteos mantendo a coluna neutra.",
            durationSeconds = 40,
            restSeconds = 20,
            drawableResId = R.drawable.ex_8,
            phaseNumber = 3,
            phaseName = "Fase 3 - Core",
            beginnerTip = "Estabilização anti-extensão fundamental em apoio de antebraços, poupando os punhos."
        ),
        Exercise(
            id = 13,
            name = "Prancha Golfinho (Dolphin Pose)",
            muscleGroup = "Core Anterior, Serrátil & Estabilizadores de Ombro",
            instruction = "Com antebraços firmes no chão, eleve o quadril em V invertido sustentando a estabilidade escapular e abdominal.",
            durationSeconds = 40,
            restSeconds = 20,
            drawableResId = R.drawable.ex_15,
            phaseNumber = 3,
            phaseName = "Fase 3 - Core",
            beginnerTip = "Movido da fase de força para atuar como estabilização isométrica pura de core e cintura escapular."
        ),
        Exercise(
            id = 14,
            name = "Toque nos Ombros em Prancha (Shoulder Taps)",
            muscleGroup = "Core Anti-rotacional & Oblíquos",
            instruction = "Em prancha alta com base afastada, toque a mão no ombro oposto impedindo qualquer rotação do quadril.",
            durationSeconds = 40,
            restSeconds = 20,
            drawableResId = R.drawable.ex_20,
            phaseNumber = 3,
            phaseName = "Fase 3 - Core",
            beginnerTip = "Desafia a estabilidade anti-rotacional lombo-pélvica em apoio unilateral alternado."
        ),
        Exercise(
            id = 15,
            name = "Prancha Lateral (Side Plank)",
            muscleGroup = "Oblíquos, Quadrado Lombar & Glúteo Médio",
            instruction = "Apoie um antebraço no solo abaixo do ombro e sustente o quadril elevado lateralmente (20s de cada lado).",
            durationSeconds = 40,
            restSeconds = 20,
            drawableResId = R.drawable.ex_9,
            phaseNumber = 3,
            phaseName = "Fase 3 - Core",
            beginnerTip = "Trabalha a estabilidade anti-flexão lateral do quadrado lombar e oblíquos enquanto alivia o reto abdominal."
        ),
        Exercise(
            id = 16,
            name = "Canoa Isométrica (Hollow Body)",
            muscleGroup = "Reto Abdominal & Transverso Profundo",
            instruction = "Deitado de costas, pressione totalmente a lombar contra o chão e sustente escápulas e pernas elevadas.",
            durationSeconds = 35,
            restSeconds = 25,
            drawableResId = R.drawable.ex_18,
            phaseNumber = 3,
            phaseName = "Fase 3 - Core",
            beginnerTip = "Encerra o bloco de Core com contração isométrica máxima da parede abdominal anterior em retroversão pélvica."
        ),

        // =========================================================================
        // FASE 4 - FINISHER (id_ordem 17 a 21 • Alta Demanda Cardiovascular)
        // =========================================================================
        Exercise(
            id = 17,
            name = "Caminhada de Urso (Bear Crawl)",
            muscleGroup = "Cardio Multiarticular, Core & Ombros",
            instruction = "Em 4 apoios com joelhos flutuando a 3 cm do solo, desloque-se coordenando mãos e pés com tronco firme.",
            durationSeconds = 40,
            restSeconds = 20,
            drawableResId = R.drawable.ex_16,
            phaseNumber = 4,
            phaseName = "Fase 4 - Finisher",
            beginnerTip = "Abre o Finisher por sua alta demanda cardiovascular multiarticular sem impacto vertical nas articulações."
        ),
        Exercise(
            id = 18,
            name = "Polichinelo (Jumping Jacks)",
            muscleGroup = "Cardio, Coordenação & Panturrilhas",
            instruction = "Afaste os pés e eleve os braços acima da cabeça em ritmo contínuo, aterrissando suavemente na ponta dos pés.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.ex_4,
            phaseNumber = 4,
            phaseName = "Fase 4 - Finisher",
            beginnerTip = "Inicia o ciclo elástico-pliométrico em plano frontal elevando gradualmente a frequência cardíaca."
        ),
        Exercise(
            id = 19,
            name = "Corrida Parada (High Knees)",
            muscleGroup = "Cardio Intenso, Flexores do Quadril & Panturrilhas",
            instruction = "Eleve alternadamente os joelhos até a linha da cintura sincronizando a impulsão dos braços.",
            durationSeconds = 40,
            restSeconds = 20,
            drawableResId = R.drawable.ex_5,
            phaseNumber = 4,
            phaseName = "Fase 4 - Finisher",
            beginnerTip = "Intensifica o consumo de oxigênio (VO2) e a potência dinâmica dos flexores do quadril em postura bípede."
        ),
        Exercise(
            id = 20,
            name = "Escalador (Mountain Climbers)",
            muscleGroup = "Cardio Metabólico, Abdômen & Cintura Escapular",
            instruction = "Em prancha alta, traga alternadamente os joelhos em direção ao peitoral mantendo o quadril baixo.",
            durationSeconds = 40,
            restSeconds = 20,
            drawableResId = R.drawable.ex_6,
            phaseNumber = 4,
            phaseName = "Fase 4 - Finisher",
            beginnerTip = "Combina alta velocidade metabólica de membros inferiores com sustentação isométrica de tronco."
        ),
        Exercise(
            id = 21,
            name = "Burpee Completo / Adaptado (Full Burpee)",
            muscleGroup = "Potência Metabólica de Corpo Inteiro",
            instruction = "Agache, apoie as mãos no chão, leve os pés à prancha, retorne à base do agachamento e estenda o corpo.",
            durationSeconds = 35,
            restSeconds = 25,
            drawableResId = R.drawable.ex_10,
            phaseNumber = 4,
            phaseName = "Fase 4 - Finisher",
            beginnerTip = "Alocado como último exercício para exaustão metabólica segura sem comprometer exercícios técnicos posteriores."
        )
    )

    val routine13: List<Exercise> get() = routine20.take(13)
}

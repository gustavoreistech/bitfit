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
    val phaseName: String = "Fase 1 - Aquecimento",
    val beginnerTip: String = ""
)

data class StrengthExercise(
    val id: Int,
    val name: String,
    val muscleGroup: String,
    val anchorSetup: String,
    val targetSets: Int,
    val targetRepsRange: String,
    val defaultReps: Int,
    val restSeconds: Int = 60,
    val biomechanicalTip: String,
    val isTimedStretch: Boolean = false,
    val stretchDurationSeconds: Int = 600,
    val drawableResId: Int = R.drawable.ex_1
)

/**
 * MÓDULO 2: HIIT CARDIO EM CASA (25 MIN)
 * Baseado no cronograma de Segunda, Quarta e Sexta-feira,
 * com adaptação de impacto articular para 1,86m / 101kg.
 */
object HiitExercises {
    val routine27: List<Exercise> = listOf(
        // =========================================================================
        // FASE 1 - AQUECIMENTO ARTICULAR & CARDIO (7 Exercícios • 20s cada • Sem pausa)
        // =========================================================================
        Exercise(
            id = 1,
            name = "Tapa Frontal + Passo Atrás",
            muscleGroup = "Ombros, Quadril & Panturrilhas",
            instruction = "Dê um passo alternado para trás enquanto bate as mãos à frente na linha do peito em ritmo contínuo.",
            durationSeconds = 20,
            restSeconds = 0,
            drawableResId = R.drawable.ex_12,
            phaseNumber = 1,
            phaseName = "Fase 1 - Aquecimento",
            beginnerTip = "Aquecimento dinâmico de ombros e quadril sem impacto nos joelhos."
        ),
        Exercise(
            id = 2,
            name = "Polichinelo Sem Salto",
            muscleGroup = "Cardiorrespiratório & Deltoides",
            instruction = "Afaste uma perna de cada vez lateralmente enquanto eleva os braços acima da cabeça, mantendo um pé sempre apoiado.",
            durationSeconds = 20,
            restSeconds = 0,
            drawableResId = R.drawable.ex_4,
            phaseNumber = 1,
            phaseName = "Fase 1 - Aquecimento",
            beginnerTip = "Ativação cardiorrespiratória progressiva zero impacto articular (ideal para 101kg)."
        ),
        Exercise(
            id = 3,
            name = "Elevação Lateral + Joelhada",
            muscleGroup = "Flexores do Quadril, Core & Ombros",
            instruction = "Eleve os braços lateralmente e traga o joelho em direção ao abdômen alternando os lados.",
            durationSeconds = 20,
            restSeconds = 0,
            drawableResId = R.drawable.ex_5,
            phaseNumber = 1,
            phaseName = "Fase 1 - Aquecimento",
            beginnerTip = "Mobilidade de tronco e ativação dos flexores do quadril mantendo postura alta."
        ),
        Exercise(
            id = 4,
            name = "Agachamento Curto 2x Baixo/Cima",
            muscleGroup = "Quadríceps, Glúteos & Joelhos",
            instruction = "Faça 2 pulsos curtos de meio-agachamento embaixo e suba estendendo o quadril com controle.",
            durationSeconds = 20,
            restSeconds = 0,
            drawableResId = R.drawable.ex_1,
            phaseNumber = 1,
            phaseName = "Fase 1 - Aquecimento",
            beginnerTip = "Lubrificação sinovial de joelhos e tornozelos antes do bloco principal."
        ),
        Exercise(
            id = 5,
            name = "Salto Simples Amortecido / Ponta dos Pés",
            muscleGroup = "Panturrilhas & Tendão Calcâneo",
            instruction = "Faça impulsos suaves na ponta dos pés (ou pequenos saltos amortecendo com joelhos semiflexionados).",
            durationSeconds = 20,
            restSeconds = 0,
            drawableResId = R.drawable.ex_14,
            phaseNumber = 1,
            phaseName = "Fase 1 - Aquecimento",
            beginnerTip = "Prepara a panturrilha como bomba venosa e amortecedor elástico."
        ),
        Exercise(
            id = 6,
            name = "Trote Leve no Lugar",
            muscleGroup = "Sistema Cardiovascular & Membros Inferiores",
            instruction = "Corra estacionário de forma suave, coordenando braços e respiração nasal/bucal.",
            durationSeconds = 20,
            restSeconds = 0,
            drawableResId = R.drawable.ex_5,
            phaseNumber = 1,
            phaseName = "Fase 1 - Aquecimento",
            beginnerTip = "Elevação gradual da frequência cardíaca para entrar na zona aeróbica/HIIT."
        ),
        Exercise(
            id = 7,
            name = "Torção de Quadril Alternada 2x",
            muscleGroup = "Oblíquos, Coluna Torácica & Core",
            instruction = "Em base atlética, gire o tronco e o quadril 2 vezes para cada lado com o abdômen contraído.",
            durationSeconds = 20,
            restSeconds = 0,
            drawableResId = R.drawable.ex_9,
            phaseNumber = 1,
            phaseName = "Fase 1 - Aquecimento",
            beginnerTip = "Solta a coluna torácica e prepara a lombar para o início do Cardio HIIT."
        ),

        // =========================================================================
        // FASE 2 - CARDIO HIIT PRINCIPAL (20 Exercícios • 45s Ação / 15s Descanso)
        // =========================================================================
        Exercise(
            id = 8,
            name = "Simulação de Corda",
            muscleGroup = "Cardio, Panturrilhas & Coordenação",
            instruction = "Gire os punhos como se batesse corda fazendo saltitos baixos ou alternando os calcanhares.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.ex_4,
            phaseNumber = 2,
            phaseName = "Fase 2 - Cardio HIIT",
            beginnerTip = "Mantenha os joelhos destravados e aterrisse sempre de forma silenciosa."
        ),
        Exercise(
            id = 9,
            name = "Polichinelo (Ritmo Constante)",
            muscleGroup = "Cardio Global, Ombros & Abdutores",
            instruction = "Abra e feche pernas e braços de forma sincronizada. Se sentir impacto, alterne os passos sem saltar.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.ex_4,
            phaseNumber = 2,
            phaseName = "Fase 2 - Cardio HIIT",
            beginnerTip = "Excelente queimador calórico; priorize a cadência constante durante os 45s."
        ),
        Exercise(
            id = 10,
            name = "Mountain Climber (Escalador)",
            muscleGroup = "Core, Ombros & Cardio Metabólico",
            instruction = "Em prancha alta (no solo ou mãos apoiadas no sofá/banco), puxe os joelhos alternadamente ao peito.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.hiit_escalador,
            phaseNumber = 2,
            phaseName = "Fase 2 - Cardio HIIT",
            beginnerTip = "Mantenha o quadril alinhado aos ombros sem deixar a lombar afundar."
        ),
        Exercise(
            id = 11,
            name = "Joelho no Cotovelo Lateral Em Pé",
            muscleGroup = "Oblíquos, Quadril & Equilíbrio",
            instruction = "Mãos atrás da cabeça, eleve o joelho lateralmente encontrando o cotovelo do mesmo lado.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.ex_9,
            phaseNumber = 2,
            phaseName = "Fase 2 - Cardio HIIT",
            beginnerTip = "Solte o ar contraindo fortemente a lateral do abdômen a cada subida."
        ),
        Exercise(
            id = 12,
            name = "Agachamento Sumô Mãos Baixo/Cima",
            muscleGroup = "Glúteos, Adutores da Coxa & Core",
            instruction = "Pés afastados com pontas levemente para fora; agache tocando as mãos embaixo e suba estendendo os braços.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.hiit_sumo,
            phaseNumber = 2,
            phaseName = "Fase 2 - Cardio HIIT",
            beginnerTip = "Mantenha o peito aberto e empurre o chão com os calcanhares ao subir."
        ),
        Exercise(
            id = 13,
            name = "Tapa Entre as Pernas",
            muscleGroup = "Abdômen Inferior & Flexores do Quadril",
            instruction = "Em pé, eleve uma perna de cada vez à frente batendo as mãos por baixo da coxa com coluna ereta.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.ex_5,
            phaseNumber = 2,
            phaseName = "Fase 2 - Cardio HIIT",
            beginnerTip = "Leve a perna até as mãos sem curvar as costas para frente."
        ),
        Exercise(
            id = 14,
            name = "Sprint Intervalado no Lugar",
            muscleGroup = "VO2 Máximo, Panturrilhas & Quadríceps",
            instruction = "Acelere a corrida parada em passos rápidos e curtos movimentando vigorosamente os braços.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.hiit_sprint,
            phaseNumber = 2,
            phaseName = "Fase 2 - Cardio HIIT",
            beginnerTip = "Pico metabólico do bloco: mantenha a respiração ritmada sem prender o ar."
        ),
        Exercise(
            id = 15,
            name = "Mãos Trocadas no Solo / Base Alta",
            muscleGroup = "Core Anti-Rotacional, Peito & Ombros",
            instruction = "Em base de prancha alta ou semi-agachado, alterne o toque das mãos no solo/ombros com agilidade.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.ex_20,
            phaseNumber = 2,
            phaseName = "Fase 2 - Cardio HIIT",
            beginnerTip = "Afaste os pés para estabilizar a pelve e proteger a coluna lombar."
        ),
        Exercise(
            id = 16,
            name = "Deslocamento Lateral + Agachamento",
            muscleGroup = "Quadríceps, Glúteo Médio & Cardio",
            instruction = "Dê 2 passos rápidos para a direita e agache; dê 2 passos para a esquerda e agache.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.ex_3,
            phaseNumber = 2,
            phaseName = "Fase 2 - Cardio HIIT",
            beginnerTip = "Trabalha desaceleração lateral protegendo os ligamentos do joelho."
        ),
        Exercise(
            id = 17,
            name = "Soco Frontal + Passo ao Lado",
            muscleGroup = "Deltoides, Peitoral, Core & Ritmo",
            instruction = "Desloque o pé lateralmente enquanto dispara socos diretos firmes na altura do queixo.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.ex_13,
            phaseNumber = 2,
            phaseName = "Fase 2 - Cardio HIIT",
            beginnerTip = "Recupere o fôlego das pernas mantendo o gasto calórico alto nos braços."
        ),
        Exercise(
            id = 18,
            name = "Trote Lateral de Agilidade",
            muscleGroup = "Cardio, Panturrilhas & Estabilizadores",
            instruction = "Desloque-se de um lado para o outro com passos curtos e ágeis em base atlética.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.ex_16,
            phaseNumber = 2,
            phaseName = "Fase 2 - Cardio HIIT",
            beginnerTip = "Mantenha o centro de gravidade baixo e os joelhos macios."
        ),
        Exercise(
            id = 19,
            name = "Socos Diretos + Joelhadas",
            muscleGroup = "Corpo Inteiro, Core & Condicionamento",
            instruction = "Combine 4 socos rápidos à frente seguidos de 2 joelhadas alternadas vigorosas.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.ex_10,
            phaseNumber = 2,
            phaseName = "Fase 2 - Cardio HIIT",
            beginnerTip = "Combinação metabólica de lutas que ativa grandes grupos musculares simultaneamente."
        ),
        Exercise(
            id = 20,
            name = "Agachamento + Soco Cruzado",
            muscleGroup = "Quadríceps, Glúteos, Oblíquos & Ombros",
            instruction = "Agache com controle e, ao subir, gire o tronco desferindo um soco cruzado para o lado oposto.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.ex_1,
            phaseNumber = 2,
            phaseName = "Fase 2 - Cardio HIIT",
            beginnerTip = "Gire o calcanhar de trás junto com o soco para não torcer o joelho."
        ),
        Exercise(
            id = 21,
            name = "Passo Atrás + Chute Frontal",
            muscleGroup = "Glúteos, Isquiotibiais, Quadríceps & Core",
            instruction = "Recue uma perna em afundo curto e suba chutando à frente com a mesma perna; alterne aos 22s.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.ex_3,
            phaseNumber = 2,
            phaseName = "Fase 2 - Cardio HIIT",
            beginnerTip = "Controle a amplitude do passo atrás para manter total equilíbrio."
        ),
        Exercise(
            id = 22,
            name = "Polichinelo + Agachamento Controlado",
            muscleGroup = "Cardio Intenso, Glúteos & Coxas",
            instruction = "Faça 2 polichinelos (com ou sem salto) seguidos imediatamente de 1 agachamento firme.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.ex_4,
            phaseNumber = 2,
            phaseName = "Fase 2 - Cardio HIIT",
            beginnerTip = "Excelente estímulo misto de frequência cardíaca e resistência muscular localizada."
        ),
        Exercise(
            id = 23,
            name = "Deslocamento Lateral + Gancho",
            muscleGroup = "Core Rotacional, Dorsal, Ombros & Pernas",
            instruction = "Dê uma passada lateral e encaixando o quadril, execute um soco em gancho firme.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.ex_17,
            phaseNumber = 2,
            phaseName = "Fase 2 - Cardio HIIT",
            beginnerTip = "A força do gancho vem da rotação do quadril e estabilidade do core."
        ),
        Exercise(
            id = 24,
            name = "Calcanhar no Glúteo Dinâmico",
            muscleGroup = "Isquiotibiais (Posterior de Coxa) & Cardio",
            instruction = "Em ritmo contínuo, leve alternadamente os calcanhares em direção aos glúteos.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.ex_5,
            phaseNumber = 2,
            phaseName = "Fase 2 - Cardio HIIT",
            beginnerTip = "Alonga dinamicamente o quadríceps enquanto trabalha o posterior de coxa."
        ),
        Exercise(
            id = 25,
            name = "Joelho no Cotovelo Cruzado",
            muscleGroup = "Oblíquos Internos/Externos & Flexores",
            instruction = "Em pé, traga o joelho direito em direção ao cotovelo esquerdo e alterne em ritmo forte.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.ex_18,
            phaseNumber = 2,
            phaseName = "Fase 2 - Cardio HIIT",
            beginnerTip = "Abdominal funcional em pé: zero pressão cervical ou lombar."
        ),
        Exercise(
            id = 26,
            name = "Socos Alternados em Base de Combate",
            muscleGroup = "Resistência de Ombros, Tríceps, Core & Coxas",
            instruction = "Em meio-agachamento isométrico leve, dispare socos retos rápidos e contínuos à frente.",
            durationSeconds = 45,
            restSeconds = 15,
            drawableResId = R.drawable.ex_2,
            phaseNumber = 2,
            phaseName = "Fase 2 - Cardio HIIT",
            beginnerTip = "Penúltimo exercício: mantenha o abdômen travado e velocidade nos braços!"
        ),
        Exercise(
            id = 27,
            name = "Joelhos Para Fora / Elevação Dinâmica (Finisher)",
            muscleGroup = "Fechamento Metabólico de Quadril, Core & Cardio",
            instruction = "Eleve os joelhos de forma ampla e dinâmica tocando as mãos para fechar os 25 minutos no topo!",
            durationSeconds = 45,
            restSeconds = 0,
            drawableResId = R.drawable.ex_10,
            phaseNumber = 2,
            phaseName = "Fase 2 - Cardio HIIT",
            beginnerTip = "Sprint final do treino! Entregue sua melhor energia nos últimos 45 segundos."
        )
    )
}

/**
 * MÓDULO 3: MUSCULAÇÃO COM ELÁSTICO (TERÇA & QUINTA-FEIRA)
 * Foco em hipertrofia funcional, proteção articular, postura para plantão noturno (22h às 06h20)
 * e descompressão lombar.
 */
object ElasticStrengthExercises {
    val routine7: List<StrengthExercise> = listOf(
        StrengthExercise(
            id = 1,
            name = "Supino de Pé com Elástico",
            muscleGroup = "Peitoral Maior, Deltoide Anterior & Tríceps",
            anchorSetup = "Âncora presa na porta na altura do peito (de costas para a porta)",
            targetSets = 4,
            targetRepsRange = "12-15 reps",
            defaultReps = 15,
            restSeconds = 60,
            biomechanicalTip = "Foco em peitorais, deltoide anterior e tríceps; mantenha postura ereta, base escalonada (um pé à frente) e core firme.",
            drawableResId = R.drawable.elastic_supino
        ),
        StrengthExercise(
            id = 2,
            name = "Remada Sentada / Baixa com Elástico",
            muscleGroup = "Grande Dorsal, Rombóides, Trapézio Médio & Bíceps",
            anchorSetup = "Elástico passado nos pés (sentado) ou na base da porta",
            targetSets = 4,
            targetRepsRange = "15 reps",
            defaultReps = 15,
            restSeconds = 60,
            biomechanicalTip = "Fortalecimento dorsal e romboides; retraia as escápulas no final de cada puxada. Essencial para proteger a postura no plantão noturno.",
            drawableResId = R.drawable.elastic_remada
        ),
        StrengthExercise(
            id = 3,
            name = "Desenvolvimento de Ombros com Elástico",
            muscleGroup = "Deltoide Anterior/Médio & Trapézio Superior",
            anchorSetup = "Em pé pisando no meio do elástico, puxadores na linha dos ombros",
            targetSets = 4,
            targetRepsRange = "12-15 reps",
            defaultReps = 15,
            restSeconds = 60,
            biomechanicalTip = "Trabalha deltoides; contraia o abdômen e glúteos para não curvar a coluna lombar ao elevar os braços acima da cabeça.",
            drawableResId = R.drawable.elastic_ombros
        ),
        StrengthExercise(
            id = 4,
            name = "Agachamento Resistido com Elástico",
            muscleGroup = "Quadríceps, Glúteos, Isquiotibiais & Core",
            anchorSetup = "Pés na largura dos ombros pisando no elástico, alças apoiadas nos ombros",
            targetSets = 4,
            targetRepsRange = "15-20 reps",
            defaultReps = 18,
            restSeconds = 60,
            biomechanicalTip = "Quadríceps, glúteos e posterior; cadência controlada na descida (2s) e força nos calcanhares ao subir vencendo a tensão do elástico.",
            drawableResId = R.drawable.elastic_agachamento
        ),
        StrengthExercise(
            id = 5,
            name = "Rosca Direta com Elástico",
            muscleGroup = "Bíceps Braquial & Braquiorradial",
            anchorSetup = "Em pé pisando no elástico, palmas das mãos voltadas para cima",
            targetSets = 3,
            targetRepsRange = "12-15 reps",
            defaultReps = 15,
            restSeconds = 60,
            biomechanicalTip = "Isolamento de bíceps; mantenha os cotovelos colados ao tronco durante toda a subida e segure o retorno excêntrico do elástico.",
            drawableResId = R.drawable.elastic_biceps
        ),
        StrengthExercise(
            id = 6,
            name = "Tríceps Testa / Francês com Elástico",
            muscleGroup = "Tríceps Braquial (Cabeça Longa, Lateral e Medial)",
            anchorSetup = "Âncora na porta ou pisando na fita por trás do corpo, cotovelos apontando p/ cima/frente",
            targetSets = 3,
            targetRepsRange = "12-15 reps",
            defaultReps = 15,
            restSeconds = 60,
            biomechanicalTip = "Tríceps braquial; mantenha os cotovelos fixos e controle a fase excêntrica (volta do elástico) para máximo estímulo muscular.",
            drawableResId = R.drawable.elastic_triceps
        ),
        StrengthExercise(
            id = 7,
            name = "Alongamento Profundo & Descompressão Lombar",
            muscleGroup = "Coluna Lombar, Isquiotibiais, Piriforme & Cadeia Posterior",
            anchorSetup = "No tapete / solo livre (respiração diafragmática lenta)",
            targetSets = 1,
            targetRepsRange = "10 min",
            defaultReps = 1,
            restSeconds = 0,
            biomechanicalTip = "Foco total na descompressão lombar, cadeia posterior e relaxamento neuromuscular antes do plantão noturno (22h às 06h20).",
            isTimedStretch = true,
            stretchDurationSeconds = 600,
            drawableResId = R.drawable.elastic_alongamento
        )
    )

    val strengthOnly6: List<StrengthExercise>
        get() = routine7.filter { !it.isTimedStretch }

    val totalTargetSets: Int
        get() = strengthOnly6.sumOf { it.targetSets } // 4+4+4+4+3+3 = 22 séries
}

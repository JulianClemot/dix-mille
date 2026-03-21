package com.julian.dixmille.feature.score_sheet.di

import com.julian.dixmille.feature.score_sheet.domain.usecase.AddScoreEntryUseCase
import com.julian.dixmille.feature.score_sheet.domain.usecase.BustTurnUseCase
import com.julian.dixmille.feature.score_sheet.domain.usecase.CommitTurnUseCase
import com.julian.dixmille.feature.score_sheet.domain.usecase.SkipTurnUseCase
import com.julian.dixmille.feature.score_sheet.domain.usecase.UndoLastEntryUseCase
import com.julian.dixmille.feature.score_sheet.domain.usecase.UndoLastTurnUseCase
import com.julian.dixmille.feature.score_sheet.presentation.viewmodel.ScoreSheetViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val scoreSheetModule = module {
    singleOf(::AddScoreEntryUseCase)
    singleOf(::CommitTurnUseCase)
    singleOf(::BustTurnUseCase)
    singleOf(::SkipTurnUseCase)
    singleOf(::UndoLastEntryUseCase)
    singleOf(::UndoLastTurnUseCase)
    viewModelOf(::ScoreSheetViewModel)
}

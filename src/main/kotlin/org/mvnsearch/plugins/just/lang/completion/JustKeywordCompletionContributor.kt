package org.mvnsearch.plugins.just.lang.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.mvnsearch.plugins.just.lang.JustLanguage
import org.mvnsearch.plugins.just.lang.psi.JustTypes

class JustKeywordCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            psiElement(PsiElement::class.java).withLanguage(JustLanguage),
            object : CompletionProvider<CompletionParameters>() {
                private val keywords = listOf(
                    JustTypes.KEYWORD_IMPORT,
                    JustTypes.KEYWORD_EXPORT,
                    JustTypes.KEYWORD_SET,
                    JustTypes.KEYWORD_ALIAS,
                    JustTypes.KEYWORD_MOD,
                )
                override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, resultSet: CompletionResultSet) {
                    resultSet.addAllElements(keywords.map { LookupElementBuilder.create(it) })
                }
            }
        )
    }
}

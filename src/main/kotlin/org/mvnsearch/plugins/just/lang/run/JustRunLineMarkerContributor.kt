package org.mvnsearch.plugins.just.lang.run

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.lineMarker.RunLineMarkerProvider
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.ide.actions.runAnything.commands.RunAnythingCommandCustomizer
import com.intellij.ide.actions.runAnything.execution.RunAnythingRunProfile
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.util.execution.ParametersListUtil
import org.mvnsearch.plugins.just.Just
import org.mvnsearch.plugins.just.ide.icons.JustIcons.JUST_FILE
import org.mvnsearch.plugins.just.lang.psi.JustTypes
import org.mvnsearch.plugins.just.parseRecipeName
import javax.swing.Icon

@Suppress("DialogTitleCapitalization")
class JustRunLineMarkerContributor : RunLineMarkerProvider() {
    override fun getName(): String {
        return "run-just-recipe"
    }

    override fun getIcon(): Icon {
        return AllIcons.RunConfigurations.TestState.Run
    }

    override fun getLineMarkerInfo(psiElement: PsiElement): LineMarkerInfo<*>? {
        val elementType = psiElement.elementType
        if (elementType == JustTypes.RECIPE_STATEMENT) {
            val text = psiElement.text
            val recipeName = parseRecipeName(text)
            return LineMarkerInfo(
                psiElement,
                psiElement.textRange,
                icon,
                {
                    "Run just recipe: $recipeName"
                },
                { e, elt ->
                    runJustRecipeByRunAnything(psiElement.project, psiElement, recipeName)
                },
                GutterIconRenderer.Alignment.CENTER,
                {
                    recipeName
                }
            )
        }
        return null
    }


    private fun runJustRecipeByRunAnything(project: Project, psiElement: PsiElement, taskName: String) {
        runCommand(
            project,
            psiElement.containingFile.virtualFile.parent,
            "${Just.getJustCmdAbsolutionPath()} $taskName",
            DefaultRunExecutor.getRunExecutorInstance(),
            SimpleDataContext.getProjectContext(project)
        )
    }

    private fun runCommand(project: Project, workDirectory: VirtualFile, commandString: String, executor: Executor, dataContext: DataContext) {
        var commandDataContext = dataContext
        commandDataContext = RunAnythingCommandCustomizer.customizeContext(commandDataContext)
        val initialCommandLine = GeneralCommandLine(ParametersListUtil.parse(commandString, false, true))
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            .withWorkDirectory(workDirectory.path)
        val commandLine = RunAnythingCommandCustomizer.customizeCommandLine(commandDataContext, workDirectory, initialCommandLine)
        try {
            val generalCommandLine = if (Registry.`is`("run.anything.use.pty", false)) PtyCommandLine(commandLine) else commandLine
            val runAnythingRunProfile = RunJustProfile(generalCommandLine, commandString)
            ExecutionEnvironmentBuilder.create(project, executor, runAnythingRunProfile)
                .dataContext(commandDataContext)
                .buildAndExecute()
        } catch (e: ExecutionException) {
            Messages.showInfoMessage(project, e.message, IdeBundle.message("run.anything.console.error.title"))
        }
    }


}

class RunJustProfile(commandLine: GeneralCommandLine, originalCommand: String) : RunAnythingRunProfile(commandLine, originalCommand) {
    override fun getIcon(): Icon {
        return JUST_FILE
    }

    override fun getName(): String {
        return if (originalCommand.contains("/bin/just")) {
            originalCommand.substring(originalCommand.indexOf("/bin") + 5)
        } else {
            originalCommand
        }
    }
}
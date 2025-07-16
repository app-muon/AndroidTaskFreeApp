// ui/task/TaskSearchScreen.kt
package com.taskfree.app.ui.task

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.taskfree.app.R
import com.taskfree.app.data.repository.TaskRepository
import com.taskfree.app.ui.admin.ToolsMenuDialog
import com.taskfree.app.ui.admin.ToolsViewModel
import com.taskfree.app.ui.admin.ToolsVmFactory
import com.taskfree.app.ui.category.CategoryViewModel
import com.taskfree.app.ui.category.CategoryVmFactory
import com.taskfree.app.ui.components.AppBottomBar
import com.taskfree.app.ui.components.DueChoice
import com.taskfree.app.ui.components.DueChoiceSaver
import com.taskfree.app.ui.components.DueKind
import com.taskfree.app.ui.components.kind
import com.taskfree.app.ui.enc.EncryptWizard
import com.taskfree.app.ui.enc.ViewPhraseDialog
import com.taskfree.app.ui.onboarding.Anchor
import com.taskfree.app.ui.onboarding.LocalTipManager
import com.taskfree.app.ui.onboarding.OnboardingOverlay
import com.taskfree.app.ui.onboarding.OnboardingTip
import com.taskfree.app.ui.onboarding.TipId
import com.taskfree.app.ui.task.components.OrderProperty
import com.taskfree.app.ui.task.components.TaskDialogHost
import com.taskfree.app.ui.task.components.TaskDialogs
import com.taskfree.app.ui.task.components.TaskListContent
import com.taskfree.app.ui.task.components.TaskScreenConfig
import com.taskfree.app.util.db
import java.time.LocalDate

@Composable
fun TaskSearchScreen(
    navController: NavHostController,
    initialCategoryId: Int? = null,
    initialDueChoice: DueChoice = DueChoice.from(LocalDate.now())
) {
    val context = LocalContext.current.applicationContext as android.app.Application
    val categoriesVm: CategoryViewModel = viewModel(factory = CategoryVmFactory(context))
    val adminVm: ToolsViewModel = viewModel(factory = ToolsVmFactory(context))
    val taskVm: TaskViewModel = viewModel(
        factory = TaskVmFactory(
            appContext = context,
            repo = TaskRepository(context.db)
        )
    )

    var due by rememberSaveable(stateSaver = DueChoiceSaver) {
        mutableStateOf(initialDueChoice)
    }
    // Create configuration object
    val config = TaskScreenConfig(
        categoryId = initialCategoryId, dueChoice = due
    )

    // Collect UI states
    val adminUi by adminVm.uiState.collectAsState()
    val categoryUi by categoriesVm.uiState.collectAsState()
    val taskUi by taskVm.uiState.collectAsState()

    // Local state
    var dialogs by remember { mutableStateOf<TaskDialogs>(TaskDialogs.None) }
    var showGlobalMenu by remember { mutableStateOf(false) }
    var showEncryptWizard by rememberSaveable { mutableStateOf(false) }
    var showPhraseDialog by rememberSaveable { mutableStateOf(false) }

    val tipManager = LocalTipManager.current
    val resetTick by tipManager.resetTick.collectAsState()
    /* ------------- TYPE-1  (T1) ------------- */
    LaunchedEffect(taskUi.isInitialLoadPending, taskUi.tasks.size, resetTick) {
        if (taskUi.isInitialLoadPending) return@LaunchedEffect
        val noTasks = taskUi.tasks.isEmpty()
        val unseen = !tipManager.hasSeen(TipId.T1_ADD_TASK)

        if (noTasks || unseen) {           // OR logic
            tipManager.request(
                OnboardingTip(
                    TipId.T1_ADD_TASK,
                    context.getString(R.string.tip_add_your_first_task_title),
                    null,
                    Anchor.AboveBottomBarOnRight
                ),
                overrideSeen = noTasks     // force-show only when list is empty
            )
        }
    }


    /* ------------- TYPE-2  (T2 & T3) ---------- */
    LaunchedEffect(taskUi.tasks.size) {
        val numTasks = taskUi.tasks.size
        if (numTasks > 0) {
            if (numTasks > 1) {
                tipManager.request(
                    OnboardingTip(
                        TipId.T2_REORDER_TASK,
                        context.getString(R.string.tip_title_reorder_rows),
                        AnnotatedString(context.getString(R.string.tip_drag_the_handle_to_reorder_rows)),
                        Anchor.ScreenHeightPercent(0.2f)
                    )
                )
            }

            tipManager.request(
                OnboardingTip(
                    TipId.T3_CLICK_TASK,
                    context.getString(R.string.tip_more_actions_title),
                    AnnotatedString(context.getString(R.string.tip_click_to_edit_and_view_tasks_and_more)),
                    Anchor.ScreenHeightPercent(0.2f)
                )
            )
            tipManager.request(
                OnboardingTip(
                    TipId.T4_FILTER_TASK,
                    context.getString(R.string.tip_filter_tasks_title),
                    AnnotatedString(context.getString(R.string.tip_filter_tasks)),
                    Anchor.HandleOfFirstRow
                )
            )
            tipManager.request(
                OnboardingTip(
                    TipId.T5_VIEW_CATEGORIES,
                    context.getString(R.string.tip_navigate_to_categories_title),
                    buildAnnotatedString {
                        append(context.getString(R.string.tip_view_prefix) + " ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(context.getString(R.string.tip_view_category_label))
                        }
                        append(" " + context.getString(R.string.tip_view_suffix))
                    },
                    Anchor.AboveBottomBarInCentre
                )
            )
        }
    }
    // Effects
    LaunchedEffect(adminUi.showArchived) {
        taskVm.setShowArchived(adminUi.showArchived)
    }

    LaunchedEffect(Unit) {
        adminVm.refresh.collect { taskVm.forceReload() }
    }
    LaunchedEffect(dialogs) {
        if (dialogs is TaskDialogs.None) {
            taskVm.forceReload()
        }
    }

    // Main UI
    Column(modifier = Modifier.fillMaxSize()) {
        TaskListContent(
            noTaskMessage = if (adminUi.showArchived) {
                stringResource(R.string.no_archived_tasks_to_display_default_message)
            } else {
                stringResource(R.string.no_tasks_to_display_default_message)
            },
            orderProperty = if (config.categoryId == null) OrderProperty.TODO_PAGE else OrderProperty.TASK_PAGE,
            config = config,
            onDueChange = { due = it },
            onClickTask = { dialogs = TaskDialogs.Options(it) },
            modifier = Modifier.weight(1f),
        )

        // Bottom bar
        AppBottomBar(navController = navController,
            isTodayView = (config.dueChoice.date == LocalDate.now() && config.categoryId == null),
            addButtonLabel = stringResource(R.string.add_task_button_label),
            hasCategories = categoryUi.categories.isNotEmpty(),
            onAddTask = {
                val defaultCategory = config.categoryId?.let { id ->
                    categoryUi.categories.firstOrNull { it.id == id }
                } ?: categoryUi.categories.firstOrNull()

                defaultCategory?.let { cat ->
                    val defaultDue = if (config.dueChoice.kind == DueKind.ALL) {
                        DueChoice.fromSpecial(DueChoice.Special.NONE)
                    } else {
                        config.dueChoice
                    }
                    dialogs = TaskDialogs.Add(cat, defaultDue)
                }
            },
            onShowGlobalMenu = { showGlobalMenu = true })
    }

    // Dialogs
    TaskDialogHost(dialogs = dialogs,
        setDialogs = { dialogs = it },
        allCategories = categoryUi.categories,
        taskVm = taskVm,
        currentFilterCatId = config.categoryId,
        onNavigateToCategory = { catId ->
            navController.navigate("search?categoryId=$catId")
        })

    ToolsMenuDialog(
        startEncryptFlow = { showEncryptWizard = true },
        showPhraseFlow = { showPhraseDialog = true },
        vm = adminVm, show = showGlobalMenu,
        onDismiss = { showGlobalMenu = false },
        onResetTips = { tipManager.resetTips() })

    if (showEncryptWizard) {
        EncryptWizard(onClose = { showEncryptWizard = false })
    }

    /* ── Recovery-phrase viewer modal ──────────────────────── */
    if (showPhraseDialog) {
        ViewPhraseDialog(onClose = { showPhraseDialog = false })
    }
    OnboardingOverlay(tipManager = tipManager)
}